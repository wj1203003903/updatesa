import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

public class ACO {
    private static final int ANT_COUNT = 20;
    private static final int GENERATIONS = 100;
    private static final int DIMENSIONS = 5;
    private static final double ALPHA = 0.4;
    private static final double EVAPORATION = 0.9;
    private static final double Q = 50.0;
    private static final int GRANULARITY = 41;

    private DataItem[] testData;
    private DataManager baseDM;
    private Random random;
    private double[][] pheromones;

    public double[] bestSolution;
    public double bestScore;

    public ACO(DataItem[] testData, DataManager baseDM, long seed) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random(seed);
        this.pheromones = new double[DIMENSIONS][GRANULARITY];
        for (int i = 0; i < DIMENSIONS; i++) {
            Arrays.fill(pheromones[i], 1.0);
        }
        this.bestSolution = new double[DIMENSIONS];
        this.bestScore = -Double.MAX_VALUE;
    }

    private double evaluate(double[] weights) {
        return DataTest.score(weights, testData, baseDM);
    }

    public double[] optimize(double[] initialWeights) {
        // Warm Start: 如果有旧权重，增强对应路径的信息素
        if (initialWeights != null) {
            this.bestSolution = initialWeights.clone();
            this.bestScore = evaluate(initialWeights);
            baseDM.saveBestStats();

            // 在旧路径上撒大量信息素，引导蚂蚁探索附近
            for (int d = 0; d < DIMENSIONS; d++) {
                int idx = (int) Math.round(initialWeights[d] * (GRANULARITY - 1));
                // 增加额外信息素
                pheromones[d][idx] += 10.0;
            }
        } else {
            // 冷启动：随机评估一次
            double[] rand = randomWeights();
            this.bestScore = evaluate(rand);
            this.bestSolution = rand.clone();
        }

        for (int gen = 0; gen < GENERATIONS; gen++) {
            double[][] ants = new double[ANT_COUNT][DIMENSIONS];
            double[] scores = new double[ANT_COUNT];

            for (int k = 0; k < ANT_COUNT; k++) {
                for (int d = 0; d < DIMENSIONS; d++) {
                    ants[k][d] = selectValue(d);
                }
                scores[k] = evaluate(ants[k]);
                if (scores[k] > this.bestScore) {
                    this.bestScore = scores[k];
                    this.bestSolution = ants[k].clone();
                    baseDM.saveBestStats();
                }
            }

            // 信息素挥发
            for (int i = 0; i < DIMENSIONS; i++) {
                for (int j = 0; j < GRANULARITY; j++) {
                    pheromones[i][j] *= EVAPORATION;
                }
            }

            // 信息素更新
            for (int k = 0; k < ANT_COUNT; k++) {
                for (int d = 0; d < DIMENSIONS; d++) {
                    int idx = (int) Math.round(ants[k][d] * (GRANULARITY - 1));
                    double safeBest = Math.max(Math.abs(this.bestScore), 1e-6);
                    pheromones[d][idx] += Q * (scores[k] / safeBest);
                }
            }
        }
        return this.bestSolution;
    }

    public double run(PrintStream out, double baselineScore) {
        optimize(null);
        if (baselineScore != -9999) out.println("ACO Finished.");
        return bestScore;
    }

    private double[] randomWeights() {
        double[] w = new double[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) w[i] = random.nextDouble();
        return w;
    }

    private double selectValue(int dimension) {
        double[] probs = new double[GRANULARITY];
        double sum = 0;
        for (int i = 0; i < GRANULARITY; i++) {
            probs[i] = Math.pow(pheromones[dimension][i], ALPHA);
            sum += probs[i];
        }
        if (sum == 0 || Double.isNaN(sum)) return (double) random.nextInt(GRANULARITY) / (GRANULARITY - 1);

        double r = random.nextDouble() * sum;
        for (int i = 0; i < GRANULARITY; i++) {
            r -= probs[i];
            if (r <= 0) return (double) i / (GRANULARITY - 1);
        }
        return 1.0;
    }
}