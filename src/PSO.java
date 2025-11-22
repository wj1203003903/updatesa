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

    public double[] bestSolution;
    public double bestScore;

    public PSO(DataItem[] testData, DataManager baseDM, long seed) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random(seed);
        this.bestSolution = new double[DIMENSIONS];
        this.bestScore = -Double.MAX_VALUE;
    }

    private double evaluate(double[] weights) {
        return DataTest.score(weights, testData, baseDM);
    }

    public double[] optimize(double[] initialWeights) {
        double[][] position = new double[POP_SIZE][DIMENSIONS];
        double[][] velocity = new double[POP_SIZE][DIMENSIONS];
        double[][] pBestPosition = new double[POP_SIZE][DIMENSIONS];
        double[] pBestScore = new double[POP_SIZE];

        this.bestScore = -Double.MAX_VALUE;

        // 1. 初始化种群
        for (int i = 0; i < POP_SIZE; i++) {
            // Warm Start: 第一个粒子继承旧权重
            if (i == 0 && initialWeights != null) {
                position[i] = initialWeights.clone();
            } else {
                for (int j = 0; j < DIMENSIONS; j++) {
                    position[i][j] = random.nextDouble();
                }
            }

            // 初始化速度
            for (int j = 0; j < DIMENSIONS; j++) {
                velocity[i][j] = random.nextDouble() * 0.1 - 0.05;
            }

            pBestPosition[i] = position[i].clone();
            pBestScore[i] = evaluate(position[i]);

            if (pBestScore[i] > this.bestScore) {
                this.bestScore = pBestScore[i];
                this.bestSolution = pBestPosition[i].clone();
                baseDM.saveBestStats();
            }
        }

        // 2. 迭代更新
        for (int gen = 0; gen < GENERATIONS; gen++) {
            for (int i = 0; i < POP_SIZE; i++) {
                for (int j = 0; j < DIMENSIONS; j++) {
                    double r1 = random.nextDouble();
                    double r2 = random.nextDouble();
                    velocity[i][j] = INERTIA * velocity[i][j]
                            + COGNITIVE * r1 * (pBestPosition[i][j] - position[i][j])
                            + SOCIAL * r2 * (this.bestSolution[j] - position[i][j]);

                    position[i][j] += velocity[i][j];
                    // 边界限制
                    if (position[i][j] < 0) position[i][j] = 0;
                    if (position[i][j] > 1) position[i][j] = 1;
                }

                double score = evaluate(position[i]);

                if (score > pBestScore[i]) {
                    pBestScore[i] = score;
                    pBestPosition[i] = position[i].clone();
                }
                if (score > this.bestScore) {
                    this.bestScore = score;
                    this.bestSolution = position[i].clone();
                    baseDM.saveBestStats();
                }
            }
        }
        return this.bestSolution;
    }

    public double run(PrintStream out, double baselineScore) {
        optimize(null);
        if (baselineScore != -9999) out.println("PSO Finished.");
        return bestScore;
    }
}