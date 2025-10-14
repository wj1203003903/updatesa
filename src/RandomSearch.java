import java.util.Arrays;
import java.util.Random;

/**
 * 一个简单的随机搜索模型.
 * 它只生成一组随机权重，然后直接评估其分数.
 * 用于和任何优化算法进行对比，作为衡量优化算法价值的基准.
 */
public class RandomSearch { // 您可以将类名改回 Nomal
    private static final int DIMENSIONS = 5; // 权重维度 (已更正为5)

    private DataItem[] testData;
    private DataManager baseDM;
    private Random random;

    public RandomSearch(DataItem[] testData, DataManager baseDM) { // 相应修改构造函数名
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();
    }

    /**
     * 运行一次随机评估
     */
    public double run() {
        // 1. 生成一组随机权重
        double[] best = new double[DIMENSIONS];
        for (int j = 0; j < DIMENSIONS; j++) {
            best[j] = random.nextDouble(); // 在 [0.0, 1.0) 范围内随机
        }

        // 2. 使用这组原始权重来评估分数
        double bestScore = evaluate(best);

        // 3. 按照统一格式打印最终结果
        System.out.println("\n=== Single Random Attempt Finished (Baseline) ===");

        // 创建副本进行归一化，仅用于显示
        double[] normalizedBest = best.clone();
        baseDM.normalizeL2(normalizedBest);
        System.out.printf("Final Best Weights (Normalized) = %s\n", Arrays.toString(normalizedBest));

        // 使用原始权重 best 重新评估并打印统计数据
        evaluate(best);
        baseDM.printStats();

        return bestScore;
    }

    /**
     * 评估函数
     * @param w 权重向量
     * @return 系统得分
     */
    private double evaluate(double[] w) {
        return DataTest.score(w, testData, baseDM);
    }
}