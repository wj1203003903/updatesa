import java.util.Arrays;
import java.util.Random;

public class SA {
    // --- 【原始版推荐参数】- 旨在减慢收敛 ---
    private static final int DIMENSIONS = 5;

    // 使用一个非常高的初始温度来强行增加早期探索
    private static final double INIT_TEMP = 1000;

    // 使用一个非常慢的冷却速率，让算法有更多时间探索
    private static final double COOLING_RATE = 0.9;

    // 在每个温度下进行更多的迭代
    private static final int ITERATIONS_PER_TEMP = 100;

    private static final double MIN_TEMP = 2e-3;

    // --- 成员变量 ---
    private DataItem[] testData;
    private DataManager baseDM;
    private Random random;

    public SA(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random(Main.randomseal);
    }

    public double run() {
        double[] currentSolution = randomWeights();
        double currentScore = evaluate(currentSolution);
        double[] bestSolution = currentSolution.clone();
        double bestScore = currentScore;
        double temperature = INIT_TEMP;
        int generation = 0;

        while (temperature > MIN_TEMP) {
            generation++;
            for (int i = 0; i < ITERATIONS_PER_TEMP; i++) {
                // 调用最原始的、固定的邻域函数
                double[] neighborSolution = generateNeighbor(currentSolution);
                double neighborScore = evaluate(neighborSolution);

                if (acceptanceProbability(currentScore, neighborScore, temperature) > random.nextDouble()) {
                    currentSolution = neighborSolution;
                    currentScore = neighborScore;
                    if (currentScore > bestScore) {
                        bestSolution = currentSolution.clone();
                        bestScore = currentScore;
                    }
                }
            }

            // 打印信息
            double improvement = bestScore - Main.baselineScore;
            System.out.printf("Gen %2d (Temp %.2e): Improvement = %.4f\n",
                    generation, temperature, improvement > 0 ? improvement : 0);

            temperature *= COOLING_RATE;
        }

        System.out.println("\n=== SA (Standard Version) Finished ===");
        double[] finalNormalizedBest = bestSolution.clone();
        baseDM.normalizeL2(finalNormalizedBest);
        System.out.printf("Final Best Weights (Normalized) = %s\n", Arrays.toString(finalNormalizedBest));

        evaluate(bestSolution);
        baseDM.printStats();
        return bestScore;
    }

    // --- 辅助方法 ---

    private double[] randomWeights() {
        double[] w = new double[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            w[i] = random.nextDouble();
        }
        return w;
    }

    /**
     * 【最原始的版本】
     * 邻域生成函数：单点、固定小步长扰动
     */
    private double[] generateNeighbor(double[] current) {
        double[] neighbor = current.clone();
        int idx = random.nextInt(DIMENSIONS);

        // 固定的、小范围的扰动
        neighbor[idx] += (random.nextDouble() - 0.5) * 0.3;

        // 边界检查
        if (neighbor[idx] < 0) neighbor[idx] = 0;
        if (neighbor[idx] > 1) neighbor[idx] = 1;

        return neighbor;
    }

    private double acceptanceProbability(double currentScore, double neighborScore, double temperature) {
        if (neighborScore > currentScore) {
            return 1.0;
        }
        return Math.exp((neighborScore - currentScore) / temperature);
    }

    private double evaluate(double[] weights) {
        return DataTest.score(weights, testData, baseDM);
    }
}