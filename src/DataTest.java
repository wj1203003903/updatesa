import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class DataTest {

    public static double score(double[] weights, DataItem[] dataSet, DataManager dm) {
        // 这个方法现在不进行任何重置操作
        dm.resetCurrentRunStats();
        DataItem.setWeights(weights);

        // 重新填充底层数据源，因为缓存可能在 resetCurrentRunStats 中被清空了
        for (DataItem item : dataSet) {
            dm.addDataToRemote(item, item.arrivalTime);
        }

        List<DataItem> futureTasks = new ArrayList<>(Arrays.asList(dataSet));
        futureTasks.sort(Comparator.comparingLong(item -> item.arrivalTime));

        List<DataItem> readyQueue = new ArrayList<>();
        int completedTasksCount = 0;
        int futureTaskPointer = 0;
        long currentTime = 0;

        if (!futureTasks.isEmpty()) {
            currentTime = futureTasks.get(0).arrivalTime;
        }

        while (completedTasksCount < dataSet.length) {
            while (futureTaskPointer < futureTasks.size() && futureTasks.get(futureTaskPointer).arrivalTime <= currentTime) {
                readyQueue.add(futureTasks.get(futureTaskPointer));
                futureTaskPointer++;
            }

            if (!readyQueue.isEmpty()) {
                final long decisionTime = currentTime;
                DataItem bestTask = readyQueue.stream()
                        .max(Comparator.comparingDouble(item -> item.getSchedulingScore(decisionTime)))
                        .orElse(null);

                readyQueue.remove(bestTask);
                long duration = dm.processAndGetDuration(bestTask, currentTime, readyQueue.size());
                currentTime += duration;
                completedTasksCount++;
            } else {
                if (futureTaskPointer < futureTasks.size()) {
                    currentTime = futureTasks.get(futureTaskPointer).arrivalTime;
                } else {
                    break;
                }
            }
        }
        return dm.getSystemScore();
    }
}