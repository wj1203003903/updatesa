import java.util.Arrays;
import java.util.Random;

public class PSO {
    // --- 参数 ---
    private static final int POP_SIZE = 20;         // 粒子数量
    private static final int GENERATIONS = 130  ;      // 迭代次数
    private static final double INERTIA = 0.7;      // 惯性权重
    private static final double COGNITIVE = 1;    // 个体学习因子
    private static final double SOCIAL = 2;       // 群体学习因子
    private static final int DIMENSIONS = 5;        // 权重维度 (已更正为5)

    // --- 成员变量 ---
    private DataItem[] testData;
    private DataManager baseDM;
    private Random random;

    public PSO(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random(Main.randomseal);
    }

    public double run() {
        // --- 初始化 ---
        double[][] position = new double[POP_SIZE][DIMENSIONS];
        double[][] velocity = new double[POP_SIZE][DIMENSIONS];
        double[][] pBestPosition = new double[POP_SIZE][DIMENSIONS];
        double[] pBestScore = new double[POP_SIZE];

        double[] best = new double[DIMENSIONS];
        double bestScore = -Double.MAX_VALUE;

        // 初始化粒子群的位置和速度
        for (int i = 0; i < POP_SIZE; i++) {
            for (int j = 0; j < DIMENSIONS; j++) {
                position[i][j] = random.nextDouble();
            }}
        for (int i = 0; i < POP_SIZE; i++) {
            for (int j = 0; j < DIMENSIONS; j++) {
                velocity[i][j] = random.nextDouble() * 0.1 - 0.05; // 初始速度在 [-0.05, 0.05]
            }}
        for (int i = 0; i < POP_SIZE; i++) {
            pBestPosition[i] = position[i].clone();
            pBestScore[i] = evaluate(position[i]);

            if (pBestScore[i] > bestScore) {
                bestScore = pBestScore[i];
                best = pBestPosition[i].clone();
            }
        }
        // --- 主循环，增加监控 ---
        for (int gen = 0; gen < GENERATIONS; gen++) {
            double lastGenBestScore = bestScore; // 记录本代开始前的最优分数

            // 更新每个粒子的速度和位置
            for (int i = 0; i < POP_SIZE; i++) {
                for (int j = 0; j < DIMENSIONS; j++) {
                    double r1 = random.nextDouble();
                    double r2 = random.nextDouble();
                    // 更新速度
                    velocity[i][j] = INERTIA * velocity[i][j]
                            + COGNITIVE * r1 * (pBestPosition[i][j] - position[i][j])
                            + SOCIAL * r2 * (best[j] - position[i][j]);

                    // 更新位置
                    position[i][j] += velocity[i][j];

                    // 限制位置在 [0,1] 范围内
                    if (position[i][j] < 0) position[i][j] = 0;
                    if (position[i][j] > 1) position[i][j] = 1;
                }

                // 评估新位置并更新最优解
                double score = evaluate(position[i]);
                if (score > pBestScore[i]) {
                    pBestScore[i] = score;
                    pBestPosition[i] = position[i].clone();
                }
                if (score > bestScore) {
                    bestScore = score;
                    best = position[i].clone();
                }
            }

            double improvement = bestScore - Main.baselineScore;
            System.out.printf("Gen %2d: Improvement = %.4f\n",
                    gen + 1, improvement>0?improvement:0);
        }

        System.out.println("\n=== PSO Finished ===");
        double[] finalNormalizedBest = best.clone();
        baseDM.normalizeL2(finalNormalizedBest);
        System.out.printf("Final Best Weights (Normalized) = %s\n", Arrays.toString(finalNormalizedBest));

        evaluate(best); // 使用原始最优解评估
        baseDM.printStats();
        return bestScore;
    }

    // 评估函数
    private double evaluate(double[] w) {
        return DataTest.score(w, testData, baseDM);
    }
}