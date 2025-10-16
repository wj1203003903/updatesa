import java.util.Arrays;
import java.util.Random;

public class SA {
    // --- 您的参数 (保持不变) ---
    private static final int DIMENSIONS = 5;
    private static final double INIT_TEMP = 1000.0;
    private static final double MIN_TEMP = 2e-3;
    private static final double COOLING_RATE = 0.9;
    private static final int ITERATIONS_PER_TEMP = 100;

    // --- 成员变量 ---
    private DataItem[] testData;
    private DataManager baseDM;
    private Random random;

    public SA(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();
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
                // 调用升级版的邻域函数，并传入温度
                double[] neighborSolution = generateNeighbor(currentSolution, temperature);
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

        System.out.println("\n=== SA (Enhanced V2) Finished ===");
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
     * 【【【 核心升级在此 】】】
     * 邻域生成函数 V2：使用非线性衰减和最小步长保证
     * @param current 当前解
     * @param temperature 当前温度
     * @return 一个新的邻域解
     */
    private double[] generateNeighbor(double[] current, double temperature) {
        double[] neighbor = current.clone();

        // 1. 定义最大和最小扰动幅度
        double maxMagnitude = 0.5; // 最高温时的最大步长
        double minMagnitude = 0.01; // 【关键】保证在低温时仍有最小的探索能力

        // 2. 使用开方根进行非线性衰减
        // tempFactor 仍然是从 1 到 0，但 sqrt() 会让它在高温时下降快，低温时下降慢
        double tempFactor = Math.sqrt(temperature / INIT_TEMP);

        // 3. 线性插值计算当前步长
        // 公式与之前相同，但由于tempFactor的变化曲线不同，magnitude的行为也完全不同
        double magnitude = minMagnitude + (maxMagnitude - minMagnitude) * tempFactor;

        // 4. 动态决定扰动维度数量
        // 在快速冷却场景下，让多维度扰动的概率更高一些
        int dimsToChange = 1;
        if (random.nextDouble() < tempFactor) { // 温度越高，越有可能改变多个维度
            dimsToChange = 1 + random.nextInt(DIMENSIONS);
        }

        // 随机选择维度 (Fisher-Yates shuffle)
        int[] allIndices = {0, 1, 2, 3, 4};
        for (int i = 0; i < dimsToChange; i++) {
            int j = i + random.nextInt(DIMENSIONS - i);
            int temp = allIndices[i]; allIndices[i] = allIndices[j]; allIndices[j] = temp;
        }

        // 5. 应用扰动
        for (int i = 0; i < dimsToChange; i++) {
            int idx = allIndices[i];
            neighbor[idx] += (random.nextDouble() - 0.5) * 2 * magnitude;
            // 边界检查
            if (neighbor[idx] < 0) neighbor[idx] = 0;
            if (neighbor[idx] > 1) neighbor[idx] = 1;
        }

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