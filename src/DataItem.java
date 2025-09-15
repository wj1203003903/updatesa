class DataItem {
    int id;
    int size;
    String type;
    int frequency;
    long lastAccessTime;
    int Priority;

    // 动态参数：可被外部设置
    static double w1 = 0.2;
    static double w2 = 0.2;
    static double w3 = 0.2;
    static double w4 = 0.2;

    public DataItem(int id, int size, String type, int frequency, int userPriority) {
        this.id = id;
        this.size = size;
        this.type = type;
        this.frequency = frequency;
        this.lastAccessTime = System.currentTimeMillis();
        this.Priority = userPriority;
    }

    public double score() {
        long now = System.currentTimeMillis();
        double gap = now - lastAccessTime;
        return w1 * frequency - w2 * gap + w3 * Priority - w4 * size;
    }

    public void updateAccess() {
        this.frequency++;
        this.lastAccessTime = System.currentTimeMillis();
    }

    // 添加设置权重的方法
    public static void setWeights(double w[]) {
        w1 = w[0];
        w2 = w[1];
        w3 = w[2];
        w4 = w[3];
    }
}
