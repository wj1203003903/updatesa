import java.util.Random;

public class Channel {
    private final double bandwidth;        // 带宽 (B) in Hz
    private final double baseLatency;      // 基础延迟 (如传播延迟) in seconds
    private final double meanSnr;          // 信噪比(S/N)的均值 (ratio, not dB)
    private final double stdDevSnr;        // 信噪比(S/N)的标准差，控制波动
    private final Random random;

    /**
     * 通用信道模型的构造函数
     *
     * @param bandwidth   信道带宽 (Hz)
     * @param baseLatency 固定的基础延迟 (seconds)
     * @param meanSnr     信噪比的均值 (ratio)
     * @param stdDevSnr   信噪比的标准差 (ratio)
     */
    public Channel(double bandwidth, double baseLatency, double meanSnr, double stdDevSnr) {
        this.bandwidth = bandwidth;
        this.baseLatency = baseLatency;
        this.meanSnr = meanSnr;
        this.stdDevSnr = stdDevSnr;
        this.random = new Random();
    }

    /**
     * 根据设定的均值和标准差，生成一个当前时刻的信噪比(S/N)
     * @return 当前的信噪比 (ratio)
     */
    private double getCurrentSnr() {
        // 使用高斯（正态）分布随机数模拟S/N波动
        double currentSnr = meanSnr + random.nextGaussian() * stdDevSnr;
        // 确保S/N至少为1 
        return Math.max(1.0, currentSnr);
    }

    /**
     * 根据香农公式计算当前信道的瞬时容量
     * @return 信道容量 (bits per second)
     */
    public double getCapacity() {
        double snr = getCurrentSnr();
        // C = B * log2(1 + S/N)
        return bandwidth * (Math.log(1 + snr) / Math.log(2));
    }

    /**
     * 计算传输指定大小数据所需的总延迟（基础延迟 + 传输时间）
     * @param dataSizeInBytes 数据大小 (bytes)
     * @return 总延迟 (seconds)
     */
    public double getTotalDelay(int dataSizeInBytes) {
        double capacityBps = getCapacity();
        if (capacityBps <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        
        // 1. 计算传输时间
        double dataSizeInBits = dataSizeInBytes * 8.0;
        double transmissionTime = dataSizeInBits / capacityBps;
        
        // 2. 加上基础延迟
        return baseLatency + transmissionTime;
    }
}