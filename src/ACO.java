import java.util.*;

public class ACO {
    private static final int ANT_COUNT = 20;           // 蚂蚁数量
    private static final int GENERATIONS = 40;         // 迭代次数
    private static final int DIMENSIONS = 4;           // 权重维度
    private static final double ALPHA = 1.0;           // 信息素影响因子
    private static final double BETA = 2.0;            // 启发函数影响因子
    private static final double EVAPORATION = 0.5;     // 信息素挥发率
    private static final double Q = 100.0;             // 信息素增量因子

    private static DataItem[] testData;
    private static DataManager baseDM;
    private static Random random;

    private double[][] pheromones;  // 每个维度每个可能值的信息素

    public ACO(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();
        this.pheromones = new double[DIMENSIONS][11]; // 每个维度的权重值从 0.0 到 1.0
        for (int i = 0; i < DIMENSIONS; i++) {
            Arrays.fill(pheromones[i], 1.0); // 初始信息素为 1
        }
    }

    public void run() {
        double bestScore = -Double.MAX_VALUE;
        double[] bestWeights = new double[DIMENSIONS];

        for (int gen = 0; gen < GENERATIONS; gen++) {
            double[][] ants = new double[ANT_COUNT][DIMENSIONS];
            double[] scores = new double[ANT_COUNT];

            // 每只蚂蚁构造解
            for (int k = 0; k < ANT_COUNT; k++) {
                for (int d = 0; d < DIMENSIONS; d++) {
                    ants[k][d] = selectValue(d);
                }
                scores[k] = evaluate(ants[k]);
                if (scores[k] > bestScore) {
                    bestScore = scores[k];
                    bestWeights = ants[k].clone();

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
                    pheromones[d][idx] += Q * (scores[k] / bestScore);
                }
            }


        }
        baseDM.normalizeL2(bestWeights);
        System.out.println("=== ACO Finished ===");
        System.out.printf("Best Score = %.4f\nBest Weights = %s\n", bestScore*100, Arrays.toString(bestWeights));
        baseDM.printStats();
    }

    // 按概率从当前维度选择一个值
    private double selectValue(int dimension) {
        double[] probs = new double[11];
        double sum = 0;

        for (int i = 0; i < 11; i++) {
            double pheromone = pheromones[dimension][i];
            double heuristic = (i + 1) / 10.0;
            probs[i] = Math.pow(pheromone, ALPHA) * Math.pow(heuristic, BETA);
            sum += probs[i];
        }

        double r = random.nextDouble() * sum;
        for (int i = 0; i < 11; i++) {
            r -= probs[i];
            if (r <= 0) return i / 10.0;
        }
        return 1.0;
    }

    private double evaluate(double[] weights) {
        return DataTest.score(weights, testData, baseDM);
    }
}
