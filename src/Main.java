import java.io.*;
import java.util.*;

public class Main {

    // ==========================================
    // 1. 仿真上下文 (State Machine)
    // ==========================================
    static class SimContext {
        String name;
        DataManager dm;
        double[] weights;

        // DataTest 逻辑变量
        List<DataItem> futureTasks;
        List<DataItem> readyQueue;
        int completedCount;
        int futurePtr;
        long currentTime;

        public SimContext(String name, DataManager dm, DataItem[] dataset) {
            this.name = name;
            this.dm = dm;
            this.weights = null;

            this.futureTasks = new ArrayList<>();
            for (DataItem item : dataset) this.futureTasks.add(cloneItem(item));
            this.futureTasks.sort(Comparator.comparingLong(item -> item.arrivalTime));

            this.readyQueue = new ArrayList<>();
            this.completedCount = 0;
            this.futurePtr = 0;
            if (!futureTasks.isEmpty()) this.currentTime = futureTasks.get(0).arrivalTime;

            for (DataItem item : this.futureTasks) {
                dm.registerDataItem(item);
                dm.addDataToRemote(item, item.arrivalTime);
            }
        }

        public void runToTarget(int targetCount) {
            if (this.weights != null) DataItem.setWeights(this.weights);

            while (this.completedCount < targetCount && this.completedCount < futureTasks.size()) {
                while (futurePtr < futureTasks.size() && futureTasks.get(futurePtr).arrivalTime <= currentTime) {
                    readyQueue.add(futureTasks.get(futurePtr));
                    futurePtr++;
                }

                if (!readyQueue.isEmpty()) {
                    final long decisionTime = currentTime;
                    DataItem bestTask = readyQueue.stream()
                            .max(Comparator.comparingDouble(item -> item.getSchedulingScore(decisionTime)))
                            .orElse(null);

                    readyQueue.remove(bestTask);
                    long duration = dm.processAndGetDuration(bestTask, currentTime, readyQueue.size());
                    currentTime += duration;
                    completedCount++;
                } else {
                    if (futurePtr < futureTasks.size()) {
                        currentTime = futureTasks.get(futurePtr).arrivalTime;
                    } else {
                        break;
                    }
                }
            }
        }
    }

