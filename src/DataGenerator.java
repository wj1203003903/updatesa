import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DataGenerator {

    private final int numRequests;
    private final int numUniqueIds;
    private final Random random;
    private final Map<Integer, Integer> idToSizeMap;

    public DataGenerator(int numRequests, int numUniqueIds, long seed) {
        this.numRequests = numRequests;
        this.numUniqueIds = numUniqueIds;
        this.random = new Random(seed);
        this.idToSizeMap = new HashMap<>();

        System.out.println("预生成 " + numUniqueIds + " 个唯一ID的固定尺寸 (模拟真实数据分布，上限200KB)...");

        // 底层正态分布的参数 (保持不变)
        double mu = 9.0;
        double sigma = 2.2;

        for (int id = 1; id <= numUniqueIds; id++) {
            double gaussian = random.nextGaussian();
            double logSize = mu + sigma * gaussian;
            int size = (int) Math.exp(logSize);

            // 首先，确保一个最小尺寸
            size = Math.max(100, size);

            // --- 【核心修改】在这里增加一个最大尺寸的硬上限 ---
            size = Math.min(size, 200000); // 确保最大尺寸不超过 200,000 字节

            this.idToSizeMap.put(id, size);
        }

        // (可选但推荐) 打印一些样本大小以供验证
        System.out.println("生成 Size 样本 (前20个):");
        for (int id = 1; id <= Math.min(20, numUniqueIds); id++) {
            System.out.println("ID " + id + ": " + this.idToSizeMap.get(id) + " bytes");
        }
    }

    // ... 下面的 generateNormalDistribution, generateExponentialDistribution,
    // และ createHeavyTaskItem 方法保持不变...

    public DataItem[] generateNormalDistribution() {
        System.out.println("生成正态分布的'重任务'数据 (ID与Size固定, Deadline窗口现实)...");
        List<DataItem> dataList = new ArrayList<>();
        long currentTime = 804556800000L;

        for (int i = 0; i < numRequests; i++) {
            if (i > 0) {
                currentTime += random.nextInt(4000);
            }

            double meanId = numUniqueIds / 2.0;
            double stdDevId = numUniqueIds / 6.0;
            double gaussianId = random.nextGaussian() * stdDevId + meanId;
            int id = (int) Math.round(Math.max(1, Math.min(gaussianId, numUniqueIds)));

            DataItem item = createHeavyTaskItem(id, currentTime);
            dataList.add(item);
        }
        return dataList.toArray(new DataItem[0]);
    }

    public DataItem[] generateExponentialDistribution() {
        System.out.println("生成指数分布的'重任务'数据 (ID与Size固定, Deadline窗口现实)...");
        List<DataItem> dataList = new ArrayList<>();
        long currentTime = 804556800000L;

        for (int i = 0; i < numRequests; i++) {
            if (i > 0) {
                currentTime += random.nextInt(4000);
            }

            double lambda = 5.0;
            double exponentialValue = -Math.log(1 - random.nextDouble()) / lambda;
            double mappedValue = 1 - Math.exp(-exponentialValue);
            int id = (int) (mappedValue * numUniqueIds) + 1;

            DataItem item = createHeavyTaskItem(id, currentTime);
            dataList.add(item);
        }
        return dataList.toArray(new DataItem[0]);
    }

    private DataItem createHeavyTaskItem(int id, long arrivalTime) {
        final long USER_TOLERANCE_WINDOW_MS = 2000;
        long randomJitter = random.nextInt(1001) - 500;
        long deadline = arrivalTime + USER_TOLERANCE_WINDOW_MS + randomJitter;
        int size = this.idToSizeMap.get(id);

        return new DataItem(id, size, "synthetic_request", arrivalTime, deadline);
    }
}