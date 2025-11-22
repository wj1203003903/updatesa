import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

public class GA {
    private static final int POP_SIZE = 20;
    private static final int GENERATIONS = 100;
    private static final double MUTATION_RATE = 0.5;
    private static final int TOURNAMENT_SIZE = 2;
    private static final int DIMENSIONS = 5;

    private DataItem[] testData;
    private DataManager baseDM;
    private Random random;

    public double[] bestSolution;
    public double bestScore;

    public GA(DataItem[] testData, DataManager baseDM, long seed) {
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
        double[][] population = new double[POP_SIZE][DIMENSIONS];

        // 1. 初始化种群
        for (int i = 0; i < POP_SIZE; i++) {
            // Warm Start: 第一个个体继承旧权重
            if (i == 0 && initialWeights != null) {
                population[i] = initialWeights.clone();
            } else {
                for (int j = 0; j < DIMENSIONS; j++) {
                    population[i][j] = random.nextDouble();
                }
            }
        }

        // 初始评估
        this.bestSolution = population[0].clone();
        this.bestScore = evaluate(this.bestSolution);
        baseDM.saveBestStats();

        // 2. 进化循环
        for (int gen = 0; gen < GENERATIONS; gen++) {
            double[][] newPopulation = new double[POP_SIZE][DIMENSIONS];
            double[] scores = new double[POP_SIZE];

            // 评估
            for(int i = 0; i < POP_SIZE; i++) {
                scores[i] = evaluate(population[i]);
                if (scores[i] > this.bestScore) {
                    this.bestScore = scores[i];
                    this.bestSolution = population[i].clone();
                    baseDM.saveBestStats();
                }
            }

            // 繁殖
            for (int i = 0; i < POP_SIZE; i++) {
                double[] parent1 = tournamentSelection(population, scores);
                double[] parent2 = tournamentSelection(population, scores);
                double[] child = crossover(parent1, parent2);
                mutate(child);
                newPopulation[i] = child;
            }
            population = newPopulation;
        }
        return this.bestSolution;
    }

    public double run(PrintStream out, double baselineScore) {
        optimize(null);
        if (baselineScore != -9999) out.println("GA Finished.");
        return bestScore;
    }

    private double[] tournamentSelection(double[][] population, double[] scores) {
        int bestIndex = -1;
        double bestScoreTemp = -Double.MAX_VALUE;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            int index = random.nextInt(POP_SIZE);
            if (scores[index] > bestScoreTemp) {
                bestScoreTemp = scores[index];
                bestIndex = index;
            }
        }
        return population[bestIndex].clone();
    }

    private double[] crossover(double[] p1, double[] p2) {
        double[] c = new double[DIMENSIONS];
        int cp = random.nextInt(DIMENSIONS);
        for(int i=0;i<DIMENSIONS;i++) c[i] = i<cp?p1[i]:p2[i];
        return c;
    }

    private void mutate(double[] ind) {
        for(int i=0;i<DIMENSIONS;i++)
            if(random.nextDouble()<MUTATION_RATE) {
                ind[i]+=random.nextGaussian()*0.2;
                if(ind[i]<0)ind[i]=0; if(ind[i]>1)ind[i]=1;
            }
    }
}