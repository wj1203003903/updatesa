import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 用于生成模拟数据请求的数据生成器。
 * 可以生成符合不同概率分布（均匀、正态、指数）的访问序列。
 */
public class DataGenerator {

    private final int numRequests;    // 要生成的总请求数量
    private final int numUniqueIds;   // 唯一的ID总数 (例如 1200)
    private final Random random;      // 用于生成随机数的实例

    /**
     * 构造函数
     * @param numRequests  要生成的请求总数, 例如 20000
     * @param numUniqueIds 唯一ID的数量, 例如 1200。ID将从1生成到numUniqueIds。
     * @param seed         随机种子，用于确保每次生成的序列都相同，保证实验的可复现性。
     */
    public DataGenerator(int numRequests, int numUniqueIds, long seed) {
        this.numRequests = numRequests;
        this.numUniqueIds = numUniqueIds;
        this.random = new Random(seed);
    }
    /**
     * 生成符合正态分布（高斯分布）的访问数据。
     * 特点：大部分请求会集中在ID的中间部分（例如ID 600附近），形成明显的“热点数据”。
     * 这模拟了流行的内容被频繁访问的场景。
     * @return DataItem数组
     */
    public DataItem[] generateNormalDistribution() {
        System.out.println("生成正态分布数据 (请求数: " + numRequests + ", 唯一ID数: " + numUniqueIds + ")");
        List<DataItem> dataList = new ArrayList<>();
        long currentTime = 804556800000L;
        double meanId = numUniqueIds / 2.0;
        double stdDevId = numUniqueIds / 6.0; // 标准差，控制ID的集中程度，值越小越集中

        for (int i = 0; i < numRequests; i++) {
            double gaussianId = random.nextGaussian() * stdDevId + meanId;

            // 将生成的高斯值限制在 [1, numUniqueIds] 的范围内
            int id = (int) Math.round(Math.max(1, Math.min(gaussianId, numUniqueIds)));

            DataItem item = createItemWithRandomizedAttributes(id, currentTime);
            dataList.add(item);
            currentTime += 1000 + random.nextInt(500);
        }
        return dataList.toArray(new DataItem[0]);
    }

    /**
     * 生成符合指数分布的访问数据。
     * 特点：极少数ID（ID号较小的）被极高频率地访问，大部分ID很少被访问。
     * 这模拟了“长尾效应”，例如少数超级热门项目和大量冷门项目。
     * @return DataItem数组
     */
    public DataItem[] generateExponentialDistribution() {
        System.out.println("生成指数分布数据 (请求数: " + numRequests + ", 唯一ID数: " + numUniqueIds + ")");
        List<DataItem> dataList = new ArrayList<>();
        long currentTime = 804556800000L;
        // lambda参数控制衰减速度，值越大，访问越集中在少数几个ID上
        double lambda = 5.0;

        for (int i = 0; i < numRequests; i++) {
            // 使用反函数变换法从均匀分布生成指数分布
            double exponentialValue = -Math.log(1 - random.nextDouble()) / lambda;

            // 将[0, inf)的指数分布值映射到[0, 1)的区间，使得值越小概率越大
            double mappedValue = 1 - Math.exp(-exponentialValue);

            // 将[0, 1)的概率值映射到[1, numUniqueIds]的ID上
            int id = (int) (mappedValue * numUniqueIds) + 1;

            DataItem item = createItemWithRandomizedAttributes(id, currentTime);
            dataList.add(item);
            currentTime += 1000 + random.nextInt(500);
        }
        return dataList.toArray(new DataItem[0]);
    }

    /**
     * 辅助方法，为给定的ID生成随机的大小和截止时间，创建一个DataItem对象。
     * @param id          数据的唯一ID。
     * @param arrivalTime 任务（请求）的到达时间。
     * @return 一个属性随机化的DataItem实例。
     */
    private DataItem createItemWithRandomizedAttributes(int id, long arrivalTime) {
        // 生成Size (正态分布，均值4000，标准差2000，最小为500)
        int size = (int) Math.max(500, random.nextGaussian() * 2000 + 4000);

        // 生成Deadline (到达时间 + 基础延迟 + 与大小相关的延迟)
        // 给予一个基础的反应时间窗口，并根据数据大小增加难度
        long deadline = arrivalTime + 3000 + (long)(size * 0.1);

        return new DataItem(id, size, "synthetic_request", arrivalTime, deadline);
    }
}