import java.util.*;

public class GA {
    private static final int POP_SIZE = 20;
    private static final int GENERATIONS = 40;
    private static final double MUTATION_RATE = 0.2;
    private static final int TOURNAMENT_SIZE = 3;
    private static DataItem[] testData;
    private static DataManager baseDM;
    private static Random random;

    // ���캯�������� testData �� baseDM ��Ϊ����
    public GA(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();
    }

    public double run() {
        double[][] population = new double[POP_SIZE][4];

        // ��ʼ����Ⱥ
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

            // ������Ⱥ
            population = newPopulation;

        }
        baseDM.normalizeL2(best);
        System.out.println("=== GA Finished ===");
        System.out.printf("Best Weights = %s\n", Arrays.toString(best));
        baseDM.printStats();
        return bestScore;
    }

    // �������壨������ĳһ��Ȩ���µķ�����
    private double evaluate(double[] w) {
   
        return DataTest.score(w, testData, baseDM);
    }

    // ������ѡ��
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

    // ������������������������Ȩ��ƽ���������Ӵ�
 // �Ľ��Ľ��������ʹ�õ��㽻��
    private double[] crossover(double[] p1, double[] p2) {
        Random rand = new Random();
        double[] child = new double[4];
        int crossoverPoint = rand.nextInt(4);  // ���ѡ�񽻲��

        for (int i = 0; i < 4; i++) {
            if (i < crossoverPoint) {
                child[i] = p1[i];
            } else {
                child[i] = p2[i];
            }
        }
        return child;
    }

    // �Ľ��ı�����������ӱ�����Ȳ����Ʒ�Χ
    private void mutate(double[] individual) {
        Random rand = new Random();
        for (int i = 0; i < 4; i++) {
            if (rand.nextDouble() < MUTATION_RATE) {
                individual[i] += rand.nextGaussian() * 0.2;  // ���ӱ������
                if (individual[i] < 0) individual[i] = 0;   // Ȩ�ز���Ϊ��
                if (individual[i] > 1) individual[i] = 1;   // Ȩ�ز��ܴ��� 1
            }
        }
    }

}
