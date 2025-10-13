public class DataItem {
    int id;
    int size;
    String type;
    int frequency;
    long lastAccessTime;
    // int Priority; // --- 移除 Priority ---

    long arrivalTime;
    long processingTime; // 这是“基础”处理时间
    long deadline;

    // --- 权重现在是5个 (w1-w2, w4-w6) ---
    static double w1 = 0.2, w2 = 0.2, w4 = 0.4; // 缓存权重
    static double w5 = 0.5, w6 = 0.5;          // 调度权重

    public DataItem(int id, int size, String type, int frequency,
                    long arrivalTime, long processingTime, long absoluteDeadline) {
        this.id = id;
        this.size = size;
        this.type = type;
        this.frequency = frequency;
        this.lastAccessTime = arrivalTime;
        // this.Priority = userPriority; // --- 移除 ---
        this.arrivalTime = arrivalTime;
        this.processingTime = processingTime;
        this.deadline = absoluteDeadline;
    }

    public double getCacheScore(long currentTime) {
        double gap = currentTime - lastAccessTime;
        // --- 从评分函数中移除 Priority ---
        return w1 * frequency - w2 * gap - w4 * size;
    }

    public double getSchedulingScore(long currentTime) {
        double remainingTime = deadline - currentTime;
        if (remainingTime <= 0) {
            remainingTime = 1;
        }
        // --- 从评分函数中移除 Priority ---
        return w5 * (1000.0 / remainingTime) + w6 * processingTime;
    }

    public void updateAccess(long currentTime) {
        this.frequency++;
        this.lastAccessTime = currentTime;
    }

    public static void setWeights(double[] w) {
        // --- 权重数组长度现在是5 ---
        if (w.length != 5) throw new IllegalArgumentException("权重数组长度必须为5");
        w1 = w[0];
        w2 = w[1];
        w4 = w[2]; // 注意索引变化
        w5 = w[3];
        w6 = w[4];
    }
}