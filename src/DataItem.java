public class DataItem {
    int id;
    int size;
    String type;
    int frequency;
    long lastAccessTime;

    long arrivalTime;
    long deadline;

    // --- 权重现在是5个 (w1,w2,w4 用于缓存, w5,w_size 用于调度) ---
    static double w1 = 0.2, w2 = 0.2, w4_cache = 0.2; // 缓存权重
    static double w5_urgency = 0.2, w4_schedule = 0.2; // 调度权重

    public DataItem(int id, int size, String type, long arrivalTime, long absoluteDeadline) {
        this.id = id;
        this.size = size;
        this.type = type;
        this.frequency = 0;
        this.lastAccessTime = arrivalTime;
        this.arrivalTime = arrivalTime;
        this.deadline = absoluteDeadline;
    }

    public DataItem(int id) { this.id = id; }

    public double getCacheScore(long currentTime) {

        double gapInSeconds = (currentTime - lastAccessTime) / 2000.0;

        double sizeInKB = size / 1024.0;


        double scaledFrequency = Math.log(1 + frequency*10);

        return 10+w1 * scaledFrequency - w2 * gapInSeconds - w4_cache * sizeInKB;
    }

    public double getSchedulingScore(long currentTime) {
        double remainingTime = deadline - currentTime;
        if (remainingTime <= 0) {
            remainingTime = 1;
        }
        // --- 核心修改：调度评分现在由 紧急性 和 size 决定 ---
        // 紧急性越高分越高，size 越大分越低 (优先处理小任务)
        return w5_urgency * (1000.0 / remainingTime) - w4_schedule * size;
    }

    public void updateAccess(long currentTime) {
        this.frequency++;
        this.lastAccessTime = currentTime;
    }

    public static void setWeights(double[] w) {
        if (w.length != 5) throw new IllegalArgumentException("权重数组长度必须为5");
        w1 = w[0];
        w2 = w[1];
        w4_cache = w[2];    // size 用于缓存的权重
        w5_urgency = w[3];  // 紧急性用于调度的权重
        w4_schedule = w[4]; // size 用于调度的权重
    }
}