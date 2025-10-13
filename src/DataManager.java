import java.util.*;

class DataManager {
    LocalCache local;
    CloudCache cloud;
    RemoteCloud remote;
    private final double LOCAL_BUS_SPEED_BPS = 10 * 1e9;
    private final Channel cloudChannel;
    private final Channel remoteChannel;
    private double totalDelaySeconds = 0.0;
    int totalAccessed = 0, localHits = 0, cloudHits = 0;
    int localAccesses = 0, cloudAccesses = 0, remoteAccesses = 0;
    int completedTasks = 0, totalTasks = 0;
    private Map<Integer, DataItem> idToDataItemMap = new HashMap<>();

    public DataManager(long localCap, long edgeCap) {
        local = new LocalCache(localCap);
        cloud = new CloudCache(edgeCap);
        remote = new RemoteCloud();
        local.bindLowerLevel(cloud);
        cloud.bindLowerLevel(remote);
        this.cloudChannel = new Channel(20 * 1e6, 0.030, 100.0, 40.0);
        this.remoteChannel = new Channel(100 * 1e6, 0.150, 50.0, 30.0);
    }

    public DataItem generateDataItem(int id, long baseTime) {
        if (idToDataItemMap.containsKey(id)) {
            return idToDataItemMap.get(id);
        }
        Random rand = new Random(id);
        int size = (rand.nextInt(50) + 10) * 1024;
        String type = "type" + rand.nextInt(5);
        int frequency = rand.nextInt(10) + 1;
        // int priority = rand.nextInt(3) + 1; // --- �Ƴ� ---

        long arrivalTime = baseTime + rand.nextInt(1000);
        long processingTime = rand.nextInt(100) + 50;
        long bufferTime = rand.nextInt(500) + 200;
        long absoluteDeadline = arrivalTime + processingTime + bufferTime;

        // --- ʹ���µĹ��캯�� ---
        DataItem item = new DataItem(id, size, type, frequency, arrivalTime, processingTime, absoluteDeadline);
        idToDataItemMap.put(id, item);
        return item;
    }

    // access ����������Ҫ���ݵĲ�������
    private double access(String type, int id, long currentTime) {
        totalAccessed++;
        double delaySeconds;
        localAccesses++;
        // --- get ����������Ҫ priority ---
        DataItem item = local.get(type, 0, id, currentTime); // priority���Դ�����ֵ����Ϊ����ʹ��
        if (item != null) { localHits++; delaySeconds = (item.size * 8.0) / LOCAL_BUS_SPEED_BPS; return delaySeconds; }
        cloudAccesses++;
        item = cloud.get(type, 0, id, currentTime);
        if (item != null) { cloudHits++; delaySeconds = cloudChannel.getTotalDelay(item.size); local.put(item, currentTime); return delaySeconds; }
        remoteAccesses++;
        item = remote.get(type, 0, id, currentTime);
        if (item != null) { delaySeconds = remoteChannel.getTotalDelay(item.size); cloud.put(item, currentTime); local.put(item, currentTime); return delaySeconds; }
        return Double.POSITIVE_INFINITY;
    }

    // --- �����޸ģ�processAndGetDuration ����һ�� readyQueueSize ���� ---
    public long processAndGetDuration(DataItem item, long currentTime, int readyQueueSize) {
        totalTasks++;
        double accessDelaySeconds = access(item.type, item.id, currentTime);

        // --- ���붯̬ processingTime ---
        // ģ�⸺�أ���������Խ����CPUԽ��æ������ʱ��Խ��
        // �������ӣ�ÿ����һ���Ŷ����񣬴���ʱ������5%
        double loadFactor = 1.0 + (readyQueueSize * 0.05);
        long actualProcessingTime = (long)(item.processingTime * loadFactor);

        double processingDelaySeconds = actualProcessingTime / 1000.0;

        // --- ������������ access �� processing ���ӳٶ��ӽ�ȥ ---
        double totalTaskDurationSeconds = accessDelaySeconds + processingDelaySeconds;
        this.totalDelaySeconds += totalTaskDurationSeconds;

        long totalTaskDurationMillis = (long)(totalTaskDurationSeconds * 1000);
        long taskCompletionTime = currentTime + totalTaskDurationMillis;

        if (taskCompletionTime <= item.deadline) {
            completedTasks++;
        }

        return totalTaskDurationMillis;
    }

    public void addDataToRemote(DataItem item, long currentTime) {
        remote.put(item, currentTime);
    }

    // �������� (getSystemScore, normalizeL2, reset, printStats) ���ֲ���
    public double getSystemScore() {
        if (totalAccessed == 0 || totalTasks == 0) return 0;
        double localHitRate = (double) localHits / Math.max(1, localAccesses);
        double cloudHitRate = (double) cloudHits / Math.max(1, cloudAccesses);
        double completionRate = (double) completedTasks / totalTasks;
        double averageDelay = totalDelaySeconds / totalTasks;
        double w1 = 2.0, w2 = 1.0, w3 = 5.0;
        return (w1 * localHitRate + w2 * cloudHitRate + w3 * completionRate) / (1.0 + averageDelay * 10);
    }
    public void normalizeL2(double[] vector) { double sumOfSquares = 0.0; for (double value : vector) { sumOfSquares += value * value; } if (sumOfSquares == 0) return; double l2Norm = Math.sqrt(sumOfSquares); for (int i = 0; i < vector.length; i++) { vector[i] = vector[i] / l2Norm; } }
    public void reset() { local.clear(); cloud.clear(); totalAccessed = 0; localHits = 0; cloudHits = 0; localAccesses = 0; cloudAccesses = 0; remoteAccesses = 0; completedTasks = 0; totalTasks = 0; totalDelaySeconds = 0.0; }
    public void printStats() { if (totalTasks == 0) { System.out.println("û�����񱻴����޷�����ͳ�����ݡ�"); return; } double localHitRate = (double) localHits / Math.max(1, localAccesses); double cloudHitRate = (double) cloudHits / Math.max(1, cloudAccesses); double completionRate = (double) completedTasks / totalTasks; System.out.println("==== ϵͳ����ͳ�� ===="); System.out.println("�ܴ���������: " + totalTasks); System.out.printf("���������: %.3f (%d / %d)\n", completionRate, completedTasks, totalTasks); System.out.printf("���ػ���������: %.3f\n", localHitRate); System.out.printf("��Ե��������: %.3f\n", cloudHitRate); System.out.printf("�ܼ��ӳ�: %.3f s\n", totalDelaySeconds); System.out.printf("ƽ�������ӳ�: %.3f ms\n", (totalDelaySeconds / totalTasks) * 1000); }
}