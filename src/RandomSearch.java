import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

public class RandomSearch {
    private static final int DIMENSIONS = 5;
    private static final int ATTEMPTS = 10;
    private final DataItem[] testData;
    private final DataManager baseDM;
    private final Random random;

    public RandomSearch(DataItem[] testData, DataManager baseDM, long seed) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random(seed);
    }

    private double evaluate(double[] weights) {
        baseDM.resetCurrentRunStats();
        return DataTest.score(weights, testData, baseDM);
    }

    public double run(PrintStream out) {
        double bestScore = -Double.MAX_VALUE;
        double[] bestWeights = new double[DIMENSIONS];
        out.println("Running " + ATTEMPTS + " random attempts to find a stable baseline...");

        for (int i = 0; i < ATTEMPTS; i++) {
            double[] randomWeights = new double[DIMENSIONS];
            for (int j = 0; j < DIMENSIONS; j++) {
                randomWeights[j] = random.nextDouble();
            }
            double currentScore = evaluate(randomWeights);
            if (currentScore > bestScore) {
                bestScore = currentScore;
                bestWeights = randomWeights.clone();
                baseDM.saveBestStats();
            }
        }

        out.println("\n=== Single Random Attempt Finished (Baseline) ===");
        double[] finalNormalizedBest = bestWeights.clone();
        baseDM.normalizeL2(finalNormalizedBest);
        out.printf("Final Best Weights (Normalized) = %s\n", Arrays.toString(finalNormalizedBest));
        baseDM.printStats(out);
        return bestScore;
    }
}