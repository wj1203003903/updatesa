import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManager {
    // 成员变量
    LocalCache local;
    CloudCache cloud;
    RemoteCloud remote;

    // 本地总线速度
    private final double LOCAL_BUS_SPEED_BPS = 2 * 1e6;

    // 网络信道
    private final Channel cloudChannel;
    private final Channel remoteChannel;

    // 统计变量
    private double totalDelaySeconds = 0.0;
    int totalAccessed = 0, localHits = 0, cloudHits = 0;
    int localAccesses = 0, cloudAccesses = 0, remoteAccesses = 0;
    int completedTasks = 0, totalTasks = 0;

    // 用于在缓存中找不到数据时的最终查找
    private Map<Integer, DataItem> idToDataItemMap = new HashMap<>();

    /**
     * DataManager 的构造函数.
     */
    public DataManager(long localCap, long edgeCap) {
        local = new LocalCache(localCap);
        cloud = new CloudCache(edgeCap);
        remote = new RemoteCloud();

        local.bindLowerLevel(cloud);
        cloud.bindLowerLevel(remote);

        // 您提供的网络环境参数
        this.cloudChannel = new Channel(1 * 1e5, 0.15, 15.0, 5.0);
        this.remoteChannel = new Channel(5 * 1e4, 0.30, 10.0, 6.0);
    }

    /**
     * 模拟一次数据访问.
     */
    private double access(String type, int id, long currentTime) {
        totalAccessed++;
        double delaySeconds;

        // 1. 查本地缓存
        localAccesses++;
        DataItem item = local.get(type, id, currentTime);
        if (item != null) {
            localHits++;
            delaySeconds = (item.size * 8.0) / LOCAL_BUS_SPEED_BPS;
            return delaySeconds;
        }

        // 2. 查边缘云缓存
        cloudAccesses++;
        item = cloud.get(type, id, currentTime);
        if (item != null) {
            cloudHits++;
            delaySeconds = cloudChannel.getTotalDelay(item.size);
            local.put(item, currentTime);
            return delaySeconds;
        }

        // 3. 查远端云
        remoteAccesses++;
        item = remote.get(type, id, currentTime);
        if (item != null) {
            delaySeconds = remoteChannel.getTotalDelay(item.size);
            cloud.put(item, currentTime);
            local.put(item, currentTime);
            return delaySeconds;
        }

        // 4. 从数据注册表中查找（模拟数据源）
        item = idToDataItemMap.get(id);
        if (item != null) {
            delaySeconds = remoteChannel.getTotalDelay(item.size);
            cloud.put(item, currentTime);
            local.put(item, currentTime);
            return delaySeconds;
        }

        return Double.POSITIVE_INFINITY;
    }

    /**
     * 处理任务并返回其持续时间.
     */
    public long processAndGetDuration(DataItem item, long currentTime, int readyQueueSize) {
        totalTasks++;

        double accessDelaySeconds = access(item.type, item.id, currentTime);
        this.totalDelaySeconds += accessDelaySeconds;

        long durationMillis = (long)(accessDelaySeconds * 1000);
        long taskCompletionTime = currentTime + durationMillis;

        if (taskCompletionTime <= item.deadline) {
            completedTasks++;
        }

        return durationMillis;
    }

    /**
     * 注册一个数据项，用于最终查找.
     */
    public void registerDataItem(DataItem item) {
        idToDataItemMap.put(item.id, item);
    }

    /**
     * 将数据项添加到最底层的远端云.
     */
    public void addDataToRemote(DataItem item, long currentTime) {
        remote.put(item, currentTime);
    }

    /**
     * 计算系统综合得分.
     */
    public double getSystemScore() {
        if (totalAccessed == 0 || totalTasks == 0) return 0;

        double localHitRate = (double) localHits / Math.max(1, localAccesses);
        double cloudHitRate = (double) cloudHits / Math.max(1, cloudAccesses);
        double completionRate = (double) completedTasks / totalTasks;
        double averageDelay = totalDelaySeconds / totalTasks;

        // 您提供的评分公式权重
        double w1 = 10.0, w2 = 2.0, w3 = 10.0;

        return 100 * (w1 * localHitRate + w2 * cloudHitRate + w3 * completionRate) / (1.0 + averageDelay);
    }

    /**
     * L2 归一化一个向量.
     */
    public void normalizeL2(double[] vector) {
        double sumOfSquares = 0.0;
        for (double value : vector) {
            sumOfSquares += value * value;
        }
        if (sumOfSquares == 0) return;
        double l2Norm = Math.sqrt(sumOfSquares);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / l2Norm;
        }
    }

    /**
     * 重置所有统计数据，用于开始新的实验.
     */
    public void reset() {
        local.clear();
        cloud.clear();
        totalAccessed = 0;
        localHits = 0;
        cloudHits = 0;
        localAccesses = 0;
        cloudAccesses = 0;
        remoteAccesses = 0;
        completedTasks = 0;
        totalTasks = 0;
        totalDelaySeconds = 0.0;
    }

    /**
     * 线程安全版本的打印统计信息方法.
     * @param out 用于打印日志的独立 PrintStream
     */
    public void printStats(PrintStream out) {
        if (totalTasks == 0) {
            out.println("---- 系统性能统计 (全局口径) ----");
            out.println("未处理任何任务。");
            return;
        }

        double globalLocalHitRate = (double) localHits / Math.max(1, totalAccessed);
        double globalCloudHitRate = (double) cloudHits / Math.max(1, totalAccessed);
        int remoteHits = remoteAccesses;
        double globalRemoteHitRate = (double) remoteHits / Math.max(1, totalAccessed);
        double completionRate = (double) completedTasks / totalTasks;

        out.println("---- 系统性能统计 (全局口径) ----");
        out.println("总处理任务数: " + totalTasks);

        // --- 已修复：移除了不存在的 out.getLocale() ---
        out.printf("任务完成率: %.3f (%d / %d)\n", completionRate, completedTasks, totalTasks);
        out.printf("全局本地缓存命中率: %.3f\n", globalLocalHitRate);
        out.printf("全局边缘云命中率: %.3f\n", globalCloudHitRate);
        out.printf("全局远端访问率: %.3f\n", globalRemoteHitRate);
        out.printf("平均任务延迟: %.3f ms\n", (totalDelaySeconds / totalTasks) * 1000);
    }
}