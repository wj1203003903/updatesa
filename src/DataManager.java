import java.util.HashMap;
import java.util.Map;

class DataManager {
    // 成员变量
    LocalCache local;
    CloudCache cloud;
    RemoteCloud remote;

    // 本地总线速度，暂时保持不变，仍然很快
    private final double LOCAL_BUS_SPEED_BPS = 2 * 1e5;

    // 网络信道，将在构造函数中初始化为性能较差的配置
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
     * --- 核心修改在此处 ---
     * 我们将 Channel 的参数设置得更差，以增加模拟的难度。
     */
    public DataManager(long localCap, long edgeCap) {
        local = new LocalCache(localCap);
        cloud = new CloudCache(edgeCap);
        remote = new RemoteCloud();

        local.bindLowerLevel(cloud);
        cloud.bindLowerLevel(remote);

        // --- 修改方案：创建一个更“拥堵”的网络环境 ---

        this.cloudChannel = new Channel(5 * 1e4, 0.200, 15.0, 5.0);   // 150KHz, 150ms
        this.remoteChannel = new Channel(5 * 1e4, 0.400, 10.0, 6.0);  // 400KHz, 500ms
    }

    /**
     * 模拟一次数据访问，该方法逻辑保持不变.
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
            local.put(item, currentTime); // 填充到本地缓存
            return delaySeconds;
        }

        // 3. 查远端云
        remoteAccesses++;
        item = remote.get(type, id, currentTime);
        if (item != null) {
            delaySeconds = remoteChannel.getTotalDelay(item.size);
            cloud.put(item, currentTime); // 填充到边缘云
            local.put(item, currentTime); // 填充到本地缓存
            return delaySeconds;
        }

        // 4. 作为最后的备用手段，直接从数据注册表中查找（模拟数据源）
        item = idToDataItemMap.get(id);
        if (item != null) {
            delaySeconds = remoteChannel.getTotalDelay(item.size);
            cloud.put(item, currentTime);

            local.put(item, currentTime);
            return delaySeconds;
        }

        // 如果数据完全不存在
        return Double.POSITIVE_INFINITY;
    }

    /**
     * 处理一个任务并返回其耗时，该方法逻辑保持不变.
     */
    public long processAndGetDuration(DataItem item, long currentTime, int readyQueueSize) {
        totalTasks++;

        // 任务的唯一耗时就是数据访问时延
        double accessDelaySeconds = access(item.type, item.id, currentTime);

        // 将访问时延累加到总延迟
        this.totalDelaySeconds += accessDelaySeconds;

        // 计算完成时间并判断是否超时
        long durationMillis = (long)(accessDelaySeconds * 1000);
        long taskCompletionTime = currentTime + durationMillis;

        if (taskCompletionTime <= item.deadline) {
            completedTasks++;
        }

        // 返回这个唯一的耗时
        return durationMillis;
    }

    // --- 其他辅助方法和统计方法保持不变 ---

    public void registerDataItem(DataItem item) {
        idToDataItemMap.put(item.id, item);
    }

    public void addDataToRemote(DataItem item, long currentTime) {
        remote.put(item, currentTime);
    }

    public double getSystemScore() {
        if (totalAccessed == 0 || totalTasks == 0) return 0;

        double localHitRate = (double) localHits / Math.max(1, localAccesses);
        double cloudHitRate = (double) cloudHits / Math.max(1, cloudAccesses);
        double completionRate = (double) completedTasks / totalTasks;
        double averageDelay = totalDelaySeconds / totalTasks;

        // 评分公式权重，可以根据需要调整
        double w1 = 4.0, w2 = 2.0, w3 = 8.0;

        // 惩罚项：平均延迟越高，分母越大，总分越低
        return 100*(w1 * localHitRate + w2 * cloudHitRate + w3 * completionRate) / (1.0 + averageDelay);
    }

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

    public void printStats() {
        if (totalTasks == 0) { /* ... */ return; }

        // --- 核心修改在此处 ---

        // 全局本地命中率 = (本地命中数) / (总请求数)
        double globalLocalHitRate = (double) localHits / Math.max(1, totalAccessed);

        // 全局边缘云命中率 = (边缘云命中数) / (总请求数)
        double globalCloudHitRate = (double) cloudHits / Math.max(1, totalAccessed);

        // 全局远端命中率 (所有穿透到最底层的请求都视为在远端“命中”)
        int remoteHits = remoteAccesses; // 假设所有到底层的请求都能在数据源找到
        double globalRemoteHitRate = (double) remoteHits / Math.max(1, totalAccessed);


        double completionRate = (double) completedTasks / totalTasks;

        System.out.println("---- 系统性能统计 (全局口径) ----");
        System.out.println("总处理任务数: " + totalTasks);
        System.out.printf("任务完成率: %.3f (%d / %d)\n", completionRate, completedTasks, totalTasks);

        System.out.printf("全局本地缓存命中率: %.3f\n", globalLocalHitRate);
        System.out.printf("全局边缘云命中率: %.3f\n", globalCloudHitRate);
        System.out.printf("全局远端访问率: %.3f\n", globalRemoteHitRate);
        System.out.printf("平均任务延迟: %.3f ms\n", (totalDelaySeconds / totalTasks) * 1000);
    }
}