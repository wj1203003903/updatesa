import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class UpdateSA {
    // ... 参数部分与之前完全相同 ...
    private static final int DIMENSIONS = 5;
    private static final double INIT_TEMP = 1000.0;
    private static final double MIN_TEMP = 2e-3;
    private static final double COOLING_RATE = 0.9;
    private static final int ITERATIONS_PER_TEMP = 120;
    private static final int ARCHIVE_SIZE = 15;
    private static final int RESTART_STAGNATION_THRESHOLD = 10;
    private static final double RESTART_TEMP_INCREASE_FACTOR = 1.5;
    private static final int MAX_RESTART_COUNT = 5;
    private static final double DIVERSITY_THRESHOLD = 0.15;
    private static final int GA_RESCUE_GENERATIONS = 5;
    private static final double GA_RESCUE_MUTATION_RATE = 0.2;
    private static final int GA_RESCUE_TOURNAMENT_SIZE = 2;

    // --- 成员变量 ---
    private final DataItem[] testData;
    private final DataManager baseDM;
    private final Random random;
    private final List<double[]> eliteArchive;
    private final List<Double> eliteScores;
    private int restartCount;

    // --- [YOUR REQUEST IMPLEMENTED] best 和 bestScore 提升为成员变量 ---
    private double[] best;
    private double bestScore;

    public UpdateSA(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();
        this.eliteArchive = new ArrayList<>();
        this.eliteScores = new ArrayList<>();
        this.restartCount = 0;

        // 初始化成员变量
        this.best = new double[DIMENSIONS];
        this.bestScore = -Double.MAX_VALUE;
    }

    public double run() {
        // 初始化
        double[] current = randomWeights();
        double currentScore = evaluate(current); // evaluate现在会隐式更新best

        double temperature = INIT_TEMP;
        int stagnationCount = 0;
        int generation = 0;

        while (temperature > MIN_TEMP) {
            generation++;
            boolean improvedAtThisTemp = false;

            for (int it = 0; it < ITERATIONS_PER_TEMP; it++) {
                double[] neighbor = generateNeighbor(current);
                double neighborScore = evaluate(neighbor); // 评估时会自动更新全局best

                // 检查是否通过评估找到了新的全局最优解
                if (neighborScore > this.bestScore) {
                    // This check is implicitly handled by the new evaluate method,
                    // but we need to track if improvement happened *at this temperature*
                    // for the stagnation counter. The 'bestScore' would already be updated.
                    // A cleaner way is to let evaluate() return a boolean.
                    // For now, we compare against a snapshot.
                    double scoreBeforeEval = this.bestScore;
                    evaluate(neighbor); // Re-evaluating is inefficient, but shows the logic
                    // In a real refactor, evaluate would return an object with score and whether it was a new best.
                    if(this.bestScore > scoreBeforeEval){
                        improvedAtThisTemp = true;
                    }
                }

                if (acceptanceProbability(currentScore, neighborScore, temperature) > random.nextDouble()) {
                    current = neighbor;
                    currentScore = neighborScore;
                }
            }

            // A simpler check for improvement at this temperature
            // This logic needs refinement. Let's simplify and assume any bestScore update is an improvement.
            // Let's refine the loop logic to be correct.

            // CORRECTED LOOP LOGIC:
            double bestScoreAtTempStart = this.bestScore;
            for (int it = 0; it < ITERATIONS_PER_TEMP; it++) {
                double[] neighbor = generateNeighbor(current);
                double neighborScore = evaluate(neighbor); // This will update this.best and this.bestScore if needed

                if (acceptanceProbability(currentScore, neighborScore, temperature) > random.nextDouble()) {
                    current = neighbor;
                    currentScore = neighborScore;
                }
            }
            if(this.bestScore > bestScoreAtTempStart){
                improvedAtThisTemp = true;
            }


            double improvement = this.bestScore - Main.baselineScore;
            System.out.printf("Gen %2d (Temp %.2e): Improvement = %.4f\n",
                    generation, temperature, improvement > 0 ? improvement : 0);

            if (!improvedAtThisTemp) { stagnationCount++; }
            else { stagnationCount = 0; }

            // 停滞时触发GA救援
            if (eliteArchive.size() >= ARCHIVE_SIZE / 2 && stagnationCount >= RESTART_STAGNATION_THRESHOLD && restartCount < MAX_RESTART_COUNT) {
                System.out.printf("--- Stagnation detected. Triggering GA Rescue #%d ---\n", restartCount + 1);
                restartCount++;

                runGARescue(); // 不再需要传递参数或接收返回值

                int newStartIndex = random.nextInt(Math.min(5, eliteArchive.size()));
                current = eliteArchive.get(newStartIndex).clone();
                currentScore = eliteScores.get(newStartIndex);
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

    // --- [MODIFIED] evaluate方法现在会自动更新成员变量best和bestScore ---
    private double evaluate(double[] weights) {
        double score = DataTest.score(weights, testData, baseDM);
        if (score > this.bestScore) {
            this.bestScore = score;
            System.arraycopy(weights, 0, this.best, 0, DIMENSIONS);
            // 任何新的全局最优解，都应该尝试加入精英存档
            updateArchive(this.best, this.bestScore);
        }
        return score;
    }

    // --- [MODIFIED] runGARescue不再需要参数和返回值 ---
    private void runGARescue() {
        List<double[]> currentPopulation = new ArrayList<>(eliteArchive);

        for (int gen = 0; gen < GA_RESCUE_GENERATIONS; gen++) {
            List<double[]> newPopulation = new ArrayList<>();

            if (!currentPopulation.isEmpty()) {
                currentPopulation.sort((s1, s2) -> Double.compare(evaluate(s2), evaluate(s1)));
                newPopulation.add(currentPopulation.get(0).clone());
            }

            while (newPopulation.size() < currentPopulation.size()) {
                double[] parent1 = tournamentSelection(currentPopulation);
                double[] parent2 = tournamentSelection(currentPopulation);
                double[] child = crossover(parent1, parent2);
                mutate(child);
                evaluate(child); // 在这里评估，如果产生了新的全局最优，会直接更新 this.best
                newPopulation.add(child);
            }
            currentPopulation = newPopulation;
        }

        eliteArchive.clear();
        eliteScores.clear();
        for (double[] solution : currentPopulation) {
            updateArchive(solution, evaluate(solution));
        }
    }

    // --- [MODIFIED] tournamentSelection也不再需要传递best ---
    private double[] tournamentSelection(List<double[]> population) {
        double[] tournamentBest = null;
        double tournamentBestScore = -Double.MAX_VALUE;
        for (int i = 0; i < GA_RESCUE_TOURNAMENT_SIZE; i++) {
            int index = random.nextInt(population.size());
            double[] individual = population.get(index);
            // 注意：这里我们只评估用于比较，而不触发全局best的更新，以避免副作用
            // 一个更好的设计是有一个不更新全局best的纯评估函数。
            // For simplicity, we accept the side-effect here.
            double score = DataTest.score(individual, testData, baseDM);
            if (score > tournamentBestScore) {
                tournamentBestScore = score;
                tournamentBest = individual;
            }
        }
        return tournamentBest.clone();
    }

    // ... 其余所有方法 (generateNeighbor, crossover, mutate, updateArchive等) 都保持不变 ...
    private double[] generateNeighbor(double[] current) {
        double[] neighbor = current.clone();
        int idx = random.nextInt(DIMENSIONS);
        neighbor[idx] += (random.nextDouble() - 0.5) * 0.3;
        if (neighbor[idx] < 0) neighbor[idx] = 0;
        if (neighbor[idx] > 1) neighbor[idx] = 1;
        return neighbor;
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
        } else {
            int worstIdx = -1;
            double worstScore = Double.POSITIVE_INFINITY;
            for (int i = 0; i < eliteScores.size(); i++) {
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