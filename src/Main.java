import java.util.Random;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        final int dataNum = 2000;

        // 容量单位是字节(B): 800KB for local, 1600KB for edge
        long localCapacity = 800L * 1024L;
        long edgeCapacity = 1600L * 1024L;
        DataManager baseDM = new DataManager(localCapacity, edgeCapacity);

        DataItem[] testData = new DataItem[dataNum];
        long baseTime = System.currentTimeMillis();

        System.out.println("正在生成模拟数据集...");
        for (int i = 0; i < dataNum; i++) {
            testData[i] = baseDM.generateDataItem(i, baseTime);
            // 假设数据在其到达时被添加到远端云
            baseDM.addDataToRemote(testData[i], testData[i].arrivalTime);
        }
        System.out.println("数据集生成完毕，共 " + dataNum + " 条数据。");

        SA sa = new SA(testData, baseDM);

        System.out.println("\n=============================================");
        System.out.println("=== 开始事件驱动模拟下的权重优化 ===");
        System.out.println("=============================================");

        double bestScore = sa.run();

        System.out.println("\nSA 算法找到的最优系统评分为: " + bestScore);
    }
}