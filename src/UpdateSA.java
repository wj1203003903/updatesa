import java.util.Arrays;
import java.util.Random;

/**
 * ��ǿ��ģ���˻�SA��ʵ�֣�
 * - ֧���ڲ�ͬ�ӿռ䣨subspace��������������ͬʱ�Ŷ����ά�ȣ�
 * - �Ŷ��������¶ȶ�̬���ţ����´��Ŷ�������С�Ŷ���
 * - ÿ�����������ֲ�΢����local search��
 * - ����Ӧ���£�����ʱ�����������������
 * - ��̬ÿ�¶ȵ������������¸��ೢ�ԣ�
 *
 * �����ⲿ��DataTest.score(weights, testData, baseDM)
 *           baseDM.normalizeL2(weights)
 *           baseDM.printStats()
 */
public class UpdateSA {
    private static final int DIMENSIONS = 4;

    // �����¶Ȳ������ɵ�����
    private static final double INIT_TEMP = 100.0;
    private static final double MIN_TEMP = 1e-3;
    private static final double COOLING_RATE_BASE = 0.9; // ��������ϵ������0.6�Ƚ���
    private static final int ITERATIONS_PER_TEMP_BASE = 50; // ����ÿ�¶ȵ�������

    // �ֲ��������Ŷ�����
    private static final double BASE_STEP = 0.2;    // �����Ŷ���׼�����ʱ���¶����ӣ�
    private static final double LOCAL_DELTA = 0.03; // �ֲ�΢����������LOCAL_DELTA��
    private static final double LAMBDA = 1.0;       // ���ܸ��ʹ�һ�����ӣ��ɵ�

    // ����Ӧ���¿���
    private static final int STAGNATION_THRESHOLD = 6; // ���������¶�û����������Ϊ��ͣ�͡�
    private static final double SLOWDOWN_FACTOR = 0.98; // ͣ��ʱ�ѽ���ϵ�����Ը�ֵ��ʹ���¸�����

    private final DataItem[] testData;
    private final DataManager baseDM;
    private final Random random;

    public UpdateSA(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();
    }

    public void run() {
        // ��ʼ��
        double[] current = randomWeights();
        double currentScore = evaluate(current);

        double[] best = current.clone();
        double bestScore = currentScore;

        double temperature = INIT_TEMP;
        double coolingRate = COOLING_RATE_BASE;

        int stagnationCount = 0; // ����δ�Ľ�����

        while (temperature > MIN_TEMP) {
            // ��̬ÿ�¶ȵ������������¶ೢ�ԣ������ٳ���
            int iterations = ITERATIONS_PER_TEMP_BASE + (int) (ITERATIONS_PER_TEMP_BASE * (temperature / INIT_TEMP));

            boolean improvedAtThisTemp = false;
            for (int it = 0; it < iterations; it++) {
                // ѡ���ӿռ䣨�����¶Ⱦ���ά��������
                int[] subspace = chooseSubspace(temperature);

                // ��������⣨��ѡ�е��ӿռ��Ŷ���
                double[] neighbor = generateNeighbor(current, subspace, temperature);

                // ����������ֲ�΢����ֻ���ӿռ���΢����
                localSearch(neighbor, subspace);

                double neighborScore = evaluate(neighbor);

                // ����׼��
                if (acceptanceProbability(currentScore, neighborScore, temperature) > random.nextDouble()) {
                    current = neighbor;
                    currentScore = neighborScore;

                    if (currentScore > bestScore) {
                        best = current.clone();
                        bestScore = currentScore;
                        improvedAtThisTemp = true;
                    }
                }
            }

            // ����Ӧ���£������¶ȶ�������������Ϊ����ͣ�� -> ��������
            if (!improvedAtThisTemp) {
                stagnationCount++;
            } else {
                stagnationCount = 0;
            }

            if (stagnationCount >= STAGNATION_THRESHOLD) {
                // �������£����㷨����̽������
                coolingRate = COOLING_RATE_BASE * SLOWDOWN_FACTOR;
            } else {
                coolingRate = COOLING_RATE_BASE;
            }

            // Ӧ�ý���
            temperature *= coolingRate;
        }

        // �淶����������
        baseDM.normalizeL2(best);
        System.out.println("=== SA (enhanced) Finished ===");
        System.out.printf("Best Score = %.4f\nBest Weights = %s\n", bestScore * 100.0, Arrays.toString(best));
        baseDM.printStats();
    }

    // -------------------- ���ķ��� --------------------

