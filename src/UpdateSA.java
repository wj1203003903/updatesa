import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class UpdateSA {
    private static class EliteSolution implements Comparable<EliteSolution> {
        final double[] solution;
        final double score;

        EliteSolution(double[] solution, double score) {
            this.solution = solution;
            this.score = score;
        }

        @Override
        public int compareTo(EliteSolution other) {
            return Double.compare(other.score, this.score);
        }
    }

    private static final int DIMENSIONS = 5;
    private static final double INIT_TEMP = 500.0;
    private static final double MIN_TEMP = 1e-3;
    private static final double COOLING_RATE = 0.9;
    private static final int ITERATIONS_PER_TEMP = 100;
    private static final int ARCHIVE_SIZE = 15;
    private static final int RESTART_STAGNATION_THRESHOLD = 25;
    private static final double RESTART_TEMP_INCREASE_FACTOR = 1.5;
    private static final int MAX_RESTART_COUNT = 5;
    private static final double DIVERSITY_THRESHOLD = 0.15;
    private static final int GA_RESCUE_GENERATIONS = 10;
    private static final double GA_RESCUE_MUTATION_RATE = 0.2;
    private static final int GA_RESCUE_TOURNAMENT_SIZE = 3;

    private final DataItem[] testData;
    private final DataManager baseDM;
    private final Random random;
    private final List<EliteSolution> eliteArchive;
    private int restartCount;
    private double[] bestSolution;
    private double bestScore;

    public UpdateSA(DataItem[] testData, DataManager baseDM, long seed) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random(seed);
        this.eliteArchive = new ArrayList<>();
        this.restartCount = 0;
        this.bestSolution = new double[DIMENSIONS];
        this.bestScore = -Double.MAX_VALUE;
    }

    private double evaluate(double[] weights) {
        baseDM.resetCurrentRunStats();
        double score = DataTest.score(weights, testData, baseDM);
        if (score > this.bestScore) {
            this.bestScore = score;
            this.bestSolution = weights.clone();
            updateArchive(new EliteSolution(this.bestSolution.clone(), this.bestScore));
            baseDM.saveBestStats();
        }
        return score;
    }

    public double run(PrintStream out, double baselineScore) {
        out.println("--- Pre-seeding Elite Archive to ensure GA-Rescue readiness... ---");
        while (eliteArchive.size() <= ARCHIVE_SIZE / 3) {
            evaluate(randomWeights());
        }
        out.println("--- Pre-seeding finished. Elite archive size: " + eliteArchive.size() + " ---");

        double[] currentSolution;
        double currentScore;
        if (!eliteArchive.isEmpty()) {
            eliteArchive.sort(null);
            currentSolution = eliteArchive.get(0).solution.clone();
            currentScore = eliteArchive.get(0).score;
        } else {
            currentSolution = randomWeights();
            currentScore = evaluate(currentSolution);
        }

        double temperature = INIT_TEMP;
        int stagnationCount = 0;
        int generation = 0;

        while (temperature > MIN_TEMP) {
            generation++;
            double bestScoreAtTempStart = this.bestScore;

            for (int it = 0; it < ITERATIONS_PER_TEMP; it++) {
                double[] neighborSolution = generateNeighborSA_Stateless(currentSolution, temperature);
                double neighborScore = evaluate(neighborSolution);

                if (acceptanceProbability(currentScore, neighborScore, temperature) > random.nextDouble()) {
                    currentSolution = neighborSolution;
                    currentScore = neighborScore;
                }
            }

            if (this.bestScore > bestScoreAtTempStart) {
                stagnationCount = 0;
            } else {
                stagnationCount++;
            }

            double improvement = this.bestScore - baselineScore;
            out.printf("Gen %2d (Temp %.2e): Improvement = %.4f\n",
                    generation, temperature, Math.max(0, improvement));

            if (eliteArchive.size() >= ARCHIVE_SIZE / 3 && stagnationCount >= RESTART_STAGNATION_THRESHOLD && restartCount < MAX_RESTART_COUNT) {
                out.printf("--- Stagnation detected. Triggering GA Rescue #%d ---\n", restartCount + 1);
                restartCount++;
                runGARescue();
                int newStartIndex = random.nextInt(Math.min(5, eliteArchive.size()));
                currentSolution = eliteArchive.get(newStartIndex).solution.clone();
                currentScore = eliteArchive.get(newStartIndex).score;
                temperature *= RESTART_TEMP_INCREASE_FACTOR;
                if (temperature > INIT_TEMP) {
                    temperature = INIT_TEMP;
                }
                stagnationCount = 0;
                continue;
            }
            temperature *= COOLING_RATE;
        }

        out.println("\n=== SA with GA-Rescue Finished ===");
        double[] finalNormalizedBest = this.bestSolution.clone();
        baseDM.normalizeL2(finalNormalizedBest);
        out.printf("Final Best Weights (Normalized) = %s\n", Arrays.toString(finalNormalizedBest));
        baseDM.printStats(out);
        return this.bestScore;
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

    private void runGARescue() {
        List<EliteSolution> currentPopulation = new ArrayList<>(eliteArchive);
        for (int gen = 0; gen < GA_RESCUE_GENERATIONS; gen++) {
            List<EliteSolution> newPopulation = new ArrayList<>();
            if (!currentPopulation.isEmpty()) {
                currentPopulation.sort(null);
                newPopulation.add(currentPopulation.get(0));
            }
            while (newPopulation.size() < ARCHIVE_SIZE) {
                EliteSolution p1 = tournamentSelection(currentPopulation);
                EliteSolution p2 = tournamentSelection(currentPopulation);
                double[] child = crossover(p1.solution, p2.solution);
                mutate(child);
                newPopulation.add(new EliteSolution(child, evaluate(child)));
            }
            currentPopulation = newPopulation;
        }
        eliteArchive.sort(null);
        while (eliteArchive.size() > ARCHIVE_SIZE / 3) {
            eliteArchive.remove(eliteArchive.size() - 1);
        }
        for (EliteSolution solution : currentPopulation) {
            updateArchive(solution);
        }
    }

    private EliteSolution tournamentSelection(List<EliteSolution> population) {
        EliteSolution best = null;
        for (int i = 0; i < GA_RESCUE_TOURNAMENT_SIZE; i++) {
            EliteSolution ind = population.get(random.nextInt(population.size()));
            if (best == null || ind.score > best.score) {
                best = ind;
            }
        }
        return best;
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
                individual[i] = Math.max(0, Math.min(1, individual[i]));
            }
        }
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

    private double distance(double[] s1, double[] s2) {
        double sum = 0;
        for (int i = 0; i < DIMENSIONS; i++) {
            sum += (s1[i] - s2[i]) * (s1[i] - s2[i]);
        }
        return Math.sqrt(sum);
    }

    private double[] generateNeighborSA_Stateless(double[] current, double temperature) {
        double[] neighbor = current.clone();
        int k = 1 + (int) Math.round((DIMENSIONS - 1) * Math.min(1.0, temperature / INIT_TEMP));
        int[] indices = randomSampleIndices(k);
        double step = 0.02 + (0.4 - 0.02) * Math.min(1.0, temperature / INIT_TEMP);
        for (int idx : indices) {
            neighbor[idx] += random.nextGaussian() * step;
            neighbor[idx] = Math.max(0, Math.min(1, neighbor[idx]));
        }
        return neighbor;
    }

    private int[] randomSampleIndices(int k) {
        int[] all = new int[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            all[i] = i;
        }
        for (int i = 0; i < k; i++) {
            int j = i + random.nextInt(DIMENSIONS - i);
            int tmp = all[i];
            all[i] = all[j];
            all[j] = tmp;
        }
        return Arrays.copyOf(all, k);
    }
}