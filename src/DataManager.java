import java.util.*;
// 数据管理类
class DataManager {
    LocalCache local;
    CloudCache cloud;
    RemoteCloud remote;

    private final double LOCAL_BUS_SPEED_BPS = 10 * 1e9; // 本地总线速度 10Gbps
    private final Channel cloudChannel;
    private final Channel remoteChannel;

    // --- 改动 2: 增加一个 totalDelay 实例变量来累加动态延迟 ---
    private double totalDelay = 0.0;

    int totalAccessed = 0;
    int localHits = 0;
    int cloudHits = 0;

    int localAccesses = 0;
    int cloudAccesses = 0;
    int remoteAccesses = 0;
    
    private Map<Integer, DataItem> idToDataItemMap = new HashMap<>();
    
    public double getSystemScore() {
        if (localAccesses == 0 || cloudAccesses == 0) return 0; // 避免除零错误

        double localHitRate = (double) localHits / localAccesses;
        double cloudHitRate = (double) cloudHits / cloudAccesses;

        // --- 改动 3: 使用累加的 totalDelay 实例变量 ---
        // 注意：这里的惩罚因子 10000 可能需要根据 totalDelay 的实际数量级进行调整
        // 比如，如果总访问次数是1000次，平均延迟0.05秒，totalDelay就是50.
        // (1 + 50/10000) 变化不大。可以考虑用 (1 + totalDelay / totalAccessed) 平均延迟
        double averageDelay = (totalAccessed > 0) ? totalDelay / totalAccessed : 0;
        
        double w1 = 3.0;
        double w2 = 1.0;
        // 使用平均延迟作为惩罚项，更具可比性
        return (w1 * localHitRate + w2 * cloudHitRate) / (1.0 + averageDelay * 10); // 乘以10放大惩罚效果
    }

    public DataManager(int localCap, int edgeCap) {
        local = new LocalCache(localCap);
        cloud = new CloudCache(edgeCap);
        remote = new RemoteCloud();

        local.bindLowerLevel(cloud);
        cloud.bindLowerLevel(remote);

        // --- 改动 4: 在构造函数中初始化信道对象 ---
        // 配置 Cloud Channel (CDN)
        this.cloudChannel = new Channel(20 * 1e6, 0.030, 100.0, 40.0);
        // 配置 Remote Channel (源站)
        this.remoteChannel = new Channel(100 * 1e6, 0.150, 50.0, 30.0);
    }
    
    // generateDataItem 方法保持不变
    public DataItem generateDataItem(int id) {
        if (idToDataItemMap.containsKey(id)) {
            return idToDataItemMap.get(id);
        }
        Random rand = new Random(id);
        int size = rand.nextInt(50) + 10;
        String type = "type" + rand.nextInt(5);
        int frequency = rand.nextInt(10) + 1;
        int priority = rand.nextInt(3) + 1;
        DataItem item = new DataItem(id, size, type, frequency, priority);
        idToDataItemMap.put(id, item);
        return item;
    }

    // --- 改动 5: 修改 access 方法以计算和累加动态延迟 ---
    public DataItem access(String type, int priority, int id) {
        totalAccessed++;
        
        // 访问本地
        localAccesses++;
        DataItem item = local.get(type, priority, id);
        if (item != null) {
            localHits++;
            // 计算本地延迟并累加
            this.totalDelay += (item.size * 8.0) / LOCAL_BUS_SPEED_BPS;
            return item;
        }

        // 访问云端
        cloudAccesses++;
        item = cloud.get(type, priority, id);
        if (item != null) {
            cloudHits++;
            // 计算云端延迟并累加
            this.totalDelay += cloudChannel.getTotalDelay(item.size);
            local.put(item);

            return item;
        }
        
        // 访问远程
        remoteAccesses++;
        item = remote.get(type, priority, id);
        if (item != null) {
            // 计算远程延迟并累加
            this.totalDelay += remoteChannel.getTotalDelay(item.size);
            local.put(item);
            cloud.put(item);
            return item;
        }

        // 生成新数据
        item = generateDataItem(id);
        // 新数据从远程获取，计算延迟并累加
        this.totalDelay += remoteChannel.getTotalDelay(item.size);
        // 将新数据放入缓存
        remote.put(item);
        cloud.put(item);
        local.put(item);
        return item;
    }

    // addDataToRemote 方法保持不变
    public void addDataToRemote(DataItem item) {
        remote.put(item);
    }
    public void normalizeL2(double[] vector) {
        if (vector == null || vector.length != 4) {
            throw new IllegalArgumentException("向量必须存在且长度为4。");
        }

        // 1. 计算所有元素平方和的平方根 (L2 范数)
        double sumOfSquares = 0.0;
        for (double value : vector) {
            sumOfSquares += value * value;
        }
        
        // 如果所有元素都是0，L2范数为0，这是特殊情况
        if (sumOfSquares == 0) {
            // 所有元素都是0，无法正则化，保持原样即可。
            // 或者可以根据业务需求设置为特定值，但保持为0最常见。
            return;
        }

        double l2Norm = Math.sqrt(sumOfSquares);

        // 2. 将每个元素除以 L2 范数
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / l2Norm;
        }
    }
    public void reset() {
        local.dataMap.clear();
        cloud.dataMap.clear();
        
        // 重置所有计数器
        totalAccessed = 0;
        localHits = 0;
        cloudHits = 0;
        localAccesses = 0;
        cloudAccesses = 0;
        remoteAccesses = 0;
        
        // --- 改动 6: 重置 totalDelay ---
        totalDelay = 0.0;
    }

    public void printStats() {
        if (localAccesses == 0 || cloudAccesses == 0) {
            System.out.println("访问次数不足，无法生成统计数据。");
            return;
        }
        
        double localHitRate = (double) localHits / localAccesses;
        double cloudHitRate = (double) cloudHits / cloudAccesses;

        System.out.println("==== 访问统计 ====");
        System.out.println("总数据访问次数: " + totalAccessed);
        System.out.printf("本地命中率: %.3f\n", localHitRate);
        System.out.printf("云端命中率: %.3f\n", cloudHitRate);
        System.out.printf("总时延: %.3f s\n", totalDelay); // 单位是秒
        System.out.printf("平均时延: %.3f ms\n", (totalDelay / totalAccessed) * 1000); // 打印更有意义的平均延迟
    }
}