    // �����ʼ��Ȩ�أ�0~1��
    private double[] randomWeights() {
        double[] w = new double[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            w[i] = random.nextDouble();
        }
        return w;
    }

    /**
     * �����¶�ѡ���ӿռ䣨ѡ����Щά��һ���Ŷ���
     * ����ʱ������ѡ�����ά�ȣ�����ʱѡ�����ά�ȣ�������
     */
    private int[] chooseSubspace(double temperature) {
        double tRatio = Math.min(1.0, temperature / INIT_TEMP); // [0,1]
        // ����ͬʱ�Ŷ���ά������������1�����DIMENSIONS
        // ����ӳ�䣺���� -> �����ӿռ�
        int maxDims = DIMENSIONS;
        int minDims = 1;
        int k = (int) Math.round(minDims + (maxDims - minDims) * tRatio);

        // Ϊ�˶����ԣ��Ը���ѡ�����ά�ȣ�������� k ��������
        int[] indices = randomSampleIndices(k);
        return indices;
    }

    // �� [0..DIMENSIONS-1] ��������� k �����ظ�����
    private int[] randomSampleIndices(int k) {
        k = Math.max(1, Math.min(k, DIMENSIONS));
        int[] all = new int[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) all[i] = i;
        // Fisher-Yates ǰ k ��
        for (int i = 0; i < k; i++) {
            int j = i + random.nextInt(DIMENSIONS - i);
            int tmp = all[i];
            all[i] = all[j];
            all[j] = tmp;
        }
        int[] res = new int[k];
        System.arraycopy(all, 0, res, 0, k);
        return res;
    }

    /**
     * �ڸ����ӿռ����Ŷ����������
     * �Ŷ����Ȱ��¶����ţ����´󲽳�������С����
     */
    private double[] generateNeighbor(double[] current, int[] subspace, double temperature) {
        double[] neighbor = current.clone();

        // �¶����ӣ�ʹ����ʱ�Ŷ�����
        double tempFactor = Math.min(1.0, temperature / INIT_TEMP); // [0,1]
        double sigma = BASE_STEP * (0.3 + 0.7 * tempFactor); // sigma ���������¶ȱ仯������̫С��

        for (int idx : subspace) {
            neighbor[idx] += random.nextGaussian() * sigma;
            // ��֤�߽�
            if (neighbor[idx] < 0) neighbor[idx] = 0;
            if (neighbor[idx] > 1) neighbor[idx] = 1;
        }
        return neighbor;
    }

    /**
     * �ֲ����������ӿռ���ÿ��ά���� ��LOCAL_DELTA ��С�����ԣ�̰�ģ�
     * ֻ�������ӿռ��ڲ��ĸĽ��������޸ķ��ӿռ�ά�ȣ�
     */
    private void localSearch(double[] candidate, int[] subspace) {
        double bestLocalScore = evaluate(candidate);
        double[] bestLocal = candidate.clone();

        // ��ÿ��ά�ȳ��� +delta / -delta
        for (int idx : subspace) {
            // ���� +delta
            double old = candidate[idx];
            candidate[idx] = clamp(old + LOCAL_DELTA, 0.0, 1.0);
            double sPlus = evaluate(candidate);
            if (sPlus > bestLocalScore) {
                bestLocalScore = sPlus;
                bestLocal = candidate.clone();
            }
            // �ָ������� -delta
            candidate[idx] = clamp(old - LOCAL_DELTA, 0.0, 1.0);
            double sMinus = evaluate(candidate);
            if (sMinus > bestLocalScore) {
                bestLocalScore = sMinus;
                bestLocal = candidate.clone();
            }
            // �ָ�Ϊԭֵ�Ա���һ��ά�ȳ���
            candidate[idx] = old;
        }

        // �� candidate ����Ϊ�ֲ����ţ������������
        System.arraycopy(bestLocal, 0, candidate, 0, DIMENSIONS);
    }

    // ���ܸ��ʣ�Metropolis�������� lambda ����
    private double acceptanceProbability(double currentScore, double neighborScore, double temperature) {
        if (neighborScore > currentScore) {
            return 1.0;
        }
        // ע�⣺temperature ���ܽ�С��������Ϊ0
        return Math.exp((neighborScore - currentScore) / (LAMBDA * Math.max(1e-12, temperature)));
    }

    // ���ְ�װ�������ⲿ DataTest��
    private double evaluate(double[] weights) {
        return DataTest.score(weights, testData, baseDM);
    }

    // ���ߣ���ס����
    private double clamp(double v, double lo, double hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }
}
