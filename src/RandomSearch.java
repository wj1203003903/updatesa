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

    public double run(PrintStream out) {
        double bestScore = -Double.MAX_VALUE;
        double[] bestWeights = new double[DIMENSIONS];
        out.println("Running " + ATTEMPTS + " random attempts to find a stable baseline...");

        for (int i = 0; i < ATTEMPTS; i++) {
            double[] randomWeights = new double[DIMENSIONS];
            for (int j = 0; j < DIMENSIONS; j++) {
                randomWeights[j] = random.nextDouble();
            }

            double currentScore = DataTest.score(randomWeights, testData, baseDM);

            if (currentScore > bestScore) {
                bestScore = currentScore;
                bestWeights = randomWeights.clone();

                // --- 【核心修复】当找到新的最高分时，立即保存当时的统计快照 ---
                baseDM.saveBestStats();
            }
        }

        out.println("\n=== Single Random Attempt Finished (Baseline) ===");

        double[] finalNormalizedBest = bestWeights.clone();
        baseDM.normalizeL2(finalNormalizedBest);
        out.printf("Final Best Weights (Normalized) = %s\n", Arrays.toString(finalNormalizedBest));

        // 现在 printStats 会正确地打印出 ATTEMPTS 次中最好那一次的结果
        baseDM.printStats(out);

        return bestScore;
    }
}