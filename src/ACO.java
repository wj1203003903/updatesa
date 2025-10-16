import java.util.Arrays;
import java.util.Random;

public class ACO {
    // --- 参数 ---
    private static final int ANT_COUNT = 20;
    private static final int GENERATIONS = 100;
    private static final int DIMENSIONS = 5;

    // 【核心修改】将BETA设为0，并可以适当增强ALPHA
    private static final double ALPHA = 0.3;           // 信息素影响因子 (可以设为1.0)

    private static final double EVAPORATION = 0.7;
    private static final double Q = 100.0;

    // --- 成员变量 (无变化) ---
    private DataItem[] testData;
    private DataManager baseDM;
    private Random random;
    private double[][] pheromones;

    public ACO(DataItem[] testData, DataManager baseDM) {
        // ... (构造函数无变化) ...
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();
        this.pheromones = new double[DIMENSIONS][11];
        for (int i = 0; i < DIMENSIONS; i++) {
            Arrays.fill(pheromones[i], random.nextDouble());
        }
    }

    public double run() {
        // ... (run方法无变化) ...
        double bestScore = -Double.MAX_VALUE;
        double[] best = new double[DIMENSIONS];
        System.out.println("ACO Initializing (Heuristics Disabled)..."); // 修改了打印信息

        for (int gen = 0; gen < GENERATIONS; gen++) {
            // ... (循环内部无变化) ...
            double[][] ants = new double[ANT_COUNT][DIMENSIONS];
            double[] scores = new double[ANT_COUNT];

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

            // ... (信息素更新无变化) ...
            for (int i = 0; i < DIMENSIONS; i++) {
                for (int j = 0; j < 11; j++) {
                    pheromones[i][j] *= (1 - EVAPORATION);
                }
            }
            for (int k = 0; k < ANT_COUNT; k++) {
                for (int d = 0; d < DIMENSIONS; d++) {
                    int idx = (int) Math.round(ants[k][d] * 10);
                    pheromones[d][idx] += Q * (scores[k] / Math.max(bestScore, 1e-6));
                }
            }

            double improvement = bestScore - Main.baselineScore;
            System.out.printf("Gen %2d: Improvement = %.4f\n",
                    gen + 1, improvement > 0 ? improvement : 0);
        }

        // ... (结束打印无变化) ...
        System.out.println("\n=== ACO Finished ===");
        double[] finalNormalizedBest = best.clone();
        baseDM.normalizeL2(finalNormalizedBest);
        System.out.printf("Final Best Weights (Normalized) = %s\n", Arrays.toString(finalNormalizedBest));
        evaluate(best);
        baseDM.printStats();
        return bestScore;
    }

    // 轮盘赌选择
    // 【【【 核心修改在此 】】】
    private double selectValue(int dimension) {
        double[] probs = new double[11];
        double sum = 0;

        for (int i = 0; i < 11; i++) {
            double pheromone = pheromones[dimension][i];

            // --- 修改开始 ---
            // 由于BETA参数已设为0，Math.pow(heuristic, BETA) 的结果永远是 1。
            // 我们可以直接移除启发式部分，让逻辑更清晰。
            probs[i] = Math.pow(pheromone, ALPHA);
            // --- 修改结束 ---

            sum += probs[i];
        }

        // 增加对sum为0或NaN的健壮性检查
        if (sum == 0 || Double.isNaN(sum)) {
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