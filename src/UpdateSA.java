import java.util.Arrays;
import java.util.Random;

/**
 * 增强型模拟退火（SA）实现：
 * - 支持在不同子空间（subspace）进行搜索（可同时扰动多个维度）
 * - 扰动幅度随温度动态缩放（高温大扰动，低温小扰动）
 * - 每次邻域解后做局部微调（local search）
 * - 自适应降温：若长时间无提升则放慢降温
 * - 动态每温度迭代次数（高温更多尝试）
 *
 * 依赖外部：DataTest.score(weights, testData, baseDM)
 *           baseDM.normalizeL2(weights)
 *           baseDM.printStats()
 */
public class UpdateSA {
    private static final int DIMENSIONS = 4;

    // 基本温度参数（可调整）
    private static final double INIT_TEMP = 100.0;
    private static final double MIN_TEMP = 1e-3;
    private static final double COOLING_RATE_BASE = 0.9; // 基础降温系数（比0.6稳健）
    private static final int ITERATIONS_PER_TEMP_BASE = 50; // 基础每温度迭代次数

    // 局部搜索与扰动参数
    private static final double BASE_STEP = 0.2;    // 基础扰动标准差（高温时乘温度因子）
    private static final double LOCAL_DELTA = 0.03; // 局部微调步长（±LOCAL_DELTA）
    private static final double LAMBDA = 1.0;       // 接受概率归一化因子，可调

    // 自适应降温控制
    private static final int STAGNATION_THRESHOLD = 6; // 连续几次温度没有提升后认为“停滞”
    private static final double SLOWDOWN_FACTOR = 0.98; // 停滞时把降温系数乘以该值（使降温更慢）

    private final DataItem[] testData;
    private final DataManager baseDM;
    private final Random random;

    public UpdateSA(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();
    }

    public void run() {
        // 初始化
        double[] current = randomWeights();
        double currentScore = evaluate(current);

        double[] best = current.clone();
        double bestScore = currentScore;

        double temperature = INIT_TEMP;
        double coolingRate = COOLING_RATE_BASE;

        int stagnationCount = 0; // 连续未改进计数

        while (temperature > MIN_TEMP) {
            // 动态每温度迭代次数：高温多尝试，低温少尝试
            int iterations = ITERATIONS_PER_TEMP_BASE + (int) (ITERATIONS_PER_TEMP_BASE * (temperature / INIT_TEMP));

            boolean improvedAtThisTemp = false;
            for (int it = 0; it < iterations; it++) {
                // 选择子空间（基于温度决定维度数量）
                int[] subspace = chooseSubspace(temperature);

                // 生成邻域解（在选中的子空间扰动）
                double[] neighbor = generateNeighbor(current, subspace, temperature);

                // 对邻域解做局部微调（只在子空间内微调）
                localSearch(neighbor, subspace);

                double neighborScore = evaluate(neighbor);

                // 接受准则
                if (acceptanceProbability(currentScore, neighborScore, temperature) > random.nextDouble()) {
                    current = neighbor;
                    currentScore = neighborScore;

                    if (currentScore > bestScore) {
                        best = current.clone();
                        bestScore = currentScore;
                        improvedAtThisTemp = true;
                    }
                }
            }

            // 自适应降温：若本温度段无提升，则认为可能停滞 -> 放慢降温
            if (!improvedAtThisTemp) {
                stagnationCount++;
            } else {
                stagnationCount = 0;
            }

            if (stagnationCount >= STAGNATION_THRESHOLD) {
                // 放慢降温，给算法更多探索机会
                coolingRate = COOLING_RATE_BASE * SLOWDOWN_FACTOR;
            } else {
                coolingRate = COOLING_RATE_BASE;
            }

            // 应用降温
            temperature *= coolingRate;
        }

        // 规范化并输出结果
        baseDM.normalizeL2(best);
        System.out.println("=== SA (enhanced) Finished ===");
        System.out.printf("Best Score = %.4f\nBest Weights = %s\n", bestScore * 100.0, Arrays.toString(best));
        baseDM.printStats();
    }

    // -------------------- 核心方法 --------------------

