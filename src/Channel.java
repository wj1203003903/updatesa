import java.util.Random;

public class Channel {
    private final double bandwidth;        // ���� (B) in Hz
    private final double baseLatency;      // �����ӳ� (�紫���ӳ�) in seconds
    private final double meanSnr;          // �����(S/N)�ľ�ֵ (ratio, not dB)
    private final double stdDevSnr;        // �����(S/N)�ı�׼����Ʋ���
    private final Random random;

    /**
     * ͨ���ŵ�ģ�͵Ĺ��캯��
     *
     * @param bandwidth   �ŵ����� (Hz)
     * @param baseLatency �̶��Ļ����ӳ� (seconds)
     * @param meanSnr     ����ȵľ�ֵ (ratio)
     * @param stdDevSnr   ����ȵı�׼�� (ratio)
     */
    public Channel(double bandwidth, double baseLatency, double meanSnr, double stdDevSnr) {
        this.bandwidth = bandwidth;
        this.baseLatency = baseLatency;
        this.meanSnr = meanSnr;
        this.stdDevSnr = stdDevSnr;
        this.random = new Random();
    }

    /**
     * �����趨�ľ�ֵ�ͱ�׼�����һ����ǰʱ�̵������(S/N)
     * @return ��ǰ������� (ratio)
     */
    private double getCurrentSnr() {
        // ʹ�ø�˹����̬���ֲ������ģ��S/N����
        double currentSnr = meanSnr + random.nextGaussian() * stdDevSnr;
        // ȷ��S/N����Ϊ1 
        return Math.max(1.0, currentSnr);
    }

    /**
     * ������ũ��ʽ���㵱ǰ�ŵ���˲ʱ����
     * @return �ŵ����� (bits per second)
     */
    public double getCapacity() {
        double snr = getCurrentSnr();
        // C = B * log2(1 + S/N)
        return bandwidth * (Math.log(1 + snr) / Math.log(2));
    }

    /**
     * ���㴫��ָ����С������������ӳ٣������ӳ� + ����ʱ�䣩
     * @param dataSizeInBytes ���ݴ�С (bytes)
     * @return ���ӳ� (seconds)
     */
    public double getTotalDelay(int dataSizeInBytes) {
        double capacityBps = getCapacity();
        if (capacityBps <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        
        // 1. ���㴫��ʱ��
        double dataSizeInBits = dataSizeInBytes * 8.0;
        double transmissionTime = dataSizeInBits / capacityBps;
        
        // 2. ���ϻ����ӳ�
        return baseLatency + transmissionTime;
    }
}