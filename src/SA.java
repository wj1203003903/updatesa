import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

public class SA {
    private static final int DIMENSIONS = 5;
    private static final double INIT_TEMP = 500;
    private static final double COOLING_RATE = 0.9;
    private static final int ITERATIONS_PER_TEMP = 100;
    private static final double MIN_TEMP = 5e-3;

    private DataItem[] testData;
    private DataManager baseDM;
    private Random random;

    // 公共字段供 Main 获取结果
    public double[] bestSolution;
    public double bestScore;

    public SA(DataItem[] testData, DataManager baseDM, long seed) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random(seed);
        this.bestSolution = new double[DIMENSIONS];
        this.bestScore = -Double.MAX_VALUE;
    }

    // 获取最优权重
    public double[] getBestWeights() {
        return this.bestSolution;
    }

    private double evaluate(double[] weights, double currentBestScore) {
        baseDM.resetCurrentRunStats();
        double score = DataTest.score(weights, testData, baseDM);
        if (score > currentBestScore) {
            baseDM.saveBestStats();
        }
        return score;
    }

    /**
     * 统一优化入口 (支持热启动)
     */
    public double[] optimize(double[] initialWeights) {
        // 1. 初始化
        double[] currentSolution;
        if (initialWeights != null) {
            currentSolution = initialWeights.clone(); // Warm Start
        } else {
            currentSolution = randomWeights(); // Cold Start
        }

        // 2. 初始评估
        this.bestScore = evaluate(currentSolution, -Double.MAX_VALUE);
        this.bestSolution = currentSolution.clone();
        double currentScore = this.bestScore;

        // 3. 开始退火
        double temperature = INIT_TEMP;

        while (temperature > MIN_TEMP) {
            for (int i = 0; i < ITERATIONS_PER_TEMP; i++) {
                double[] neighborSolution = generateNeighbor(currentSolution);
                double neighborScore = evaluate(neighborSolution, this.bestScore);

                // 更新全局最优
                if (neighborScore > this.bestScore) {
                    this.bestScore = neighborScore;
                    this.bestSolution = neighborSolution.clone();
                }

                // Metropolis 准则
                if (acceptanceProbability(currentScore, neighborScore, temperature) > random.nextDouble()) {
                    currentSolution = neighborSolution;
                    currentScore = neighborScore;
                }
            }
            temperature *= COOLING_RATE;
        }

        return this.bestSolution;
    }

    // 兼容旧代码调用的方法
    public double run(PrintStream out, double baselineScore) {
        optimize(null);
        if (baselineScore != -9999) {
            out.println("SA Finished. Best Score: " + bestScore);
        }
        return bestScore;
    }

    private double[] randomWeights() {
        double[] w = new double[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) w[i] = random.nextDouble();
        return w;
    }

    private double[] generateNeighbor(double[] current) {
        double[] neighbor = current.clone();
        int idx = random.nextInt(DIMENSIONS);
        neighbor[idx] += (random.nextDouble() - 0.5) * 0.3;
        if (neighbor[idx] < 0) neighbor[idx] = 0;
        if (neighbor[idx] > 1) neighbor[idx] = 1;
        return neighbor;
    }

    private double acceptanceProbability(double currentScore, double neighborScore, double temperature) {
        if (neighborScore > currentScore) return 1.0;
        return Math.exp((neighborScore - currentScore) / temperature);
    }
}