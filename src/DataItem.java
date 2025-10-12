public class DataItem {
    int id;
    int size;
    String type;
    int frequency;
    long lastAccessTime;
    int Priority;

    // --- ����Ϊ����ʱ��� ---
    long arrivalTime;      // ���񵽴�ľ���ʱ��
    long processingTime;   // ��������Ҫ�Ĵ���ʱ�� (����)
    long deadline;         // [����] ���������ɵľ���ʱ�̵� (ʱ���)

    // ����Ȩ��
    static double w1 = 0.2, w2 = 0.2, w3 = 0.2, w4 = 0.4;
    // ����Ȩ��
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
        this.deadline = absoluteDeadline; // �洢���Խ�ֹʱ���
    }

    public double getCacheScore(long currentTime) {
        double gap = currentTime - lastAccessTime;
        return w1 * frequency - w2 * gap + w3 * Priority - w4 * size;
    }

    public double getSchedulingScore(long currentTime) {
        // ��ʱ����ʣ��ʱ�䣬�������������̶�
        double remainingTime = deadline - currentTime;

        if (remainingTime <= 0) {
            remainingTime = 1; // �����ѳ�ʱ�򼴽���ʱ������������ȼ�
        }
        return w3 * Priority + w5 * (1000.0 / remainingTime) + w6 * processingTime;
    }

    public void updateAccess(long currentTime) {
        this.frequency++;
        this.lastAccessTime = currentTime;
    }

    public static void setWeights(double[] w) {
        if (w.length != 6) throw new IllegalArgumentException("Ȩ�����鳤�ȱ���Ϊ6");
        w1 = w[0]; w2 = w[1]; w3 = w[2]; w4 = w[3]; w5 = w[4]; w6 = w[5];
    }
}