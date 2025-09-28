import java.util.Arrays;
import java.util.Random;

/**
 * 一个最基础的基线模型.
 * 它只生成一组随机权重，计算其得分，然后直接输出结果.
 * 这用于衡量任何优化算法（包括多次随机搜索和PSO）所带来的最基本的价值.
 */
public class Nomal {
    private static final int DIMENSIONS = 4;        // 维度数量（权重数量）

    private DataItem[] testData;
    private DataManager baseDM;
    private Random random;

    // 构造函数 (与其他版本保持一致，方便替换)
    public Nomal(DataItem[] testData, DataManager baseDM) {
        this.testData = testData;
        this.baseDM = baseDM;
        this.random = new Random();
    }

    /**
     * 运行单次随机尝试
     */
    public double run() {
        // 1. 生成一组随机权重
        double[] best = new double[DIMENSIONS];
        for (int j = 0; j < DIMENSIONS; j++) {
            best[j] = random.nextDouble(); // 在 [0.0, 1.0) 区间生成随机数
        }

        // 2. 评估这组权重的得分
        double bestScore = evaluate(best);

        // 3. 归一化权重并打印结果 (与PSO版本格式保持一致)
        baseDM.normalizeL2(best);
        System.out.println("=== Single Random Attempt Finished ===");
        System.out.printf("Best Weights = %s\n", Arrays.toString(best));
        baseDM.printStats();
        return bestScore;
    }

    /**
     * 评估函数 (与其他版本完全相同)
     * @param w 权重数组
     * @return 评估得分
     */
    private double evaluate(double[] w) {
        return DataTest.score(w, testData, baseDM);
    }
}