import java.util.*;

public class SA {
    private static final int DIMENSIONS = 4;       // 权重维度
    private static final double INIT_TEMP = 100.0; // 初始温度
    private static final double MIN_TEMP = 1e-3;   // 最小温度
    private static final double COOLING_RATE = 0.6; // 降温系数
    private static final int ITERATIONS_PER_TEMP = 50;

    private static DataItem[] testData;
    private static DataManager baseDM;
    private static Random random;

    public SA(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();
    }

    public void run() {
        double[] current = randomWeights(); // 当前解
        double currentScore = evaluate(current);

        double[] best = current.clone();
        double bestScore = currentScore;

        double temperature = INIT_TEMP;

        while (temperature > MIN_TEMP) {
            for (int i = 0; i < ITERATIONS_PER_TEMP; i++) {
                double[] neighbor = generateNeighbor(current);
                double neighborScore = evaluate(neighbor);

                if (acceptanceProbability(currentScore, neighborScore, temperature) > random.nextDouble()) {
                    current = neighbor;
                    currentScore = neighborScore;

                    if (currentScore > bestScore) {
                        best = current.clone();
                        bestScore = currentScore;
                     }
                }
            }

            temperature *= COOLING_RATE;
        }
        baseDM.normalizeL2(best);
        System.out.println("=== SA Finished ===");
        System.out.printf("Best Score = %.4f\nBest Weights = %s\n", bestScore*100, Arrays.toString(best));
        baseDM.printStats();
    }

    // 生成初始解（每维随机 0~1）
    private double[] randomWeights() {
        double[] w = new double[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            w[i] = random.nextDouble();
        }
        return w;
    }

    // 生成邻居解
    private double[] generateNeighbor(double[] current) {
        double[] neighbor = current.clone();
        int idx = random.nextInt(DIMENSIONS);
        neighbor[idx] += random.nextGaussian() * 0.1; // 小范围扰动

        // 限制在 [0,1] 范围内
        if (neighbor[idx] < 0) neighbor[idx] = 0;
        if (neighbor[idx] > 1) neighbor[idx] = 1;

        return neighbor;
    }

    // 接受概率函数
    private double acceptanceProbability(double currentScore, double neighborScore, double temperature) {
        if (neighborScore > currentScore) {
            return 1.0;
        }
        return Math.exp((neighborScore - currentScore) / temperature);
    }

    // 评估函数
    private double evaluate(double[] weights) {
        return DataTest.score(weights, testData, baseDM);
    }
}
