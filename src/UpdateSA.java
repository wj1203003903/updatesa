import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class UpdateSA {
    // --- 核心参数 ---
    private static final int DIMENSIONS = 4;
    private static final double INIT_TEMP = 100.0;
    private static final double MIN_TEMP = 1e-3;
    private static final double COOLING_RATE_BASE = 0.9;
    private static final int ITERATIONS_PER_TEMP_BASE = 30;

    // --- 扰动与局部搜索 ---
    private static final double LOCAL_DELTA = 0.03;
    private static final double LAMBDA = 1.0;

    // --- 自适应降温控制 ---
    private static final int STOP_GENERATION = 6;
    private static final double SLOWDOWN_FACTOR = 0.98;

    // ---  最优解记忆库与重启机制 ---
    private static final int ARCHIVE_SIZE = 5;
    private static final int RESTART_STAGNATION_THRESHOLD = 18;
    private static final double RESTART_TEMP_INCREASE_FACTOR = 2.0;
    private static final int MAX_RESTART_COUNT = 6;

    // ---  精英库多样性维护 ---
    private static final double DIVERSITY_THRESHOLD = 0.2; // 精英库成员间的最小距离

    // --- 基于接受率的步长自适应 ---
    private static final double ACCEPTANCE_RATE_TARGET = 0.4;
    private static final double STEP_ADJUST_FACTOR = 0.99;

    // --- 禁忌搜索思想 ---
    private static final int TABU_TENURE = 10;
    private static final double TABU_SIMILARITY_THRESHOLD = 0.01;

    // --- 成员变量 ---
    private final DataItem[] testData;
    private final DataManager baseDM;
    private final Random random;

    // --- 各策略对应的成员变量 ---
    private final List<double[]> eliteArchive;
    private final List<Double> eliteScores;
    private int restartCount; // 重启冷却计数器
    private double baseStep;
    private final Queue<double[]> tabuList;

    public UpdateSA(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();

        this.eliteArchive = new ArrayList<>();
        this.eliteScores = new ArrayList<>();

        this.restartCount = 0; // 初始化重启计数
        this.baseStep = 0.2;
        this.tabuList = new LinkedList<>();
    }

    public double run() {
        double[] current = randomWeights();
        double currentScore = evaluate(current);
        updateTabuList(current);

        double[] best = current.clone();
        double bestScore = currentScore;
        updateArchive(best, bestScore);

        double temperature = INIT_TEMP;
        int stagnationCount = 0;

        while (temperature > MIN_TEMP) {
            int iterations = ITERATIONS_PER_TEMP_BASE + (int) (ITERATIONS_PER_TEMP_BASE * (temperature / INIT_TEMP));
            int acceptedCount = 0;
            boolean improvedAtThisTemp = false;

            for (int it = 0; it < iterations; it++) {
                double[] neighbor;
                int tryCount = 0;
                do {
                    int[] subspace = chooseSubspace(temperature);
                    neighbor = generateNeighbor(current, subspace, temperature);
                    localSearch(neighbor, subspace);
                    tryCount++;
                } while (isTabu(neighbor) && tryCount < 10);

                double neighborScore = evaluate(neighbor);

                if (acceptanceProbability(currentScore, neighborScore, temperature) > random.nextDouble()) {
                    current = neighbor;
                    currentScore = neighborScore;
                    acceptedCount++;
                    updateTabuList(current);

                    if (currentScore > bestScore) {
                        best = current.clone();
                        bestScore = currentScore;
                        improvedAtThisTemp = true;
                        updateArchive(best, bestScore);
                    }
                }
            }

            double acceptanceRate = (double) acceptedCount / iterations;
            if (acceptanceRate > ACCEPTANCE_RATE_TARGET) {
                baseStep /= STEP_ADJUST_FACTOR;
            } else {
                baseStep *= STEP_ADJUST_FACTOR;
            }

            if (!improvedAtThisTemp) {
                stagnationCount++;
            } else {
                stagnationCount = 0;
            }

            // --- 重启机制---
            if (!eliteArchive.isEmpty() && stagnationCount >= RESTART_STAGNATION_THRESHOLD && restartCount < MAX_RESTART_COUNT) {
                System.out.printf("--- Triggering Restart #%d and removing solution from archive ---\n", restartCount + 1);
                restartCount++; // 重启次数加一

                // 从精英库中随机选择一个解作为跳板
                int randomIndex = random.nextInt(eliteArchive.size());
                current = eliteArchive.get(randomIndex).clone();
                currentScore = eliteScores.get(randomIndex);

                // 核心修改：从精英库中永久移除这个被用作跳板的解
                eliteArchive.remove(randomIndex);
                eliteScores.remove(randomIndex);

                // --- 重启冷却 ---
                // 重启温度随着重启次数的增加而衰减
                temperature *= RESTART_TEMP_INCREASE_FACTOR;

                // 可选：加一个上限，防止温度超过初始温度
                if (temperature > INIT_TEMP) {
                    temperature = INIT_TEMP;
                }

                stagnationCount = 0;
                tabuList.clear();
                updateTabuList(current);
                continue;
            }

            double coolingRate = COOLING_RATE_BASE;
            if (stagnationCount >= STOP_GENERATION) {
                coolingRate = Math.pow(COOLING_RATE_BASE, SLOWDOWN_FACTOR);
            }
            temperature *= coolingRate;
        }

        baseDM.normalizeL2(best);
        System.out.println("=== SA (Hybrid Advanced) Finished ===");
        System.out.printf("Best Weights = %s\n", Arrays.toString(best));
        baseDM.printStats();
        return bestScore;
    }

    // --- 辅助方法 ---

    // --- 更新精英档案库  ---
    private void updateArchive(double[] solution, double score) {
        // 检查与库中已有解的多样性
        for (int i = 0; i < eliteArchive.size(); i++) {
            if (distance(solution, eliteArchive.get(i)) < DIVERSITY_THRESHOLD) {
                // 如果新解与库中某个解过于相似，只有在新解分数更高时才替换
                if (score > eliteScores.get(i)) {
                    eliteArchive.set(i, solution.clone());
                    eliteScores.set(i, score);
                }
                return; // 相似，不再继续判断，直接返回
            }
        }

        // 如果不相似，并且名人堂还没满，直接加入
        if (eliteArchive.size() < ARCHIVE_SIZE) {
            eliteArchive.add(solution.clone());
            eliteScores.add(score);
            return;
        }

        // 如果名人堂满了，则替换掉分数最低的那个
        int worstIdx = -1;
        double worstScore = Double.POSITIVE_INFINITY;
        for (int i = 0; i < ARCHIVE_SIZE; i++) {
            if (eliteScores.get(i) < worstScore) {
                worstScore = eliteScores.get(i);
                worstIdx = i;
            }
        }

        // 只有当新解比最差的解更好时才替换
        if (score > worstScore) {
            eliteArchive.set(worstIdx, solution.clone());
            eliteScores.set(worstIdx, score);
        }
    }

    private void updateTabuList(double[] solution) {
        if (tabuList.size() >= TABU_TENURE) tabuList.poll();
        tabuList.add(solution.clone());
    }

    private boolean isTabu(double[] solution) {
        for (double[] tabuSolution : tabuList) {
            if (distance(solution, tabuSolution) < TABU_SIMILARITY_THRESHOLD)
                return true;
        }
        return false;
    }

    private double distance(double[] s1, double[] s2) {
        double sum = 0;
        for (int i = 0; i < DIMENSIONS; i++) sum += (s1[i] - s2[i]) * (s1[i] - s2[i]);
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
        for (int i = 0; i < DIMENSIONS; i++) w[i] = random.nextDouble();
        return w;
    }

    private int[] chooseSubspace(double temperature) {
        double tRatio = Math.min(1.0, temperature / INIT_TEMP);
        int k = (int) Math.round(1 + (DIMENSIONS - 1) * tRatio);
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

    private void localSearch(double[] candidate, int[] subspace) {
        double bestLocalScore = evaluate(candidate);
        double[] bestLocal = candidate.clone();
        for (int idx : subspace) {
            double old = candidate[idx];
            candidate[idx] = clamp(old + LOCAL_DELTA, 0.0, 1.0);
            double sPlus = evaluate(candidate);
            if (sPlus > bestLocalScore) {
                bestLocalScore = sPlus;
                bestLocal = candidate.clone();
            }
            candidate[idx] = clamp(old - LOCAL_DELTA, 0.0, 1.0);
            double sMinus = evaluate(candidate);
            if (sMinus > bestLocalScore) {
                bestLocalScore = sMinus;
                bestLocal = candidate.clone();
            }
            candidate[idx] = old;
        }
        System.arraycopy(bestLocal, 0, candidate, 0, DIMENSIONS);
    }

    private double acceptanceProbability(double currentScore, double neighborScore, double temperature) {
        if (neighborScore > currentScore) return 1.0;
        return Math.exp((neighborScore - currentScore) / (LAMBDA * Math.max(1e-12, temperature)));
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