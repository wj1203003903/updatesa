import java.util.Arrays;
import java.util.Random;

/**
 * һ��������Ļ���ģ��.
 * ��ֻ����һ�����Ȩ�أ�������÷֣�Ȼ��ֱ��������.
 * �����ں����κ��Ż��㷨������������������PSO����������������ļ�ֵ.
 */
public class Nomal {
    private static final int DIMENSIONS = 4;        // ά��������Ȩ��������

    private DataItem[] testData;
    private DataManager baseDM;
    private Random random;

    // ���캯�� (�������汾����һ�£������滻)
    public Nomal(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();
    }

    /**
     * ���е����������
     */
    public double run() {
        // 1. ����һ�����Ȩ��
        double[] best = new double[DIMENSIONS];
        for (int j = 0; j < DIMENSIONS; j++) {
            best[j] = random.nextDouble(); // �� [0.0, 1.0) �������������
        }

        // 2. ��������Ȩ�صĵ÷�
        double bestScore = evaluate(best);

        // 3. ��һ��Ȩ�ز���ӡ��� (��PSO�汾��ʽ����һ��)
        baseDM.normalizeL2(best);
        System.out.println("=== Single Random Attempt Finished ===");
        System.out.printf("Best Weights = %s\n", Arrays.toString(best));
        baseDM.printStats();
        return bestScore;
    }

    /**
     * �������� (�������汾��ȫ��ͬ)
     * @param w Ȩ������
     * @return �����÷�
     */
    private double evaluate(double[] w) {
        return DataTest.score(w, testData, baseDM);
    }
}