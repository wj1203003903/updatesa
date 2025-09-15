import java.util.*;

public class PSO {
    private static final int POP_SIZE = 20;         // ��������
    private static final int GENERATIONS = 40;      // ��������
    private static final double INERTIA = 0.7;      // ����Ȩ��
    private static final double COGNITIVE = 1.5;    // ����ѧϰ����
    private static final double SOCIAL = 1.5;       // Ⱥ��ѧϰ����
    private static final int DIMENSIONS = 4;        // ά��������Ȩ��������

    private static DataItem[] testData;
    private static DataManager baseDM;
    private static Random random;

    // ���캯��
    public PSO(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();
    }

    public void run() {
        double[][] position = new double[POP_SIZE][DIMENSIONS];
        double[][] velocity = new double[POP_SIZE][DIMENSIONS];
        double[][] pBestPosition = new double[POP_SIZE][DIMENSIONS];
        double[] pBestScore = new double[POP_SIZE];

        double[] gBestPosition = new double[DIMENSIONS];
        double gBestScore = -Double.MAX_VALUE;

        // ��ʼ�����ӵ�λ�ú��ٶ�
        for (int i = 0; i < POP_SIZE; i++) {
            for (int j = 0; j < DIMENSIONS; j++) {
                position[i][j] = random.nextDouble();
                velocity[i][j] = random.nextDouble() * 0.1 - 0.05;  // ��ʼ�ٶ��� [-0.05, 0.05]
            }
            pBestPosition[i] = position[i].clone();
            pBestScore[i] = evaluate(position[i]);

            if (pBestScore[i] > gBestScore) {
                gBestScore = pBestScore[i];
                gBestPosition = pBestPosition[i].clone();
            }
        }

        for (int gen = 0; gen < GENERATIONS; gen++) {
            for (int i = 0; i < POP_SIZE; i++) {
                for (int j = 0; j < DIMENSIONS; j++) {
                    double r1 = random.nextDouble();
                    double r2 = random.nextDouble();
                    velocity[i][j] = INERTIA * velocity[i][j]
                            + COGNITIVE * r1 * (pBestPosition[i][j] - position[i][j])
                            + SOCIAL * r2 * (gBestPosition[j] - position[i][j]);

                    position[i][j] += velocity[i][j];

                    // ����λ���� [0,1]
                    if (position[i][j] < 0) position[i][j] = 0;
                    if (position[i][j] > 1) position[i][j] = 1;
                }

                double score = evaluate(position[i]);
                if (score > pBestScore[i]) {
                    pBestScore[i] = score;
                    pBestPosition[i] = position[i].clone();
                }

                if (score > gBestScore) {
                    gBestScore = score;
                    gBestPosition = position[i].clone();
               
                }
            }

        }
        baseDM.normalizeL2(gBestPosition);
        System.out.println("=== PSO Finished ===");
        System.out.printf("Best Score = %.4f\nBest Weights = %s\n", gBestScore*100, Arrays.toString(gBestPosition));
        baseDM.printStats();
    }

    // ��������
    private double evaluate(double[] w) {
        return DataTest.score(w, testData, baseDM);
    }
}
