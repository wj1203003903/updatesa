import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DataGenerator {

    private final int numRequests;
    private final int numUniqueIds;
    private final Random random;

    // --- 【核心修改】新增一个Map来存储每个ID唯一的尺寸 ---
    private final Map<Integer, Integer> idToSizeMap;

    public DataGenerator(int numRequests, int numUniqueIds, long seed) {
        this.numRequests = numRequests;
        this.numUniqueIds = numUniqueIds;
        this.random = new Random(seed);

        // --- 【核心修改】在构造函数中，预先为所有唯一ID生成并存储它们的固定尺寸 ---
        this.idToSizeMap = new HashMap<>();
        System.out.println("预生成 " + numUniqueIds + " 个唯一ID的固定尺寸...");
        for (int id = 1; id <= numUniqueIds; id++) {
            // 为每个ID生成一个随机但固定的尺寸，并存入Map
            // 生成 Size 的逻辑与之前相同 (均值4000, 标准差2000, 最小500)
            int size = (int) Math.max(500, random.nextGaussian() * 2000 + 4000);
            this.idToSizeMap.put(id, size);
        }
    }

    public DataItem[] generateNormalDistribution() {
        System.out.println("生成正态分布数据 (ID与Size固定)...");
        List<DataItem> dataList = new ArrayList<>();
        long currentTime = 804556800000L;

        for (int i = 0; i < numRequests; i++) {
            if (i > 0) {
                currentTime += random.nextInt(4000);
            }

            // 生成ID (逻辑不变)
            double meanId = numUniqueIds / 2.0;
            double stdDevId = numUniqueIds / 6.0;
            double gaussianId = random.nextGaussian() * stdDevId + meanId;
            int id = (int) Math.round(Math.max(1, Math.min(gaussianId, numUniqueIds)));

            // 创建DataItem，此时会使用预先生成的固定size
            DataItem item = createItemWithFixedAttributes(id, currentTime);
            dataList.add(item);
        }
        return dataList.toArray(new DataItem[0]);
    }

    public DataItem[] generateExponentialDistribution() {
        System.out.println("生成指数分布数据 (ID与Size固定)...");
        List<DataItem> dataList = new ArrayList<>();
        long currentTime = 804556800000L;

        for (int i = 0; i < numRequests; i++) {
            if (i > 0) {
                currentTime += random.nextInt(4000);
            }

            // 生成ID (逻辑不变)
            double lambda = 5.0;
            double exponentialValue = -Math.log(1 - random.nextDouble()) / lambda;
            double mappedValue = 1 - Math.exp(-exponentialValue);
            int id = (int) (mappedValue * numUniqueIds) + 1;

            // 创建DataItem，此时会使用预先生成的固定size
            DataItem item = createItemWithFixedAttributes(id, currentTime);
            dataList.add(item);
        }
        return dataList.toArray(new DataItem[0]);
    }

    /**
     * --- 【核心修改】辅助方法已重命名并修改逻辑 ---
     * 它不再随机生成size，而是从Map中查询。
     */
    private DataItem createItemWithFixedAttributes(int id, long arrivalTime) {
        final long BASE_TIME_WINDOW_MS = 400;
        final double EXTRA_TIME_PER_BYTE = 0.05;

        // --- 从Map中获取这个ID对应的、预先生成的固定尺寸 ---
        int size = this.idToSizeMap.get(id);

        // 根据固定的size和当前的arrivalTime生成Deadline
        long deadline = arrivalTime + BASE_TIME_WINDOW_MS + (long)(size * EXTRA_TIME_PER_BYTE);

        return new DataItem(id, size, "synthetic_request", arrivalTime, deadline);
    }
}