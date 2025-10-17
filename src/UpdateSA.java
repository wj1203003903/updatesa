import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class UpdateSA {
    // --- 内部类：用于封装解和分数，使代码更简洁、更健壮 ---
    private static class EliteSolution implements Comparable<EliteSolution> {
        final double[] solution;
        final double score;

        EliteSolution(double[] solution, double score) {
            this.solution = solution;
            this.score = score;
        }

        @Override
        public int compareTo(EliteSolution other) {
            // 用于排序，分数高的在前
            return Double.compare(other.score, this.score);
        }
    }

    // --- 参数 ---
    private static final int DIMENSIONS = 5;                        // 解向量的维度（基因数量）
    private static final double INIT_TEMP = 1000.0;                 // 初始温度：决定了算法初期的探索胆量
    private static final double MIN_TEMP = 2e-3;                    // 终止温度：当温度低于此值时，算法结束
    private static final double COOLING_RATE = 0.9;                 // 冷却速率：每一代温度降低的比例
    private static final int ITERATIONS_PER_TEMP = 120;             // 每个温度下的迭代次数：决定了在当前“胆量”下探索的充分程度

    // --- 精英存档与GA救援触发参数 ---
    private static final int ARCHIVE_SIZE = 15;                     // 精英存档的大小：决定了GA救援时的“初始种群”规模
    private static final int RESTART_STAGNATION_THRESHOLD = 10;     // 【GA救援触发器】停滞容忍上限：当连续这么多代最优解没有更新时，触发GA救援
    private static final double RESTART_TEMP_INCREASE_FACTOR = 1.5; // 重启/救援后的温度提升系数：让算法重新获得探索活力
    private static final int MAX_RESTART_COUNT = 5;                 // 最大救援次数：限制GA救援这个“大招”的使用次数
    private static final double DIVERSITY_THRESHOLD = 0.15;          // 精英多样性门槛：确保精英存档中的解足够不同，为GA提供多样化的基因

    // --- GA救援阶段自身参数 ---
    private static final int GA_RESCUE_GENERATIONS = 5;             // GA救援运行的代数：一次救援中基因重组的轮次
    private static final double GA_RESCUE_MUTATION_RATE = 0.2;       // GA救援中的变异率：新生代个体发生基因突变的概率
    private static final int GA_RESCUE_TOURNAMENT_SIZE = 2;         // GA救援中的锦标赛选择规模：决定了“选拔父母”的竞争激烈程度

    // --- 动态步长SA的参数 ---
    private static final double CAUCHY_PROBABILITY = 0.1;           // 使用柯西分布进行大跳跃的概率
    private static final double ACCEPTANCE_RATE_TARGET = 0.4;       // 目标接受率，用于调整步长
    private static final double STEP_ADJUST_FACTOR = 0.99;          // 步长调整因子

    // --- 成员变量 ---
    private final DataItem[] testData;
    private final DataManager baseDM;
    private final Random random;
    private final List<EliteSolution> eliteArchive;
    private int restartCount;
    private double[] best;
    private double bestScore;
    private double baseStep;

    public UpdateSA(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random(Main.randomseal);
        this.eliteArchive = new ArrayList<>();
        this.restartCount = 0;
        this.best = new double[DIMENSIONS];
        this.bestScore = -Double.MAX_VALUE;
        this.baseStep = 0.4;
    }

    public double run() {
        double[] current = randomWeights();
        double currentScore = evaluate(current);

        double temperature = INIT_TEMP;
        int stagnationCount = 0;
        int generation = 0;

        while (temperature > MIN_TEMP) {
            generation++;
            double bestScoreAtTempStart = this.bestScore;
            int acceptedCount = 0;

            for (int it = 0; it < ITERATIONS_PER_TEMP; it++) {
                double[] neighbor = generateNeighborSA(current, temperature);
                double neighborScore = evaluate(neighbor);

                if (acceptanceProbability(currentScore, neighborScore, temperature) > random.nextDouble()) {
                    current = neighbor;
                    currentScore = neighborScore;
                    acceptedCount++;
                }
            }

            if (this.bestScore > bestScoreAtTempStart) {
                stagnationCount = 0;
            } else {
                stagnationCount++;
            }

            double acceptanceRate = (double) acceptedCount / ITERATIONS_PER_TEMP;
            if (acceptanceRate > ACCEPTANCE_RATE_TARGET) {
                baseStep /= STEP_ADJUST_FACTOR;
            } else {
                baseStep *= STEP_ADJUST_FACTOR;
            }

            double improvement = this.bestScore - Main.baselineScore;
            System.out.printf("Gen %2d (Temp %.2e): Improvement = %.4f\n",
                    generation, temperature, improvement > 0 ? improvement : 0);

            if (eliteArchive.size() >= ARCHIVE_SIZE / 2 && stagnationCount >= RESTART_STAGNATION_THRESHOLD && restartCount < MAX_RESTART_COUNT) {
                System.out.printf("--- Stagnation detected. Triggering GA Rescue #%d ---\n", restartCount + 1);
                restartCount++;
                runGARescue();
                int newStartIndex = random.nextInt(Math.min(5, eliteArchive.size()));
                current = eliteArchive.get(newStartIndex).solution.clone();
                currentScore = eliteArchive.get(newStartIndex).score;
                temperature *= RESTART_TEMP_INCREASE_FACTOR;
                if (temperature > INIT_TEMP) temperature = INIT_TEMP;
                stagnationCount = 0;
                continue;
            }

            temperature *= COOLING_RATE;
        }

        System.out.println("\n=== SA with GA-Rescue Finished ===");
        double[] finalNormalizedBest = this.best.clone();
        baseDM.normalizeL2(finalNormalizedBest);
        System.out.printf("Final Best Weights (Normalized) = %s\n", Arrays.toString(finalNormalizedBest));
        evaluate(this.best);
        baseDM.printStats();
        return this.bestScore;
    }

    private double[] generateNeighborSA(double[] current, double temperature) {
        double[] neighbor = current.clone();
        int[] subspace = chooseSubspace(temperature);

        if (random.nextDouble() < CAUCHY_PROBABILITY) {
            double scale = this.baseStep * 0.1;
            for (int idx : subspace) {
                neighbor[idx] += scale * Math.tan(Math.PI * (random.nextDouble() - 0.5));
                neighbor[idx] = clamp(neighbor[idx], 0, 1);
            }
        } else {
            double tempFactor = Math.min(1.0, temperature / INIT_TEMP);
            double sigma = this.baseStep * (0.3 + 0.7 * tempFactor);
            for (int idx : subspace) {
                neighbor[idx] += random.nextGaussian() * sigma/1.6;
                neighbor[idx] = clamp(neighbor[idx], 0, 1);
            }
        }
        return neighbor;
    }

    private double evaluate(double[] weights) {
        double score = DataTest.score(weights, testData, baseDM);
        if (score > this.bestScore) {
            this.bestScore = score;
            System.arraycopy(weights, 0, this.best, 0, DIMENSIONS);
            updateArchive(new EliteSolution(this.best.clone(), this.bestScore));
        }
        return score;
    }

    private void runGARescue() {
        List<EliteSolution> currentPopulation = new ArrayList<>(eliteArchive);

        for (int gen = 0; gen < GA_RESCUE_GENERATIONS; gen++) {
            List<EliteSolution> newPopulation = new ArrayList<>();
            if (!currentPopulation.isEmpty()) {
                currentPopulation.sort(null);
                newPopulation.add(currentPopulation.get(0));
            }
            while (newPopulation.size() < currentPopulation.size()) {
                EliteSolution parent1 = tournamentSelection(currentPopulation);
                EliteSolution parent2 = tournamentSelection(currentPopulation);
                double[] childSolution = crossover(parent1.solution, parent2.solution);
                mutate(childSolution);
                double childScore = evaluate(childSolution);
                newPopulation.add(new EliteSolution(childSolution, childScore));
            }
            currentPopulation = newPopulation;
        }

        eliteArchive.sort(null);
        while (eliteArchive.size() > 3) {
            eliteArchive.remove(eliteArchive.size() - 1);
        }

        for (EliteSolution solution : currentPopulation) {
            updateArchive(solution);
        }
    }

    private EliteSolution tournamentSelection(List<EliteSolution> population) {
        EliteSolution bestInTournament = null;
        for (int i = 0; i < GA_RESCUE_TOURNAMENT_SIZE; i++) {
            int index = random.nextInt(population.size());
            EliteSolution individual = population.get(index);
            if (bestInTournament == null || individual.score > bestInTournament.score) {
                bestInTournament = individual;
            }
        }
        return bestInTournament;
    }

    private void updateArchive(EliteSolution newSolution) {
        for (int i = 0; i < eliteArchive.size(); i++) {
            if (distance(newSolution.solution, eliteArchive.get(i).solution) < DIVERSITY_THRESHOLD) {
                if (newSolution.score > eliteArchive.get(i).score) {
                    eliteArchive.set(i, newSolution);
                }
                return;
            }
        }
        if (eliteArchive.size() < ARCHIVE_SIZE) {
            eliteArchive.add(newSolution);
        } else {
            eliteArchive.sort(null);
            if (newSolution.score > eliteArchive.get(eliteArchive.size() - 1).score) {
                eliteArchive.set(eliteArchive.size() - 1, newSolution);
            }
        }
    }

    private double[] crossover(double[] p1, double[] p2) {
        double[] child = new double[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            child[i] = random.nextBoolean() ? p1[i] : p2[i];
        }
        return child;
    }

    private void mutate(double[] individual) {
        for (int i = 0; i < DIMENSIONS; i++) {
            if (random.nextDouble() < GA_RESCUE_MUTATION_RATE) {
                individual[i] += random.nextGaussian() * 0.1;
                individual[i] = clamp(individual[i], 0, 1);
            }
        }
    }

    private double distance(double[] s1, double[] s2) {
        double sum = 0;
        for (int i = 0; i < DIMENSIONS; i++) {
            sum += (s1[i] - s2[i]) * (s1[i] - s2[i]);
        }
        return Math.sqrt(sum);
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
            int tmp = all[i];
            all[i] = all[j];
            all[j] = tmp;
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

    private double clamp(double v, double lo, double hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }
}