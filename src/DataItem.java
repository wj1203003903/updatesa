public class DataItem {
    int id;
    int size;
    String type;
    int frequency;
    long lastAccessTime;
    int Priority;

    // --- 定义为绝对时间戳 ---
    long arrivalTime;      // 任务到达的绝对时刻
    long processingTime;   // 任务本身需要的处理时间 (毫秒)
    long deadline;         // [定义] 任务必须完成的绝对时刻点 (时间戳)

    // 缓存权重
    static double w1 = 0.2, w2 = 0.2, w3 = 0.2, w4 = 0.4;
    // 调度权重
    static double w5 = 0.5, w6 = 0.5;

    public DataItem(int id, int size, String type, int frequency, int userPriority,
                    long arrivalTime, long processingTime, long absoluteDeadline) {
        this.id = id;
        this.size = size;
        this.type = type;
        this.frequency = frequency;
        this.lastAccessTime = arrivalTime;
        this.Priority = userPriority;
        this.arrivalTime = arrivalTime;
        this.processingTime = processingTime;
        this.deadline = absoluteDeadline; // 存储绝对截止时间戳
    }

    public double getCacheScore(long currentTime) {
        double gap = currentTime - lastAccessTime;
        return w1 * frequency - w2 * gap + w3 * Priority - w4 * size;
    }

    public double getSchedulingScore(long currentTime) {
        // 临时计算剩余时间，用于评估紧急程度
        double remainingTime = deadline - currentTime;

        if (remainingTime <= 0) {
            remainingTime = 1; // 任务已超时或即将超时，给予最高优先级
        }
        return w3 * Priority + w5 * (1000.0 / remainingTime) + w6 * processingTime;
    }

    public void updateAccess(long currentTime) {
        this.frequency++;
        this.lastAccessTime = currentTime;
    }

    public static void setWeights(double[] w) {
        if (w.length != 6) throw new IllegalArgumentException("权重数组长度必须为6");
        w1 = w[0]; w2 = w[1]; w3 = w[2]; w4 = w[3]; w5 = w[4]; w6 = w[5];
    }
}