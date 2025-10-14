import java.util.Arrays;
import java.util.Random;

public class ACO {
    // --- 参数 ---
    private static final int ANT_COUNT = 20;           // 蚂蚁数量
    private static final int GENERATIONS = 40;         // 迭代次数
    private static final int DIMENSIONS = 5;           // 权重维度 (已更正为5)
    private static final double ALPHA = 1.0;           // 信息素影响因子
    private static final double BETA = 2.0;            // 启发式影响因子
    private static final double EVAPORATION = 0.5;     // 信息素挥发率
    private static final double Q = 100.0;             // 信息素增强常数

    // --- 成员变量 ---
    private DataItem[] testData;
    private DataManager baseDM;
    private Random random;
    private double[][] pheromones;  // 信息素矩阵

    public ACO(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();

        // 将解空间离散化为11个点 (0.0, 0.1, ..., 1.0)
        this.pheromones = new double[DIMENSIONS][11];
        for (int i = 0; i < DIMENSIONS; i++) {
            Arrays.fill(pheromones[i], 1.0); // 初始信息素为1
        }
    }

    public double run() {
        double bestScore = -Double.MAX_VALUE;
        double[] best = new double[DIMENSIONS];
        System.out.println("ACO Initializing...");

        // --- 主循环，增加监控 ---
        for (int gen = 0; gen < GENERATIONS; gen++) {
            double lastGenBestScore = bestScore; // 记录本代开始前的最优分数

            double[][] ants = new double[ANT_COUNT][DIMENSIONS];
            double[] scores = new double[ANT_COUNT];

            // 每只蚂蚁构建解
            for (int k = 0; k < ANT_COUNT; k++) {
                for (int d = 0; d < DIMENSIONS; d++) {
                    ants[k][d] = selectValue(d);
                }
                scores[k] = evaluate(ants[k]);
                if (scores[k] > bestScore) {
                    bestScore = scores[k];
                    best = ants[k].clone();
                }
            }

            // 信息素挥发
            for (int i = 0; i < DIMENSIONS; i++) {
                for (int j = 0; j < 11; j++) {
                    pheromones[i][j] *= (1 - EVAPORATION);
                }
            }

            // 更新信息素
            for (int k = 0; k < ANT_COUNT; k++) {
                for (int d = 0; d < DIMENSIONS; d++) {
                    int idx = (int) Math.round(ants[k][d] * 10);
                    // 适应度越高，增加的信息素越多
                    pheromones[d][idx] += Q * (scores[k] / Math.max(bestScore, 1e-6));
                }
            }

            double improvement = bestScore - Main.baselineScore;
            System.out.printf("Gen %2d: Improvement = %.4f\n",
                    gen + 1, improvement>0?improvement:0);
        }

        System.out.println("\n=== ACO Finished ===");
        double[] finalNormalizedBest = best.clone();
        baseDM.normalizeL2(finalNormalizedBest);
        System.out.printf("Final Best Weights (Normalized) = %s\n", Arrays.toString(finalNormalizedBest));

        evaluate(best); // 使用原始最优解评估
        baseDM.printStats();
        return bestScore;
    }

    // 轮盘赌选择
    private double selectValue(int dimension) {
        double[] probs = new double[11];
        double sum = 0;

        for (int i = 0; i < 11; i++) {
            double pheromone = pheromones[dimension][i];
            double heuristic = (i + 1) / 11.0; // 启发式信息，偏向于选择更大的值
            probs[i] = Math.pow(pheromone, ALPHA) * Math.pow(heuristic, BETA);
            sum += probs[i];
        }

        if (sum == 0) { // 避免因概率总和为零而卡死
            return random.nextInt(11) / 10.0;
        }

        double r = random.nextDouble() * sum;
        for (int i = 0; i < 11; i++) {
            r -= probs[i];
            if (r <= 0) return i / 10.0;
        }
        return 1.0; // 作为浮点数误差的保障
    }

    private double evaluate(double[] weights) {
        return DataTest.score(weights, testData, baseDM);
    }
}