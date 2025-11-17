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

    public ACO(DataItem[] testData, DataManager baseDM, long seed) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random(seed);
        this.pheromones = new double[DIMENSIONS][GRANULARITY];
        for (int i = 0; i < DIMENSIONS; i++) {
            Arrays.fill(pheromones[i], 1.0);
        }
    }

    private double evaluate(double[] weights) {
        baseDM.resetCurrentRunStats();
        return DataTest.score(weights, testData, baseDM);
    }

    public double run(PrintStream out, double baselineScore) {
        double bestScore = -Double.MAX_VALUE;
        double[] bestSolution = new double[DIMENSIONS];
        out.println("ACO Initializing (Granularity: " + GRANULARITY + ")...");

        // 初始化最佳分数
        double initialScore = evaluate(randomWeights());
        bestScore = initialScore;
        baseDM.saveBestStats();

        for (int gen = 0; gen < GENERATIONS; gen++) {
            double[][] ants = new double[ANT_COUNT][DIMENSIONS];
            double[] scores = new double[ANT_COUNT];

            for (int k = 0; k < ANT_COUNT; k++) {
                for (int d = 0; d < DIMENSIONS; d++) {
                    ants[k][d] = selectValue(d);
                }
                scores[k] = evaluate(ants[k]);
                if (scores[k] > bestScore) {
                    bestScore = scores[k];
                    bestSolution = ants[k].clone();
                    baseDM.saveBestStats();
                }
            }

            for (int i = 0; i < DIMENSIONS; i++) {
                for (int j = 0; j < GRANULARITY; j++) {
                    pheromones[i][j] *= EVAPORATION;
                }
            }

            for (int k = 0; k < ANT_COUNT; k++) {
                for (int d = 0; d < DIMENSIONS; d++) {
                    int idx = (int) Math.round(ants[k][d] * (GRANULARITY - 1));
                    pheromones[d][idx] += Q * (scores[k] / Math.max(bestScore, 1e-6));
                }
            }
            double improvement = bestScore - baselineScore;
            out.printf("Gen %2d: Improvement = %.4f\n", gen + 1, Math.max(0, improvement));
        }

        out.println("\n=== ACO Finished ===");
        double[] finalNormalizedBest = bestSolution.clone();
        baseDM.normalizeL2(finalNormalizedBest);
        out.printf("Final Best Weights (Normalized) = %s\n", Arrays.toString(finalNormalizedBest));
        baseDM.printStats(out);
        return bestScore;
    }

    private double[] randomWeights() { double[] w = new double[DIMENSIONS]; for (int i = 0; i < DIMENSIONS; i++) w[i] = random.nextDouble(); return w; }
    private double selectValue(int dimension) { double[] probs = new double[GRANULARITY]; double sum = 0; for (int i = 0; i < GRANULARITY; i++) { probs[i] = Math.pow(pheromones[dimension][i], ALPHA); sum += probs[i]; } if (sum == 0 || Double.isNaN(sum)) { return (double) random.nextInt(GRANULARITY) / (GRANULARITY - 1); } double r = random.nextDouble() * sum; for (int i = 0; i < GRANULARITY; i++) { r -= probs[i]; if (r <= 0) { return (double) i / (GRANULARITY - 1); } } return 1.0; }
}