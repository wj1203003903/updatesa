import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class UpdateSA {
    // --- 【最终优化版】推荐参数 ---
    private static final int DIMENSIONS = 5;
    private static final double INIT_TEMP = 1000.0;        // 足够高的初始温度，以支持大范围探索
    private static final double MIN_TEMP = 2e-3;           // 足够低的终止温度，用于精细收敛
    private static final double COOLING_RATE = 0.9;      // 【关键】非常慢的降温速率，给予充分探索时间
    private static final int ITERATIONS_PER_TEMP = 120;     // 【关键】在每个温度下进行更多次尝试（因为速度变快了）

    // --- 精英存档与重启机制参数 ---
    private static final int ARCHIVE_SIZE = 5;              // 精英解存档数量
    private static final int RESTART_STAGNATION_THRESHOLD = 25; // 停滞25代后考虑重启
    private static final double RESTART_TEMP_INCREASE_FACTOR = 1.5; // 重启时温度恢复系数
    private static final int MAX_RESTART_COUNT = 5;         // 最多重启5次
    private static final double DIVERSITY_THRESHOLD = 0.1;   // 精英解之间的最小距离

    // --- 成员变量 ---
    private final DataItem[] testData;
    private final DataManager baseDM;
    private final Random random;

    // --- 算法状态变量 ---
    private final List<double[]> eliteArchive;
    private final List<Double> eliteScores;
    private int restartCount;
    private double baseStep; // 用于自适应步长

    public UpdateSA(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();
        this.eliteArchive = new ArrayList<>();
        this.eliteScores = new ArrayList<>();
        this.restartCount = 0;
        this.baseStep = 0.4; // 初始步长可以稍大一些
    }

    public double run() {
        // 初始化
        double[] current = randomWeights();
        double currentScore = evaluate(current);
        double[] best = current.clone();
        double bestScore = currentScore;
        updateArchive(best, bestScore);

        double temperature = INIT_TEMP;
        int stagnationCount = 0;
        int generation = 0;

        while (temperature > MIN_TEMP) {
            generation++;
            int acceptedCount = 0;
            boolean improvedAtThisTemp = false;

            for (int it = 0; it < ITERATIONS_PER_TEMP; it++) {
                // 【核心简化】直接生成邻域解，完全移除了 localSearch 和 isTabu
                int[] subspace = chooseSubspace(temperature);
                double[] neighbor = generateNeighbor(current, subspace, temperature);
                double neighborScore = evaluate(neighbor);

                if (acceptanceProbability(currentScore, neighborScore, temperature) > random.nextDouble()) {
                    current = neighbor;
                    currentScore = neighborScore;
                    acceptedCount++;
                    if (currentScore > bestScore) {
                        best = current.clone();
                        bestScore = currentScore;
                        improvedAtThisTemp = true;
                        updateArchive(best, bestScore);
                    }
                }
            }

            // 打印信息
            double improvement = bestScore - Main.baselineScore;
            System.out.printf("Gen %2d (Temp %.2e): Improvement = %.4f\n",
                    generation, temperature, improvement > 0 ? improvement : 0);

            // 【保留】自适应步长
            double acceptanceRate = (double) acceptedCount / ITERATIONS_PER_TEMP;
            if (acceptanceRate > 0.4) { // 接受率过高，说明步长太小，需要扩大探索
                baseStep /= 0.99;
            } else { // 接受率过低，说明步长太大，需要缩小探索
                baseStep *= 0.99;
            }

            // 更新停滞计数
            if (!improvedAtThisTemp) {
                stagnationCount++;
            } else {
                stagnationCount = 0;
            }

            // 重启机制
            if (!eliteArchive.isEmpty() && stagnationCount >= RESTART_STAGNATION_THRESHOLD && restartCount < MAX_RESTART_COUNT) {
                System.out.printf("--- Stagnation detected. Triggering Restart #%d ---\n", restartCount + 1);
                restartCount++;
                int randomIndex = random.nextInt(eliteArchive.size());
                current = eliteArchive.get(randomIndex).clone();
                currentScore = eliteScores.get(randomIndex);
                temperature *= RESTART_TEMP_INCREASE_FACTOR;
                if (temperature > INIT_TEMP) temperature = INIT_TEMP;
                stagnationCount = 0;
                continue;
            }

            // 标准降温
            temperature *= COOLING_RATE;
        }

        System.out.println("\n=== SA (Optimized Hybrid) Finished ===");
        double[] finalNormalizedBest = best.clone();
        baseDM.normalizeL2(finalNormalizedBest);
        System.out.printf("Final Best Weights (Normalized) = %s\n", Arrays.toString(finalNormalizedBest));

        evaluate(best);
        baseDM.printStats();
        return bestScore;
    }

    // --- 辅助方法 ---

    private void updateArchive(double[] solution, double score) {
        for (int i = 0; i < eliteArchive.size(); i++) {
            if (distance(solution, eliteArchive.get(i)) < DIVERSITY_THRESHOLD) {
                if (score > eliteScores.get(i)) {
                    eliteArchive.set(i, solution.clone());
                    eliteScores.set(i, score);
                }
                return;
            }
        }
        if (eliteArchive.size() < ARCHIVE_SIZE) {
            eliteArchive.add(solution.clone());
            eliteScores.add(score);
            return;
        }
        int worstIdx = -1;
        double worstScore = Double.POSITIVE_INFINITY;
        for (int i = 0; i < ARCHIVE_SIZE; i++) {
            if (eliteScores.get(i) < worstScore) {
                worstScore = eliteScores.get(i);
                worstIdx = i;
            }
        }
        if (score > worstScore) {
            eliteArchive.set(worstIdx, solution.clone());
            eliteScores.set(worstIdx, score);
        }
    }

    private double distance(double[] s1, double[] s2) {
        double sum = 0;
        for (int i = 0; i < DIMENSIONS; i++) {
            sum += (s1[i] - s2[i]) * (s1[i] - s2[i]);
        }
        return Math.sqrt(sum);
    }

    private double[] generateNeighbor(double[] current, int[] subspace, double temperature) {
        double[] neighbor = current.clone();
        double tempFactor = Math.min(1.0, temperature / INIT_TEMP);
        double sigma = this.baseStep * (0.3 + 0.7 * tempFactor);
        for (int idx : subspace) {
            neighbor[idx] += random.nextGaussian() * sigma;
            neighbor[idx] = clamp(neighbor[idx], 0, 1);
        }
        return neighbor;
    }

    private double[] randomWeights() {
        double[] w = new double[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            w[i] = random.nextDouble();
        }
        return w;
    }

    private int[] chooseSubspace(double temperature) {
        double tRatio = Math.min(1.0, temperature / INIT_TEMP);
        int k = 1 + (int) Math.round((DIMENSIONS - 1) * tRatio);
        return randomSampleIndices(k);
    }

    private int[] randomSampleIndices(int k) {
        k = Math.max(1, Math.min(k, DIMENSIONS));
        int[] all = new int[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) all[i] = i;
        for (int i = 0; i < k; i++) {
            int j = i + random.nextInt(DIMENSIONS - i);
            int tmp = all[i]; all[i] = all[j]; all[j] = tmp;
        }
        int[] res = new int[k];
        System.arraycopy(all, 0, res, 0, k);
        return res;
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

    private double clamp(double v, double lo, double hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }
}