    // ==========================================
    // 2. 数据加载与辅助
    // ==========================================
    private static DataItem[] loadDataFromFile(String filePath, int maxLines) {
        List<DataItem> dataList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            br.readLine();
            String line;
            int count = 0;
            while ((line = br.readLine()) != null && (maxLines <= 0 || count < maxLines)) {
                String[] v = line.split(",");
                int id = Integer.parseInt(v[0].replace("\"", "").trim());
                int size = Integer.parseInt(v[1].replace("\"", "").trim());
                long arr = (long) Double.parseDouble(v[2].replace("\"", "").trim());
                long ddl = (long) Double.parseDouble(v[3].replace("\"", "").trim());
                dataList.add(new DataItem(id, size, "nasa", arr, ddl));
                count++;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return dataList.toArray(new DataItem[0]);
    }

    private static DataItem cloneItem(DataItem o) {
        return new DataItem(o.id, o.size, o.type, o.arrivalTime, o.deadline);
    }

    // ==========================================
    // 3. 动态实验主入口
    // ==========================================
    public static void runDynamicComparison(int runNumber) {
        String filename = "dynamic_cumulative_run_" + runNumber + ".csv";

        try (FileOutputStream fos = new FileOutputStream(filename);
             PrintStream out = new PrintStream(fos, true, "UTF-8")) {

            System.out.printf("\n=== 启动动态实验 (Run #%d) [Cumulative Metrics] ===\n", runNumber);

            DataItem[] rawDataset = loadDataFromFile("dataset/processed_nasa_log.csv", 50000);
            if (rawDataset.length == 0) return;

            // 建议：3MB L1, 5MB L2 (配合静态实验的规模)
            long localCap = 3000 * 1024L;
            long edgeCap = 5000 * 1024L;

            SimContext ctxStatic = new SimContext("Static", new DataManager(localCap, edgeCap), rawDataset);
            SimContext ctxSA     = new SimContext("SA",     new DataManager(localCap, edgeCap), rawDataset);
            SimContext ctxPSO    = new SimContext("PSO",    new DataManager(localCap, edgeCap), rawDataset);
            SimContext ctxGA     = new SimContext("GA",     new DataManager(localCap, edgeCap), rawDataset);
            SimContext ctxACO    = new SimContext("ACO",    new DataManager(localCap, edgeCap), rawDataset);
            SimContext ctxSAGA   = new SimContext("SAGA",   new DataManager(localCap, edgeCap), rawDataset);

            int TOTAL_TASKS = rawDataset.length;
            int RETRAIN_WINDOW = 5000;
            int TRAINING_SIZE = 5000;
            int LOG_INTERVAL = 500;
            long seed = runNumber * 999L;
            Random rng = new Random();

            // CSV Header
            out.println("LogIndex," +
                    "Static_Hit,SA_Hit,PSO_Hit,GA_Hit,ACO_Hit,SAGA_Hit," +
                    "Static_Lat,SA_Lat,PSO_Lat,GA_Lat,ACO_Lat,SAGA_Lat," +
                    "Static_Comp,SA_Comp,PSO_Comp,GA_Comp,ACO_Comp,SAGA_Comp");

            // ==========================================
            // 4. 步进循环
            // ==========================================
            for (int target = LOG_INTERVAL; target <= TOTAL_TASKS; target += LOG_INTERVAL) {

                int currentProgress = target - LOG_INTERVAL;

                // --- A. 训练阶段 (All Cold Start) ---
                if (currentProgress % RETRAIN_WINDOW == 0) {
                    System.out.printf(">>> Progress %d: Retraining (Cold Start)...\n", currentProgress);

                    if (currentProgress == 0) ctxStatic.weights = randomWeights(rng);

                    DataItem[] trainData = getTrainData(rawDataset, currentProgress, TRAINING_SIZE);
                    DataManager trainDM = new DataManager(localCap, edgeCap);

                    SA optSA = new SA(trainData, trainDM, seed + currentProgress);
                    ctxSA.weights = optSA.optimize(null);

                    PSO optPSO = new PSO(trainData, trainDM, seed + currentProgress);
                    ctxPSO.weights = optPSO.optimize(null);

                    GA optGA = new GA(trainData, trainDM, seed + currentProgress);
                    ctxGA.weights = optGA.optimize(null);

                    ACO optACO = new ACO(trainData, trainDM, seed + currentProgress);
                    ctxACO.weights = optACO.optimize(null);

                    UpdateSA optSAGA = new UpdateSA(trainData, trainDM, seed + currentProgress);
                    ctxSAGA.weights = optSAGA.optimize(null);
                }

                // --- B. 执行阶段 ---
                ctxStatic.runToTarget(target);
                ctxSA.runToTarget(target);
                ctxPSO.runToTarget(target);
                ctxGA.runToTarget(target);
                ctxACO.runToTarget(target);
                ctxSAGA.runToTarget(target);

                // --- C. 打点 (累计指标) ---
                int logIdx = target / LOG_INTERVAL;
                StringBuilder sb = new StringBuilder();
                sb.append(logIdx).append(",");

                // Hit Rate (Cumulative)
                sb.append(fmt(getCumulativeMetric(ctxStatic.dm, 0))).append(",");
                sb.append(fmt(getCumulativeMetric(ctxSA.dm, 0))).append(",");
                sb.append(fmt(getCumulativeMetric(ctxPSO.dm, 0))).append(",");
                sb.append(fmt(getCumulativeMetric(ctxGA.dm, 0))).append(",");
                sb.append(fmt(getCumulativeMetric(ctxACO.dm, 0))).append(",");
                sb.append(fmt(getCumulativeMetric(ctxSAGA.dm, 0))).append(",");

                // Latency (Cumulative)
                sb.append(fmt(getCumulativeMetric(ctxStatic.dm, 1))).append(",");
                sb.append(fmt(getCumulativeMetric(ctxSA.dm, 1))).append(",");
                sb.append(fmt(getCumulativeMetric(ctxPSO.dm, 1))).append(",");
                sb.append(fmt(getCumulativeMetric(ctxGA.dm, 1))).append(",");
                sb.append(fmt(getCumulativeMetric(ctxACO.dm, 1))).append(",");
                sb.append(fmt(getCumulativeMetric(ctxSAGA.dm, 1))).append(",");

                // Comp Rate (Cumulative)
                sb.append(fmt(getCumulativeMetric(ctxStatic.dm, 2))).append(",");
                sb.append(fmt(getCumulativeMetric(ctxSA.dm, 2))).append(",");
                sb.append(fmt(getCumulativeMetric(ctxPSO.dm, 2))).append(",");
                sb.append(fmt(getCumulativeMetric(ctxGA.dm, 2))).append(",");
                sb.append(fmt(getCumulativeMetric(ctxACO.dm, 2))).append(",");
                sb.append(fmt(getCumulativeMetric(ctxSAGA.dm, 2)));

                out.println(sb.toString());
            }
            System.out.println("Done. File: " + filename);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- Helpers ---

    private static DataItem[] getTrainData(DataItem[] all, int currentIdx, int size) {
        if (currentIdx == 0) return copy(all, 0, Math.min(size, all.length));
        int start = Math.max(0, currentIdx - size);
        return copy(all, start, currentIdx);
    }
    private static DataItem[] copy(DataItem[] src, int s, int e) {
        int len = e - s; DataItem[] d = new DataItem[len];
        for(int k=0;k<len;k++) d[k] = cloneItem(src[s+k]); return d;
    }
    private static double[] randomWeights(Random r) {
        double[] w = new double[5]; double s=0; for(int i=0;i<5;i++){w[i]=r.nextDouble();s+=w[i];} for(int i=0;i<5;i++)w[i]/=s; return w;
    }
    private static String fmt(double v) { return String.format("%.4f", v); }

    /**
     * 获取【累计】指标
     * 直接使用 dm 的 total 变量，不使用快照做减法
     */
    private static double getCumulativeMetric(DataManager dm, int type) {
        // 0: Cumulative Hit Rate
        if (type == 0) {
            return (dm.totalAccessed > 0) ?
                    (double)(dm.localHits + dm.cloudHits) / dm.totalAccessed : 0;
        }
        // 1: Cumulative Avg Latency (ms)
        if (type == 1) {
            return (dm.totalTasks > 0) ?
                    (dm.totalDelaySeconds / dm.totalTasks) * 1000 : 0;
        }
        // 2: Cumulative Completion Rate
        if (type == 2) {
            return (dm.totalTasks > 0) ?
                    (double)dm.completedTasks / dm.totalTasks : 0;
        }
        return 0;
    }

    public static void main(String[] args) { runDynamicComparison(1); }
}