    // 随机初始化权重（0~1）
    private double[] randomWeights() {
        double[] w = new double[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            w[i] = random.nextDouble();
        }
        return w;
    }

    /**
     * 根据温度选择子空间（选择哪些维度一起扰动）
     * 高温时倾向于选择更多维度，低温时选择较少维度（精调）
     */
    private int[] chooseSubspace(double temperature) {
        double tRatio = Math.min(1.0, temperature / INIT_TEMP); // [0,1]
        // 决定同时扰动的维度数量：至少1，最多DIMENSIONS
        // 比例映射：高温 -> 更大子空间
        int maxDims = DIMENSIONS;
        int minDims = 1;
        int k = (int) Math.round(minDims + (maxDims - minDims) * tRatio);

        // 为了多样性：以概率选择具体维度（随机采样 k 个索引）
        int[] indices = randomSampleIndices(k);
        return indices;
    }

    // 从 [0..DIMENSIONS-1] 中随机采样 k 个不重复索引
    private int[] randomSampleIndices(int k) {
        k = Math.max(1, Math.min(k, DIMENSIONS));
        int[] all = new int[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) all[i] = i;
        // Fisher-Yates 前 k 个
        for (int i = 0; i < k; i++) {
            int j = i + random.nextInt(DIMENSIONS - i);
            int tmp = all[i];
            all[i] = all[j];
            all[j] = tmp;
        }
        int[] res = new int[k];
        System.arraycopy(all, 0, res, 0, k);
        return res;
    }

    /**
     * 在给定子空间上扰动生成邻域解
     * 扰动幅度按温度缩放：高温大步长，低温小步长
     */
    private double[] generateNeighbor(double[] current, int[] subspace, double temperature) {
        double[] neighbor = current.clone();

        // 温度因子，使高温时扰动更大
        double tempFactor = Math.min(1.0, temperature / INIT_TEMP); // [0,1]
        double sigma = BASE_STEP * (0.3 + 0.7 * tempFactor); // sigma 基础上随温度变化（避免太小）

        for (int idx : subspace) {
            neighbor[idx] += random.nextGaussian() * sigma;
            // 保证边界
            if (neighbor[idx] < 0) neighbor[idx] = 0;
            if (neighbor[idx] > 1) neighbor[idx] = 1;
        }
        return neighbor;
    }

    /**
     * 局部搜索：对子空间内每个维度做 ±LOCAL_DELTA 的小幅尝试（贪心）
     * 只接受在子空间内部的改进（避免修改非子空间维度）
     */
    private void localSearch(double[] candidate, int[] subspace) {
        double bestLocalScore = evaluate(candidate);
        double[] bestLocal = candidate.clone();

        // 对每个维度尝试 +delta / -delta
        for (int idx : subspace) {
            // 尝试 +delta
            double old = candidate[idx];
            candidate[idx] = clamp(old + LOCAL_DELTA, 0.0, 1.0);
            double sPlus = evaluate(candidate);
            if (sPlus > bestLocalScore) {
                bestLocalScore = sPlus;
                bestLocal = candidate.clone();
            }
            // 恢复并尝试 -delta
            candidate[idx] = clamp(old - LOCAL_DELTA, 0.0, 1.0);
            double sMinus = evaluate(candidate);
            if (sMinus > bestLocalScore) {
                bestLocalScore = sMinus;
                bestLocal = candidate.clone();
            }
            // 恢复为原值以便下一个维度尝试
            candidate[idx] = old;
        }

        // 将 candidate 更新为局部最优（如果有提升）
        System.arraycopy(bestLocal, 0, candidate, 0, DIMENSIONS);
    }

    // 接受概率（Metropolis），引入 lambda 缩放
    private double acceptanceProbability(double currentScore, double neighborScore, double temperature) {
        if (neighborScore > currentScore) {
            return 1.0;
        }
        // 注意：temperature 可能较小，除数不为0
        return Math.exp((neighborScore - currentScore) / (LAMBDA * Math.max(1e-12, temperature)));
    }

    // 评分包装（调用外部 DataTest）
    private double evaluate(double[] weights) {
        return DataTest.score(weights, testData, baseDM);
    }

    // 工具：夹住区间
    private double clamp(double v, double lo, double hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }
}
