public class DataItem {
    int id;
    int size;
    String type;
    int frequency;
    long lastAccessTime;
    // int Priority; // --- �Ƴ� Priority ---

    long arrivalTime;
    long processingTime; // ���ǡ�����������ʱ��
    long deadline;

    // --- Ȩ��������5�� (w1-w2, w4-w6) ---
    static double w1 = 0.2, w2 = 0.2, w4 = 0.4; // ����Ȩ��
    static double w5 = 0.5, w6 = 0.5;          // ����Ȩ��

    public DataItem(int id, int size, String type, int frequency,
                    long arrivalTime, long processingTime, long absoluteDeadline) {
        this.id = id;
        this.size = size;
        this.type = type;
        this.frequency = frequency;
        this.lastAccessTime = arrivalTime;
        // this.Priority = userPriority; // --- �Ƴ� ---
        this.arrivalTime = arrivalTime;
        this.processingTime = processingTime;
        this.deadline = absoluteDeadline;
    }

    public double getCacheScore(long currentTime) {
        double gap = currentTime - lastAccessTime;
        // --- �����ֺ������Ƴ� Priority ---
        return w1 * frequency - w2 * gap - w4 * size;
    }

    public double getSchedulingScore(long currentTime) {
        double remainingTime = deadline - currentTime;
        if (remainingTime <= 0) {
            remainingTime = 1;
        }
        // --- �����ֺ������Ƴ� Priority ---
        return w5 * (1000.0 / remainingTime) + w6 * processingTime;
    }

    public void updateAccess(long currentTime) {
        this.frequency++;
        this.lastAccessTime = currentTime;
    }

    public static void setWeights(double[] w) {
        // --- Ȩ�����鳤��������5 ---
        if (w.length != 5) throw new IllegalArgumentException("Ȩ�����鳤�ȱ���Ϊ5");
        w1 = w[0];
        w2 = w[1];
        w4 = w[2]; // ע�������仯
        w5 = w[3];
        w6 = w[4];
    }
}