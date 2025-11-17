import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

public class GA {
    private static final int POP_SIZE = 20;
    private static final int GENERATIONS = 100;
    private static final double MUTATION_RATE = 0.2;
    private static final int TOURNAMENT_SIZE = 3;
    private static final int DIMENSIONS = 5;

    private DataItem[] testData;
    private DataManager baseDM;
    private Random random;

    public GA(DataItem[] testData, DataManager baseDM, long seed) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random(seed);
    }

    private double evaluate(double[] weights) {
        return DataTest.score(weights, testData, baseDM);
    }

    public double run(PrintStream out, double baselineScore) {
        double[][] population = new double[POP_SIZE][DIMENSIONS];
        for (int i = 0; i < POP_SIZE; i++) {
            for (int j = 0; j < DIMENSIONS; j++) {
                population[i][j] = random.nextDouble();
            }
        }

        double[] bestIndividual = population[0].clone();
        double bestScore = evaluate(bestIndividual);
        baseDM.saveBestStats();

        for (int gen = 0; gen < GENERATIONS; gen++) {
            double[][] newPopulation = new double[POP_SIZE][DIMENSIONS];
            // 评估整个旧种群，为锦标赛选择做准备
            double[] scores = new double[POP_SIZE];
            for(int i = 0; i < POP_SIZE; i++) {
                scores[i] = evaluate(population[i]);
                if (scores[i] > bestScore) {
                    bestScore = scores[i];
                    bestIndividual = population[i].clone();
                    baseDM.saveBestStats();
                }
            }

            for (int i = 0; i < POP_SIZE; i++) {
                double[] parent1 = tournamentSelection(population, scores);
                double[] parent2 = tournamentSelection(population, scores);
                double[] child = crossover(parent1, parent2);
                mutate(child);
                newPopulation[i] = child;
            }
            population = newPopulation;
            double improvement = bestScore - baselineScore;
            out.printf("Gen %2d: Improvement = %.4f\n", gen + 1, Math.max(0, improvement));
        }

        out.println("\n=== GA Finished ===");
        double[] finalNormalizedBest = bestIndividual.clone();
        baseDM.normalizeL2(finalNormalizedBest);
        out.printf("Final Best Weights (Normalized) = %s\n", Arrays.toString(finalNormalizedBest));
        baseDM.printStats(out);
        return bestScore;
    }

    private double[] tournamentSelection(double[][] population, double[] scores) {
        int bestIndex = -1;
        double bestScore = -Double.MAX_VALUE;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            int index = random.nextInt(POP_SIZE);
            if (scores[index] > bestScore) {
                bestScore = scores[index];
                bestIndex = index;
            }
        }
        return population[bestIndex].clone();
    }

    private double[] crossover(double[] p1, double[] p2) { double[] c = new double[DIMENSIONS]; int cp = random.nextInt(DIMENSIONS); for(int i=0;i<DIMENSIONS;i++) c[i] = i<cp?p1[i]:p2[i]; return c; }
    private void mutate(double[] ind) { for(int i=0;i<DIMENSIONS;i++) if(random.nextDouble()<MUTATION_RATE) { ind[i]+=random.nextGaussian()*0.2; if(ind[i]<0)ind[i]=0; if(ind[i]>1)ind[i]=1; } }
}