import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

// 假设您项目中的DataItem类在同一个包或已导入
// import your.package.path.DataItem;

/**
 * 一个用于生成模拟数据的类，旨在创建具有“重任务”特性的数据集。
 * 主要特性包括：
 * 1.  ID流行度符合Zipfian分布：模拟真实世界的“热点”访问模式，以产生可控的缓存局部性。
 * 2.  不均衡的文件大小分布：通过对数正态分布生成跨越多个数量级的文件大小。
 * 3.  现实的时间约束：通过一个与任务大小解耦的Deadline窗口来模拟时间压力。
 */
public class DataGenerator {

    private final int numRequests;
    private final int numUniqueIds;
    private final Random random;
    private final Map<Integer, Integer> idToSizeMap;
    private final double[] zipfCdf;

    /**
     * DataGenerator 的构造函数。
     * @param numRequests  要生成的请求总数。
     * @param numUniqueIds 数据集中唯一ID的总数。
     * @param seed         用于随机数生成的种子，以确保可复现性。
     */
    public DataGenerator(int numRequests, int numUniqueIds, long seed) {
        this.numRequests = numRequests;
        this.numUniqueIds = numUniqueIds;
        this.random = new Random(seed);
        this.idToSizeMap = new HashMap<>();

        // 1. 生成文件大小分布 (Size Distribution)
        System.out.println("预生成 " + numUniqueIds + " 个唯一ID的固定尺寸 (已按比例缩小)...");

        // --- 【核心修改】将 mu 从 9.0 降低到 8.5，以使文件大小中位数接近5KB ---
        double mu = 8.6;
        double sigma = 2.2; // 保持不变以维持分布形状

        for (int id = 1; id <= numUniqueIds; id++) {
            double gaussian = random.nextGaussian();
            double logSize = mu + sigma * gaussian;
            int size = (int) Math.exp(logSize);

            size = Math.max(200, size);
            // 将上限也按比例缩小，例如到 150,000
            size = Math.min(size, 150000);
            this.idToSizeMap.put(id, size);
        }

        // 打印样本以供验证
        System.out.println("生成 Size 样本 (前20个):");
        for (int id = 1; id <= Math.min(20, numUniqueIds); id++) {
            System.out.println("ID " + id + ": " + this.idToSizeMap.get(id) + " bytes");
        }

        // 2. 预计算Zipfian分布 (ID Popularity Distribution)
        double alpha = 0.85;
        this.zipfCdf = setupZipfDistribution(alpha);
    }

    private double[] setupZipfDistribution(double alpha) {
        System.out.println("预计算Zipfian分布 (alpha=" + alpha + ") ...");
        double[] probabilities = new double[numUniqueIds];
        double sum = 0;
        for (int i = 0; i < numUniqueIds; i++) {
            probabilities[i] = 1.0 / Math.pow(i + 1, alpha);
            sum += probabilities[i];
        }

        double[] cdf = new double[numUniqueIds];
        double cumulative = 0;
        for (int i = 0; i < numUniqueIds; i++) {
            cumulative += (probabilities[i] / sum);
            cdf[i] = cumulative;
        }
        return cdf;
    }

    private int generateZipfId() {
        double p = random.nextDouble();
        int index = Arrays.binarySearch(this.zipfCdf, p);
        if (index < 0) {
            index = -(index + 1);
        }
        return index + 1;
    }

    /**
     * 生成一个符合Zipfian分布的请求序列。
     * @return DataItem数组。
     */
    public DataItem[] generateData() {
        System.out.println("生成合成数据集 (ID流行度服从Zipf分布)...");
        List<DataItem> dataList = new ArrayList<>();
        long currentTime = 804556800000L;
        for (int i = 0; i < numRequests; i++) {
            if (i > 0) currentTime += random.nextInt(4000);

            int id = generateZipfId();
            dataList.add(createHeavyTaskItem(id, currentTime));
        }
        return dataList.toArray(new DataItem[0]);
    }

    /**
     * 创建一个DataItem实例。
     * @param id          任务的ID。
     * @param arrivalTime 任务的到达时间。
     * @return 一个新的DataItem实例。
     */
    private DataItem createHeavyTaskItem(int id, long arrivalTime) {
        // --- 【核心修改】联动调整 Deadline 窗口 ---
        final long BASE_WINDOW_MS = 1500;

        // --- 2. 定义一个与对数尺寸相关的附加时间系数 ---
        // 这个系数决定了文件大小对deadline的影响程度。
        final double SIZE_FACTOR_MS = 300.0;

        int size = this.idToSizeMap.get(id);

        // --- 3. 计算与size弱相关的动态时间窗口 ---
        // 窗口 = 基础窗口 + 系数 * ln(size)
        // 使用 Math.log() 可以平滑size的巨大差异，实现“弱相关”。
        // 例如：
        // size=500B (ln≈6.2), bonus ≈ 620ms. Total window ≈ 2120ms
        // size=50000B (ln≈10.8), bonus ≈ 1080ms. Total window ≈ 2580ms
        // 文件大小增加了100倍，但deadline窗口只增加了约460ms。
        long dynamicWindow = BASE_WINDOW_MS + (long)(SIZE_FACTOR_MS * Math.log(Math.max(1, size)));

        // 增加随机抖动
        long randomJitter = random.nextInt(1000) - 400; // 在窗口基础上浮动 +/- 400ms

        // 最终的 deadline
        long deadline = arrivalTime + dynamicWindow + randomJitter;

        return new DataItem(id, size, "synthetic_request", arrivalTime, deadline);
    }
}