import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

public class PSO {
    private static final int POP_SIZE = 20;
    private static final int GENERATIONS = 100;
    private static final double INERTIA = 0.75;
    private static final double COGNITIVE = 1.0;
    private static final double SOCIAL = 1.0;
    private static final int DIMENSIONS = 5;

    private DataItem[] testData;
    private DataManager baseDM;
    private Random random;

    public PSO(DataItem[] testData, DataManager baseDM, long seed) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random(seed);
    }

    private double evaluate(double[] weights) {
        return DataTest.score(weights, testData, baseDM);
    }

    public double run(PrintStream out, double baselineScore) {
        double[][] position = new double[POP_SIZE][DIMENSIONS];
        double[][] velocity = new double[POP_SIZE][DIMENSIONS];
        double[][] pBestPosition = new double[POP_SIZE][DIMENSIONS];
        double[] pBestScore = new double[POP_SIZE];
        double[] gBestPosition = new double[DIMENSIONS];
        double gBestScore = -Double.MAX_VALUE;

        for (int i = 0; i < POP_SIZE; i++) {
            for (int j = 0; j < DIMENSIONS; j++) {
                position[i][j] = random.nextDouble();
                velocity[i][j] = random.nextDouble() * 0.1 - 0.05;
            }
            pBestPosition[i] = position[i].clone();
            pBestScore[i] = evaluate(position[i]);
            if (pBestScore[i] > gBestScore) {
                gBestScore = pBestScore[i];
                gBestPosition = pBestPosition[i].clone();
                baseDM.saveBestStats();
            }
        }

        for (int gen = 0; gen < GENERATIONS; gen++) {
            for (int i = 0; i < POP_SIZE; i++) {
                for (int j = 0; j < DIMENSIONS; j++) {
                    double r1 = random.nextDouble();
                    double r2 = random.nextDouble();
                    velocity[i][j] = INERTIA * velocity[i][j]
                            + COGNITIVE * r1 * (pBestPosition[i][j] - position[i][j])
                            + SOCIAL * r2 * (gBestPosition[j] - position[i][j]);
                    position[i][j] += velocity[i][j];
                    if (position[i][j] < 0) position[i][j] = 0;
                    if (position[i][j] > 1) position[i][j] = 1;
                }

                double score = evaluate(position[i]);

                if (score > pBestScore[i]) {
                    pBestScore[i] = score;
                    pBestPosition[i] = position[i].clone();
                }
                if (score > gBestScore) {
                    gBestScore = score;
                    gBestPosition = position[i].clone();
                    baseDM.saveBestStats();
                }
            }
            double improvement = gBestScore - baselineScore;
            out.printf("Gen %2d: Improvement = %.4f\n", gen + 1, Math.max(0, improvement));
        }

        out.println("\n=== PSO Finished ===");
        double[] finalNormalizedBest = gBestPosition.clone();
        baseDM.normalizeL2(finalNormalizedBest);
        out.printf("Final Best Weights (Normalized) = %s\n", Arrays.toString(finalNormalizedBest));
        baseDM.printStats(out);
        return gBestScore;
    }
}