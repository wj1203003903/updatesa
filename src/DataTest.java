import java.util.*;

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

        // --- 使用单一时钟变量 ---
        long currentTime = 0; // 模拟时钟，代表当前处理器完成上个任务的时刻

        if (!futureTasks.isEmpty()) {
            // 时钟从第一个任务的到达时间开始
            currentTime = futureTasks.get(0).arrivalTime;
        }

        // --- 主模拟循环 ---
        while (completedTasksCount < dataSet.length) {

            // 步骤 2: 根据“当前时间”，将所有已到达的任务加入就绪队列
            while (futureTaskPointer < futureTasks.size() && futureTasks.get(futureTaskPointer).arrivalTime <= currentTime) {
                readyQueue.add(futureTasks.get(futureTaskPointer));
                futureTaskPointer++;
            }

            // 步骤 3: 如果就绪队列中有任务，则处理优先级最高的一个
            if (!readyQueue.isEmpty()) {
                // a. 在当前时间点，对就绪队列中的任务进行排序，选出最优任务
                final long decisionTime = currentTime; // 决策时刻就是当前时钟
                DataItem bestTask = readyQueue.stream()
                        .max(Comparator.comparingDouble(item -> item.getSchedulingScore(decisionTime)))
                        .orElse(null);
                readyQueue.remove(bestTask);

                // b. 处理任务，并获取其持续时间
                // 任务的开始时间就是当前时钟 `currentTime`
                long duration = dm.processAndGetDuration(bestTask, currentTime, readyQueue.size());

                // c. 【核心】做完一个任务，计算结束时间，并直接快进时钟
                currentTime += duration;

                completedTasksCount++;

            } else { // 如果就Git Branch - M Main是什么？ 就绪队列为空（处理器空闲），但还有未到达的任务
                if (futureTaskPointer < futureTasks.size()) {
                    // 直接将时钟快进到下一个任务到达的时刻，跳过空闲时间
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