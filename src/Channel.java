import java.util.Random;

public class Channel {
    private final double bandwidth;        // ���� (B) in Hz
    private final double baseLatency;      // �����ӳ� (�紫���ӳ�) in seconds
    private final double meanSnr;          // �����(S/N)�ľ�ֵ (ratio, not dB)
    private final double stdDevSnr;        // �����(S/N)�ı�׼����Ʋ���
    private final Random random;

    public Channel(double bandwidth, double baseLatency, double meanSnr, double stdDevSnr) {
        this.bandwidth = bandwidth;
        this.baseLatency = baseLatency;
        this.meanSnr = meanSnr;
        this.stdDevSnr = stdDevSnr;
        this.random = new Random();
    }

    private double getCurrentSnr() {
        double currentSnr = meanSnr + random.nextGaussian() * stdDevSnr;
        return Math.max(1.0, currentSnr);
    }

    public double getCapacity() {
        double snr = getCurrentSnr();
        // C = B * log2(1 + S/N)
        return bandwidth * (Math.log(1 + snr) / Math.log(2));
    }

    public double getTotalDelay(int dataSizeInBytes) {
        double capacityBps = getCapacity();
        if (capacityBps <= 0) {
            return Double.POSITIVE_INFINITY;
        }

        double dataSizeInBits = dataSizeInBytes * 8.0;
        double transmissionTime = dataSizeInBits / capacityBps;

        return baseLatency + transmissionTime;
    }
}