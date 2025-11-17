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

    public double run(PrintStream out, double baselineScore) {
        double[] currentSolution = randomWeights();
        double currentScore = evaluate(currentSolution);
        double[] bestSolution = currentSolution.clone();
        double bestScore = currentScore;
        // 初始解也可能是最佳解，所以保存一次
        baseDM.saveBestStats();

        double temperature = INIT_TEMP;
        int generation = 0;

        while (temperature > MIN_TEMP) {
            generation++;
            for (int i = 0; i < ITERATIONS_PER_TEMP; i++) {
                double[] neighborSolution = generateNeighbor(currentSolution);
                double neighborScore = evaluate(neighborSolution);
                if (acceptanceProbability(currentScore, neighborScore, temperature) > random.nextDouble()) {
                    currentSolution = neighborSolution;
                    currentScore = neighborScore;
                    if (currentScore > bestScore) {
                        bestSolution = currentSolution.clone();
                        bestScore = currentScore;
                        // 发现新高分，立即保存此刻的统计快照
                        baseDM.saveBestStats();
                    }
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
        // 打印与最高分对应的统计数据
        baseDM.printStats(out);
        return bestScore;
    }
    private double evaluate(double[] weights) { return DataTest.score(weights, testData, baseDM); }
    private double[] randomWeights() { double[] w = new double[DIMENSIONS]; for (int i = 0; i < DIMENSIONS; i++) { w[i] = random.nextDouble(); } return w; }
    private double[] generateNeighbor(double[] current) { double[] n = current.clone(); int i = random.nextInt(DIMENSIONS); n[i] += (random.nextDouble()-0.5)*0.3; if(n[i]<0)n[i]=0; if(n[i]>1)n[i]=1; return n; }
    private double acceptanceProbability(double cs, double ns, double t) { if (ns > cs) return 1.0; return Math.exp((ns - cs) / t); }
}