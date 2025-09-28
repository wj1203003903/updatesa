import java.util.*;

public class GA {
    private static final int POP_SIZE = 20;
    private static final int GENERATIONS = 40;
    private static final double MUTATION_RATE = 0.2;
    private static final int TOURNAMENT_SIZE = 3;
    private static DataItem[] testData;
    private static DataManager baseDM;
    private static Random random;

    // 构造函数：接受 testData 和 baseDM 作为参数
    public GA(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();
    }

    public double run() {
        double[][] population = new double[POP_SIZE][4];

        // 初始化种群
        for (int i = 0; i < POP_SIZE; i++) {
            for (int j = 0; j < 4; j++) {
                population[i][j] = random.nextDouble();
            }
        }

        double[] best = null;
        double bestScore = -Double.MAX_VALUE;

        for (int gen = 0; gen < GENERATIONS; gen++) {
            double[][] newPopulation = new double[POP_SIZE][4];

            for (int i = 0; i < POP_SIZE; i++) {
                double[] parent1 = tournamentSelection(population);
                double[] parent2 = tournamentSelection(population);

                double[] child = crossover(parent1, parent2);
                mutate(child);
                newPopulation[i] = child;

                double score = evaluate(child);
                if (score > bestScore) {
                    bestScore = score;
                    best = child.clone();
        
                }
             
            }

            // 更新种群
            population = newPopulation;

        }
        baseDM.normalizeL2(best);
        System.out.println("=== GA Finished ===");
        System.out.printf("Best Weights = %s\n", Arrays.toString(best));
        baseDM.printStats();
        return bestScore;
    }

    // 评估个体（即计算某一组权重下的分数）
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

    // 交叉操作：将两个父代个体的权重平均化生成子代
 // 改进的交叉操作：使用单点交叉
    private double[] crossover(double[] p1, double[] p2) {
        Random rand = new Random();
        double[] child = new double[4];
        int crossoverPoint = rand.nextInt(4);  // 随机选择交叉点

        for (int i = 0; i < 4; i++) {
            if (i < crossoverPoint) {
                child[i] = p1[i];
            } else {
                child[i] = p2[i];
            }
        }
        return child;
    }

    // 改进的变异操作：增加变异幅度并限制范围
    private void mutate(double[] individual) {
        Random rand = new Random();
        for (int i = 0; i < 4; i++) {
            if (rand.nextDouble() < MUTATION_RATE) {
                individual[i] += rand.nextGaussian() * 0.2;  // 增加变异幅度
                if (individual[i] < 0) individual[i] = 0;   // 权重不能为负
                if (individual[i] > 1) individual[i] = 1;   // 权重不能大于 1
            }
        }
    }

}
