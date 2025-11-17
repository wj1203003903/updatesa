import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class DataTest {

    public static double score(double[] weights, DataItem[] dataSet, DataManager dm) {
        dm.reset();
        DataItem.setWeights(weights);

        // 步骤 1: 准备任务，按到达时间排序
        List<DataItem> futureTasks = new ArrayList<>(Arrays.asList(dataSet));
        futureTasks.sort(Comparator.comparingLong(item -> item.arrivalTime));

        List<DataItem> readyQueue = new ArrayList<>(); // 就绪队列
        int completedTasksCount = 0;
        int futureTaskPointer = 0;

        long currentTime = 0; // 模拟时钟

        if (!futureTasks.isEmpty()) {
            currentTime = futureTasks.get(0).arrivalTime;
        }

        // --- 主模拟循环 (已恢复为“尽力而为”模式) ---
        while (completedTasksCount < dataSet.length) {

            // 步骤 2: 将所有已到达的任务加入就绪队列
            while (futureTaskPointer < futureTasks.size() && futureTasks.get(futureTaskPointer).arrivalTime <= currentTime) {
                readyQueue.add(futureTasks.get(futureTaskPointer));
                futureTaskPointer++;
            }

            // 步骤 3: 如果就绪队列中有任务，则处理优先级最高的一个
            if (!readyQueue.isEmpty()) {
                // a. 在当前时间点，对就绪队列中的任务进行排序，选出最优任务
                final long decisionTime = currentTime;
                DataItem bestTask = readyQueue.stream()
                        .max(Comparator.comparingDouble(item -> item.getSchedulingScore(decisionTime)))
                        .orElse(null);

                // b. 从就绪队列中移除这个任务
                readyQueue.remove(bestTask);

                // c. 处理任务，并获取其持续时间（无论是否超时）
                long duration = dm.processAndGetDuration(bestTask, currentTime, readyQueue.size());

                // d. 做完一个任务，快进时钟
                currentTime += duration;
                completedTasksCount++;

            } else { // 如果就绪队列为空（处理器空闲），但还有未到达的任务
                if (futureTaskPointer < futureTasks.size()) {
                    // 直接将时钟快进到下一个任务到达的时刻
                    currentTime = futureTasks.get(futureTaskPointer).arrivalTime;
                } else {
                    // 所有任务都已处理完，可以安全退出
                    break;
                }
            }
        }
        return dm.getSystemScore();
    }
}