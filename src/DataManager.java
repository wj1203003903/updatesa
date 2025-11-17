import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManager {
    // --- 所有成员变量，包括 best_ 系列，保持不变 ---
    public LocalCache local; public CloudCache cloud; public RemoteCloud remote;
    private final double LOCAL_BUS_SPEED_BPS = 1 * 1e6;
    private final Channel cloudChannel; private final Channel remoteChannel;
    public double totalDelaySeconds = 0.0;
    public int totalAccessed = 0, localHits = 0, cloudHits = 0;
    public int localAccesses = 0, cloudAccesses = 0, remoteAccesses = 0;
    public int completedTasks = 0, totalTasks = 0;
    private final Map<Integer, DataItem> idToDataItemMap = new HashMap<>();
    private double best_totalDelaySeconds = 0.0;
    private int best_totalAccessed = 0, best_localHits = 0, best_cloudHits = 0;
    private int best_localAccesses = 0, best_cloudAccesses = 0, best_remoteAccesses = 0;
    private int best_completedTasks = 0, best_totalTasks = 0;

    public DataManager(long localCap, long edgeCap) {
        local = new LocalCache(localCap); cloud = new CloudCache(edgeCap); remote = new RemoteCloud();
        local.bindLowerLevel(cloud); cloud.bindLowerLevel(remote);
        this.cloudChannel = new Channel(8 * 1e4, 0.15, 15.0, 5.0);
        this.remoteChannel = new Channel(4 * 1e4, 0.30, 10.0, 6.0);
    }

    // --- 核心业务逻辑 (保持不变) ---
    public double access(String type, int id, long currentTime) { totalAccessed++; double delaySeconds; localAccesses++; DataItem item = local.get(type, id, currentTime); if (item != null) { localHits++; delaySeconds = (item.size * 8.0) / LOCAL_BUS_SPEED_BPS; return delaySeconds; } cloudAccesses++; item = cloud.get(type, id, currentTime); if (item != null) { cloudHits++; delaySeconds = cloudChannel.getTotalDelay(item.size); local.put(item, currentTime); return delaySeconds; } remoteAccesses++; item = remote.get(type, id, currentTime); if (item != null) { delaySeconds = remoteChannel.getTotalDelay(item.size); cloud.put(item, currentTime); local.put(item, currentTime); return delaySeconds; } item = idToDataItemMap.get(id); if (item != null) { delaySeconds = remoteChannel.getTotalDelay(item.size); cloud.put(item, currentTime); local.put(item, currentTime); return delaySeconds; } return Double.POSITIVE_INFINITY; }
    public long processAndGetDuration(DataItem item, long currentTime, int readyQueueSize) { totalTasks++; double accessDelaySeconds = access(item.type, item.id, currentTime); this.totalDelaySeconds += accessDelaySeconds; long durationMillis = (long)(accessDelaySeconds * 1000); long taskCompletionTime = currentTime + durationMillis; if (taskCompletionTime <= item.deadline) { completedTasks++; } return durationMillis; }
    public void registerDataItem(DataItem item) { idToDataItemMap.put(item.id, item); }
    public void addDataToRemote(DataItem item, long currentTime) { remote.put(item, currentTime); }
    public void normalizeL2(double[] vector) { double sumOfSquares = 0.0; for (double value : vector) { sumOfSquares += value * value; } if (sumOfSquares == 0) return; double l2Norm = Math.sqrt(sumOfSquares); for (int i = 0; i < vector.length; i++) { vector[i] = vector[i] / l2Norm; } }

    /**
     * 【核心修复】getSystemScore 方法现在使用全局命中率
     */
    public double getSystemScore() {
        if (totalAccessed == 0 || totalTasks == 0) return 0;

        // 【关键】所有命中率的分母都统一为 totalAccessed
        double localHitRate = (double) localHits / Math.max(1, totalAccessed);
        double cloudHitRate = (double) cloudHits / Math.max(1, totalAccessed);

        double completionRate = (double) completedTasks / totalTasks;
        double averageDelay = totalDelaySeconds / totalTasks;
        double w1 = 10.0, w2 = 1.0, w3 = 10.0;
        if (averageDelay == 0) return Double.POSITIVE_INFINITY;
        return 100 * (w1 * localHitRate + w2 * cloudHitRate + w3 * completionRate) / (averageDelay);
    }

    // --- 状态管理方法 (保持上一版的正确逻辑) ---
    public void resetCurrentRunStats() { totalAccessed = 0; localHits = 0; cloudHits = 0; localAccesses = 0; cloudAccesses = 0; remoteAccesses = 0; completedTasks = 0; totalTasks = 0; totalDelaySeconds = 0.0; local.clear(); cloud.clear(); }
    public void reset() { resetCurrentRunStats(); best_totalDelaySeconds = 0.0; best_totalAccessed = 0; best_localHits = 0; best_cloudHits = 0; best_localAccesses = 0; best_cloudAccesses = 0; best_remoteAccesses = 0; best_completedTasks = 0; best_totalTasks = 0; }
    public void saveBestStats() { this.best_totalDelaySeconds = this.totalDelaySeconds; this.best_totalAccessed = this.totalAccessed; this.best_localHits = this.localHits; this.best_cloudHits = this.cloudHits; this.best_localAccesses = this.localAccesses; this.best_cloudAccesses = this.cloudAccesses; this.best_remoteAccesses = this.remoteAccesses; this.best_completedTasks = this.completedTasks; this.best_totalTasks = this.totalTasks; }

    /**
     * printStats 方法现在与 getSystemScore 的计算逻辑完全一致
     */
    public void printStats(PrintStream out) {
        if (best_totalTasks == 0) { out.println("---- 与最高分对应的系统性能统计 ----\n未记录任何有效的最佳结果。"); return; }
        double globalLocalHitRate = (double) best_localHits / Math.max(1, best_totalAccessed);
        double globalCloudHitRate = (double) best_cloudHits / Math.max(1, best_totalAccessed);
        double globalRemoteHitRate = (double) best_remoteAccesses / Math.max(1, best_totalAccessed);
        double completionRate = (double) best_completedTasks / best_totalTasks;
        double averageDelay = best_totalDelaySeconds / best_totalTasks;
        out.println("---- 与最高分对应的系统性能统计 ----");
        out.println("总处理任务数: " + best_totalTasks);
        out.printf("任务完成率: %.3f (%d / %d)\n", completionRate, best_completedTasks, best_totalTasks);
        out.printf("全局本地缓存命中率: %.3f\n", globalLocalHitRate);
        out.printf("全局边缘云命中率: %.3f\n", globalCloudHitRate);
        out.printf("全局远端访问率: %.3f\n", globalRemoteHitRate);
        out.printf("平均任务延迟: %.3f ms\n", averageDelay * 1000);
    }
}