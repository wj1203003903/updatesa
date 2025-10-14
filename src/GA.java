import java.util.Arrays;
import java.util.Random;

public class GA {
    // --- 参数 ---
    private static final int POP_SIZE = 20;            // 种群大小
    private static final int GENERATIONS = 100;         // 迭代次数
    private static final double MUTATION_RATE = 0.2;    // 变异率
    private static final int TOURNAMENT_SIZE = 3;      // 锦标赛选择的规模
    private static final int DIMENSIONS = 5;           // 权重维度 (已更正为5)

    // --- 成员变量 ---
    private DataItem[] testData;
    private DataManager baseDM;
    private Random random;

    public GA(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();
    }

    public double run() {
        double[][] population = new double[POP_SIZE][DIMENSIONS];

        // 初始化种群
        for (int i = 0; i < POP_SIZE; i++) {
            for (int j = 0; j < DIMENSIONS; j++) {
                population[i][j] = random.nextDouble();
            }
        }

        // 初始化最优解
        double[] best = population[0].clone();
        double bestScore = evaluate(best);
        // --- 主循环，增加监控 ---
        for (int gen = 0; gen < GENERATIONS; gen++) {
            double lastGenBestScore = bestScore; // 记录本代开始前的最优分数

            double[][] newPopulation = new double[POP_SIZE][DIMENSIONS];

            for (int i = 0; i < POP_SIZE; i++) {
                // 选择
                double[] parent1 = tournamentSelection(population);
                double[] parent2 = tournamentSelection(population);

                // 交叉
                double[] child = crossover(parent1, parent2);

                // 变异
                mutate(child);
                newPopulation[i] = child;

                // 评估并更新最优解 (仅在当前代内部更新)
                double score = evaluate(child);
                if (score > bestScore) {
                    bestScore = score;
                    best = child.clone();
                }
            }

            // 用新一代替换旧一代
            population = newPopulation;

            double improvement = bestScore - Main.baselineScore;
            System.out.printf("Gen %2d: Improvement = %.4f\n",
                    gen + 1, improvement>0?improvement:0);
        }

        System.out.println("\n=== GA Finished ===");
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

    // 锦标赛选择
    private double[] tournamentSelection(double[][] population) {
        double[] best = null;
        double bestScore = -Double.MAX_VALUE;

        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            int index = random.nextInt(POP_SIZE);
            double[] individual = population[index];
            double score = evaluate(individual);
            if (score > bestScore) {
                bestScore = score;
                best = individual;
            }
        }
        return best.clone();
    }

    // 单点交叉
    private double[] crossover(double[] p1, double[] p2) {
        double[] child = new double[DIMENSIONS];
        int crossoverPoint = random.nextInt(DIMENSIONS);

        for (int i = 0; i < DIMENSIONS; i++) {
            if (i < crossoverPoint) {
                child[i] = p1[i];
            } else {
                child[i] = p2[i];
            }
        }
        return child;
    }

    // 高斯变异
    private void mutate(double[] individual) {
        for (int i = 0; i < DIMENSIONS; i++) {
            if (random.nextDouble() < MUTATION_RATE) {
                individual[i] += random.nextGaussian() * 0.2;  // 增加高斯噪声
                // 确保权重值在 [0, 1] 范围内
                if (individual[i] < 0) individual[i] = 0;
                if (individual[i] > 1) individual[i] = 1;
            }
        }
    }
}