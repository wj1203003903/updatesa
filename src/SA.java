import java.util.*;

public class SA {
    private static final int DIMENSIONS = 6;       // Ȩ��ά��
    private static final double INIT_TEMP = 1000.0; // ��ʼ�¶�
    private static final double MIN_TEMP = 1e-4;   // ��С�¶�
    private static final double COOLING_RATE = 0.95; // ����ϵ��
    private static final int ITERATIONS_PER_TEMP = 50;

    private DataItem[] testData;
    private DataManager baseDM;
    private Random random;

    public SA(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();
    }

    public double run() {
        double[] currentSolution = randomWeights();
        baseDM.normalizeL2(currentSolution);
        double currentScore = evaluate(currentSolution);

        double[] bestSolution = currentSolution.clone();
        double bestScore = currentScore;

        double temperature = INIT_TEMP;

        while (temperature > MIN_TEMP) {
            for (int i = 0; i < ITERATIONS_PER_TEMP; i++) {
                double[] neighborSolution = generateNeighbor(currentSolution);
                baseDM.normalizeL2(neighborSolution);
                double neighborScore = evaluate(neighborSolution);

                if (acceptanceProbability(currentScore, neighborScore, temperature) > random.nextDouble()) {
                    currentSolution = neighborSolution;
                    currentScore = neighborScore;

                    if (currentScore > bestScore) {
                        bestSolution = currentSolution.clone();
                        bestScore = currentScore;
                        System.out.printf("�����Ž�: %.4f, �¶�: %.4f, Ȩ��: %s\n",
                                bestScore, temperature, Arrays.toString(bestSolution));
                    }
                }
            }
            temperature *= COOLING_RATE;
        }

        System.out.println("\n=== SA �Ż���� ===");
        System.out.printf("��������Ȩ�� = %s\n", Arrays.toString(bestSolution));
        // ʹ������Ȩ�������һ�Σ���ӡ����״̬
        evaluate(bestSolution);
        baseDM.printStats();

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
        neighbor[idx] += (random.nextDouble() - 0.5) * 0.2; // С��Χ�Ŷ�

        if (neighbor[idx] < 0) neighbor[idx] = 0;
        if (neighbor[idx] > 1) neighbor[idx] = 1;

        return neighbor;
    }

    private double acceptanceProbability(double currentScore, double neighborScore, double temperature) {
        if (neighborScore > currentScore) {
            return 1.0;
        }
        return Math.exp((neighborScore - currentScore) / temperature);
    }

    private double evaluate(double[] weights) {
        return DataTest.score(weights, testData, baseDM);
    }
}