import java.util.*;

public class DataTest {

    public static double score(double[] weights, DataItem[] dataSet, DataManager dm) {
        dm.reset();
        DataItem.setWeights(weights);

        List<DataItem> futureTasks = new ArrayList<>(Arrays.asList(dataSet));
        futureTasks.sort(Comparator.comparingLong(item -> item.arrivalTime));

        List<DataItem> readyQueue = new ArrayList<>();
        int completedTasksCount = 0;
        long simulatedTime = 0;
        int futureTaskPointer = 0;

        if (!futureTasks.isEmpty()) {
            simulatedTime = futureTasks.get(0).arrivalTime;
        }

        while (completedTasksCount < dataSet.length) {

            while (futureTaskPointer < futureTasks.size() && futureTasks.get(futureTaskPointer).arrivalTime <= simulatedTime) {
                readyQueue.add(futureTasks.get(futureTaskPointer));
                futureTaskPointer++;
            }

            if (!readyQueue.isEmpty()) {
                final long currentTime = simulatedTime;
                DataItem bestTask = readyQueue.stream()
                        .max(Comparator.comparingDouble(item -> item.getSchedulingScore(currentTime)))
                        .orElse(null);

                readyQueue.remove(bestTask);

                long duration = dm.processAndGetDuration(bestTask, simulatedTime);

                simulatedTime += duration;
                completedTasksCount++;

            } else {
                if (futureTaskPointer < futureTasks.size()) {
                    simulatedTime = futureTasks.get(futureTaskPointer).arrivalTime;
                } else {
                    break;
                }
            }
        }
        return dm.getSystemScore();
    }
}