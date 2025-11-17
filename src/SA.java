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

    public SA(DataItem[] testData, DataManager baseDM, long seed) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random(seed);
    }

    private double evaluate(double[] weights, double currentBestScore) {
        baseDM.resetCurrentRunStats();
        double score = DataTest.score(weights, testData, baseDM);
        if (score > currentBestScore) {
            baseDM.saveBestStats();
        }
        return score;
    }

    public double run(PrintStream out, double baselineScore) {
        double[] currentSolution = randomWeights();
        double bestScore = evaluate(currentSolution, -Double.MAX_VALUE);
        double[] bestSolution = currentSolution.clone();
        double currentScore = bestScore;

        double temperature = INIT_TEMP;
        int generation = 0;

        while (temperature > MIN_TEMP) {
            generation++;
            for (int i = 0; i < ITERATIONS_PER_TEMP; i++) {
                double[] neighborSolution = generateNeighbor(currentSolution);
                double neighborScore = evaluate(neighborSolution, bestScore);

                if (neighborScore > bestScore) {
                    bestScore = neighborScore;
                    bestSolution = neighborSolution.clone();
                }

                if (acceptanceProbability(currentScore, neighborScore, temperature) > random.nextDouble()) {
                    currentSolution = neighborSolution;
                    currentScore = neighborScore;
                }
            }
            double improvement = bestScore - baselineScore;
            out.printf("Gen %2d (Temp %.2e): Improvement = %.4f\n",
                    generation, temperature, Math.max(0, improvement));
            temperature *= COOLING_RATE;
        }

        out.println("\n=== SA (Standard Version) Finished ===");
        double[] finalNormalizedBest = bestSolution.clone();
        baseDM.normalizeL2(finalNormalizedBest);
        out.printf("Final Best Weights (Normalized) = %s\n", Arrays.toString(finalNormalizedBest));
        baseDM.printStats(out);
        return bestScore;
    }

    private double[] randomWeights() {
        double[] w = new double[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            w[i] = random.nextDouble();
        }
        return w;
    }

    private double[] generateNeighbor(double[] current) {
        double[] neighbor = current.clone();
        int idx = random.nextInt(DIMENSIONS);
        neighbor[idx] += (random.nextDouble() - 0.5) * 0.3;
        if (neighbor[idx] < 0) {
            neighbor[idx] = 0;
        }
        if (neighbor[idx] > 1) {
            neighbor[idx] = 1;
        }
        return neighbor;
    }

    private double acceptanceProbability(double currentScore, double neighborScore, double temperature) {
        if (neighborScore > currentScore) {
            return 1.0;
        }
        return Math.exp((neighborScore - currentScore) / temperature);
    }
}