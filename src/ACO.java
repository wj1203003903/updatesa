import java.util.Arrays;
import java.util.Random;

public class ACO {
    // --- 参数 ---
    private static final int ANT_COUNT = 20;
    private static final int GENERATIONS = 130;
    private static final int DIMENSIONS = 5;

    private static final double ALPHA = 0.4;
    private static final double EVAPORATION = 0.9;
    private static final double Q = 50.0;

    // --- 【核心修改】引入粒度参数 ---
    private static final int GRANULARITY = 41; // 将解空间离散化为41个点 (0.00, 0.025, ..., 1.00)

    // --- 成员变量 ---
    private DataItem[] testData;
    private DataManager baseDM;
    private Random random;
    private double[][] pheromones;

    public ACO(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random(Main.randomseal);

        // 【修改】信息素矩阵的大小现在由GRANULARITY决定
        this.pheromones = new double[DIMENSIONS][GRANULARITY];
        for (int i = 0; i < DIMENSIONS; i++) {
            // 初始化信息素，可以简单地设为一个固定值或随机值
            Arrays.fill(pheromones[i], 1.0);
        }
    }

    public double run() {
        double bestScore = -Double.MAX_VALUE;
        double[] best = new double[DIMENSIONS];
        System.out.println("ACO Initializing (Granularity: " + GRANULARITY + ")...");

        for (int gen = 0; gen < GENERATIONS; gen++) {
            double[][] ants = new double[ANT_COUNT][DIMENSIONS];
            double[] scores = new double[ANT_COUNT];

            for (int k = 0; k < ANT_COUNT; k++) {
                // 每只蚂蚁构建一个解
                for (int d = 0; d < DIMENSIONS; d++) {
                    ants[k][d] = selectValue(d);
                }

                // 评估解
                scores[k] = evaluate(ants[k]);
                if (scores[k] > bestScore) {
                    bestScore = scores[k];
                    best = ants[k].clone();
                }
            }

            // 更新信息素
            // 1. 信息素挥发
            for (int i = 0; i < DIMENSIONS; i++) {
                for (int j = 0; j < GRANULARITY; j++) {
                    pheromones[i][j] *= EVAPORATION;
                }
            }

            // 2. 信息素增强
            for (int k = 0; k < ANT_COUNT; k++) {
                for (int d = 0; d < DIMENSIONS; d++) {
                    // 【修改】将连续值映射到新的离散索引
                    int idx = (int) Math.round(ants[k][d] * (GRANULARITY - 1));
                    pheromones[d][idx] += Q * (scores[k] / Math.max(bestScore, 1e-6));
                }
            }

            double improvement = bestScore - Main.baselineScore;
            System.out.printf("Gen %2d: Improvement = %.4f\n",
                    gen + 1, improvement > 0 ? improvement : 0);
        }

        // 结束打印
        System.out.println("\n=== ACO Finished ===");
        double[] finalNormalizedBest = best.clone();
        baseDM.normalizeL2(finalNormalizedBest);
        System.out.printf("Final Best Weights (Normalized) = %s\n", Arrays.toString(finalNormalizedBest));
        evaluate(best);
        baseDM.printStats();
        return bestScore;
    }

    // 轮盘赌选择
    private double selectValue(int dimension) {
        // 【修改】概率数组的大小现在由GRANULARITY决定
        double[] probs = new double[GRANULARITY];
        double sum = 0;

        for (int i = 0; i < GRANULARITY; i++) {
            double pheromone = pheromones[dimension][i];
            probs[i] = Math.pow(pheromone, ALPHA);
            sum += probs[i];
        }

        if (sum == 0 || Double.isNaN(sum)) {
            // 如果信息素总和为0（罕见情况），则随机选择一个值
            int randomIndex = random.nextInt(GRANULARITY);
            return (double) randomIndex / (GRANULARITY - 1);
        }

        double r = random.nextDouble() * sum;
        for (int i = 0; i < GRANULARITY; i++) {
            r -= probs[i];
            if (r <= 0) {
                // 【修改】将索引转换回 [0, 1] 范围内的连续值
                return (double) i / (GRANULARITY - 1);
            }
        }

        // 作为浮点数误差的保障
        return 1.0;
    }

    private double evaluate(double[] weights) {
        return DataTest.score(weights, testData, baseDM);
    }
}