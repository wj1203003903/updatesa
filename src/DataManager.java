import java.util.*;
// ���ݹ�����
class DataManager {
    LocalCache local;
    CloudCache cloud;
    RemoteCloud remote;

    private final double LOCAL_BUS_SPEED_BPS = 10 * 1e9; // ���������ٶ� 10Gbps
    private final Channel cloudChannel;
    private final Channel remoteChannel;

    // --- �Ķ� 2: ����һ�� totalDelay ʵ���������ۼӶ�̬�ӳ� ---
    private double totalDelay = 0.0;

    int totalAccessed = 0;
    int localHits = 0;
    int cloudHits = 0;

    int localAccesses = 0;
    int cloudAccesses = 0;
    int remoteAccesses = 0;
    
    private Map<Integer, DataItem> idToDataItemMap = new HashMap<>();
    
    public double getSystemScore() {
        if (localAccesses == 0 || cloudAccesses == 0) return 0; // ����������

        double localHitRate = (double) localHits / localAccesses;
        double cloudHitRate = (double) cloudHits / cloudAccesses;

        // --- �Ķ� 3: ʹ���ۼӵ� totalDelay ʵ������ ---
        // ע�⣺����ĳͷ����� 10000 ������Ҫ���� totalDelay ��ʵ�����������е���
        // ���磬����ܷ��ʴ�����1000�Σ�ƽ���ӳ�0.05�룬totalDelay����50.
        // (1 + 50/10000) �仯���󡣿��Կ����� (1 + totalDelay / totalAccessed) ƽ���ӳ�
        double averageDelay = (totalAccessed > 0) ? totalDelay / totalAccessed : 0;
        
        double w1 = 3.0;
        double w2 = 1.0;
        // ʹ��ƽ���ӳ���Ϊ�ͷ�����߿ɱ���
        return (w1 * localHitRate + w2 * cloudHitRate) / (1.0 + averageDelay * 10); // ����10�Ŵ�ͷ�Ч��
    }

    public DataManager(int localCap, int edgeCap) {
        local = new LocalCache(localCap);
        cloud = new CloudCache(edgeCap);
        remote = new RemoteCloud();

        local.bindLowerLevel(cloud);
        cloud.bindLowerLevel(remote);

        // --- �Ķ� 4: �ڹ��캯���г�ʼ���ŵ����� ---
        // ���� Cloud Channel (CDN)
        this.cloudChannel = new Channel(20 * 1e6, 0.030, 100.0, 40.0);
        // ���� Remote Channel (Դվ)
        this.remoteChannel = new Channel(100 * 1e6, 0.150, 50.0, 30.0);
    }
    
    // generateDataItem �������ֲ���
    public DataItem generateDataItem(int id) {
        if (idToDataItemMap.containsKey(id)) {
            return idToDataItemMap.get(id);
        }
        Random rand = new Random(id);
        int size = rand.nextInt(50) + 10;
        String type = "type" + rand.nextInt(5);
        int frequency = rand.nextInt(10) + 1;
        int priority = rand.nextInt(3) + 1;
        DataItem item = new DataItem(id, size, type, frequency, priority);
        idToDataItemMap.put(id, item);
        return item;
    }

    // --- �Ķ� 5: �޸� access �����Լ�����ۼӶ�̬�ӳ� ---
    public DataItem access(String type, int priority, int id) {
        totalAccessed++;
        
        // ���ʱ���
        localAccesses++;
        DataItem item = local.get(type, priority, id);
        if (item != null) {
            localHits++;
            // ���㱾���ӳٲ��ۼ�
            this.totalDelay += (item.size * 8.0) / LOCAL_BUS_SPEED_BPS;
            return item;
        }

        // �����ƶ�
        cloudAccesses++;
        item = cloud.get(type, priority, id);
        if (item != null) {
            cloudHits++;
            // �����ƶ��ӳٲ��ۼ�
            this.totalDelay += cloudChannel.getTotalDelay(item.size);
            local.put(item);

            return item;
        }
        
        // ����Զ��
        remoteAccesses++;
        item = remote.get(type, priority, id);
        if (item != null) {
            // ����Զ���ӳٲ��ۼ�
            this.totalDelay += remoteChannel.getTotalDelay(item.size);
            local.put(item);
            cloud.put(item);
            return item;
        }

        // ����������
        item = generateDataItem(id);
        // �����ݴ�Զ�̻�ȡ�������ӳٲ��ۼ�
        this.totalDelay += remoteChannel.getTotalDelay(item.size);
        // �������ݷ��뻺��
        remote.put(item);
        cloud.put(item);
        local.put(item);
        return item;
    }

    // addDataToRemote �������ֲ���
    public void addDataToRemote(DataItem item) {
        remote.put(item);
    }
    public void normalizeL2(double[] vector) {
        if (vector == null || vector.length != 4) {
            throw new IllegalArgumentException("������������ҳ���Ϊ4��");
        }

        // 1. ��������Ԫ��ƽ���͵�ƽ���� (L2 ����)
        double sumOfSquares = 0.0;
        for (double value : vector) {
            sumOfSquares += value * value;
        }
        
        // �������Ԫ�ض���0��L2����Ϊ0�������������
        if (sumOfSquares == 0) {
            // ����Ԫ�ض���0���޷����򻯣�����ԭ�����ɡ�
            // ���߿��Ը���ҵ����������Ϊ�ض�ֵ��������Ϊ0�����
            return;
        }

        double l2Norm = Math.sqrt(sumOfSquares);

        // 2. ��ÿ��Ԫ�س��� L2 ����
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / l2Norm;
        }
    }
    public void reset() {
        local.dataMap.clear();
        cloud.dataMap.clear();
        
        // �������м�����
        totalAccessed = 0;
        localHits = 0;
        cloudHits = 0;
        localAccesses = 0;
        cloudAccesses = 0;
        remoteAccesses = 0;
        
        // --- �Ķ� 6: ���� totalDelay ---
        totalDelay = 0.0;
    }

    public void printStats() {
        if (localAccesses == 0 || cloudAccesses == 0) {
            System.out.println("���ʴ������㣬�޷�����ͳ�����ݡ�");
            return;
        }
        
        double localHitRate = (double) localHits / localAccesses;
        double cloudHitRate = (double) cloudHits / cloudAccesses;

        System.out.println("==== ����ͳ�� ====");
        System.out.println("�����ݷ��ʴ���: " + totalAccessed);
        System.out.printf("����������: %.3f\n", localHitRate);
        System.out.printf("�ƶ�������: %.3f\n", cloudHitRate);
        System.out.printf("��ʱ��: %.3f s\n", totalDelay); // ��λ����
        System.out.printf("ƽ��ʱ��: %.3f ms\n", (totalDelay / totalAccessed) * 1000); // ��ӡ���������ƽ���ӳ�
    }
}