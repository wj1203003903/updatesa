import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class UpdateSA {
    // ... (内部类 EliteSolution 保持不变)
    private static class EliteSolution implements Comparable<EliteSolution> { final double[] solution; final double score; EliteSolution(double[] solution, double score) { this.solution = solution; this.score = score; } @Override public int compareTo(EliteSolution other) { return Double.compare(other.score, this.score); } }

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

    public double run(PrintStream out, double baselineScore) {
        out.println("--- Pre-seeding Elite Archive to ensure GA-Rescue readiness... ---");
        while(eliteArchive.size() <= ARCHIVE_SIZE / 3) {
            evaluate(randomWeights()); // evaluate内部会调用saveBestStats
        }
        out.println("--- Pre-seeding finished. Elite archive size: " + eliteArchive.size() + " ---");

        double[] current;
        double currentScore;
        if (!eliteArchive.isEmpty()) {
            eliteArchive.sort(null);
            current = eliteArchive.get(0).solution.clone();
            currentScore = eliteArchive.get(0).score;
        } else {
            current = randomWeights();
            currentScore = evaluate(current);
        }

        double temperature = INIT_TEMP;
        int stagnationCount = 0;
        int generation = 0;

        while (temperature > MIN_TEMP) {
            generation++;
            double bestScoreAtTempStart = this.bestScore;

            for (int it = 0; it < ITERATIONS_PER_TEMP; it++) {
                double[] neighbor = generateNeighborSA_Stateless(current, temperature);
                evaluate(neighbor); // evaluate内部会调用saveBestStats

                // acceptanceProbability现在只用于决定是否移动，不再需要重新评估
                if (acceptanceProbability(currentScore, evaluate(neighbor), temperature) > random.nextDouble()) {
                    current = neighbor;
                    currentScore = evaluate(current);
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
                current = eliteArchive.get(newStartIndex).solution.clone();
                currentScore = eliteArchive.get(newStartIndex).score;
                temperature *= RESTART_TEMP_INCREASE_FACTOR;
                if (temperature > INIT_TEMP) temperature = INIT_TEMP;
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

    private double evaluate(double[] weights) {
        double score = DataTest.score(weights, testData, baseDM);
        if (score > this.bestScore) {
            this.bestScore = score;
            this.bestSolution = weights.clone();
            updateArchive(new EliteSolution(this.bestSolution.clone(), this.bestScore));
            // 发现新高分，保存快照
            baseDM.saveBestStats();
        }
        return score;
    }
    // ... (所有其他辅助方法保持不变)
    private double[] randomWeights() { double[] w = new double[DIMENSIONS]; for (int i = 0; i < DIMENSIONS; i++) w[i] = random.nextDouble(); return w; }
    private double acceptanceProbability(double cs, double ns, double t) { if (ns > cs) return 1.0; return Math.exp((ns - cs) / t); }
    private void runGARescue() { List<EliteSolution> pop = new ArrayList<>(eliteArchive); for (int g = 0; g < GA_RESCUE_GENERATIONS; g++) { List<EliteSolution> newPop = new ArrayList<>(); if (!pop.isEmpty()) { pop.sort(null); newPop.add(pop.get(0)); } while (newPop.size() < ARCHIVE_SIZE) { EliteSolution p1 = tournamentSelection(pop); EliteSolution p2 = tournamentSelection(pop); double[] child = crossover(p1.solution, p2.solution); mutate(child); newPop.add(new EliteSolution(child, evaluate(child))); } pop = newPop; } eliteArchive.sort(null); while (eliteArchive.size() > ARCHIVE_SIZE / 3) eliteArchive.remove(eliteArchive.size() - 1); for (EliteSolution s : pop) updateArchive(s); }
    private EliteSolution tournamentSelection(List<EliteSolution> p) { EliteSolution b = null; for (int i = 0; i < GA_RESCUE_TOURNAMENT_SIZE; i++) { EliteSolution ind = p.get(random.nextInt(p.size())); if (b == null || ind.score > b.score) b = ind; } return b; }
    private double[] crossover(double[] p1, double[] p2) { double[] c = new double[DIMENSIONS]; for(int i=0;i<DIMENSIONS;i++) c[i] = random.nextBoolean()?p1[i]:p2[i]; return c; }
    private void mutate(double[] ind) { for(int i=0;i<DIMENSIONS;i++) if(random.nextDouble()<GA_RESCUE_MUTATION_RATE) { ind[i]+=random.nextGaussian()*0.1; ind[i]=Math.max(0,Math.min(1,ind[i])); } }
    private void updateArchive(EliteSolution s) { for (int i=0;i<eliteArchive.size();i++) { if (distance(s.solution, eliteArchive.get(i).solution) < DIVERSITY_THRESHOLD) { if (s.score > eliteArchive.get(i).score) eliteArchive.set(i, s); return; } } if (eliteArchive.size() < ARCHIVE_SIZE) eliteArchive.add(s); else { eliteArchive.sort(null); if (s.score > eliteArchive.get(eliteArchive.size()-1).score) eliteArchive.set(eliteArchive.size()-1, s); } }
    private double distance(double[] s1, double[] s2) { double sum=0; for(int i=0;i<DIMENSIONS;i++) sum+=(s1[i]-s2[i])*(s1[i]-s2[i]); return Math.sqrt(sum); }
    private double[] generateNeighborSA_Stateless(double[] c, double t) { double[] n = c.clone(); int k = 1+(int)Math.round((DIMENSIONS-1)*Math.min(1.0, t/INIT_TEMP)); int[] ids = randomSampleIndices(k); double step = 0.02+(0.4-0.02)*Math.min(1.0, t/INIT_TEMP); for(int id : ids) { n[id]+=random.nextGaussian()*step; n[id]=Math.max(0,Math.min(1,n[id])); } return n; }
    private int[] randomSampleIndices(int k) { int[] all = new int[DIMENSIONS]; for(int i=0;i<DIMENSIONS;i++) all[i]=i; for(int i=0;i<k;i++) { int j=i+random.nextInt(DIMENSIONS-i); int tmp=all[i]; all[i]=all[j]; all[j]=tmp; } return Arrays.copyOf(all, k); }
}