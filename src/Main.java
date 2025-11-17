import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

public class Main {

    // loadDataFromFile 方法保持不变...
    private static DataItem[] loadDataFromFile(String filePath, int maxLines) {
        List<DataItem> dataList = new ArrayList<>();
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            br.readLine();
            int lineCount = 0;
            while ((line = br.readLine()) != null && (maxLines <= 0 || lineCount < maxLines)) {
                String[] values = line.split(",");
                int id = Integer.parseInt(values[0].replace("\"", ""));
                int size = Integer.parseInt(values[1].replace("\"", ""));
                long arrivalTime = (long) Double.parseDouble(values[2].replace("\"", ""));
                long deadline = (long) Double.parseDouble(values[3].replace("\"", ""));
                String type = "nasa_request";
                dataList.add(new DataItem(id, size, type, arrivalTime, deadline));
                lineCount++;
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("错误：读取或解析 " + filePath + " 失败！");
            e.printStackTrace();
        }
        return dataList.toArray(new DataItem[0]);
    }

    /**
     * 线程安全版本：在单个数据集上运行所有算法。
     */
    public static void runExperimentOnDataset(String datasetName, DataItem[] dataSet, DataManager dm, long seed, PrintStream out) {
        out.printf("\n\n=============== 正在对 [%s] 数据集运行完整测试 ===============\n", datasetName);
        if (dataSet == null || dataSet.length == 0) {
            out.println("错误：数据集为空，跳过测试。");
            return;
        }

        dm.reset();
        for (DataItem item : dataSet) {
            dm.registerDataItem(item);
            dm.addDataToRemote(item, item.arrivalTime);
        }

        Map<String, Double> results = new LinkedHashMap<>();
        double baselineScore;

        out.println("\n--- 1. Running Random Search (to set Baseline) ---");
        RandomSearch rs = new RandomSearch(dataSet, dm, seed);
        baselineScore = rs.run(out); // RandomSearch.run 不需要 baselineScore
        results.put("Random Search (Baseline)", baselineScore);

        // --- 核心修改：将 baselineScore 传递给每个算法的 run 方法 ---
        out.println("\n--- 2. Running Basic Simulated Annealing (SA) ---");
        SA sa = new SA(dataSet, dm, seed);
        results.put("Simulated Annealing (SA)", sa.run(out, baselineScore));

        out.println("\n--- 3. Running Particle Swarm Optimization (PSO) ---");
        PSO pso = new PSO(dataSet, dm, seed);
        results.put("Particle Swarm (PSO)", pso.run(out, baselineScore));

        out.println("\n--- 4. Running Genetic Algorithm (GA) ---");
        GA ga = new GA(dataSet, dm, seed);
        results.put("Genetic Algorithm (GA)", ga.run(out, baselineScore));

        out.println("\n--- 5. Running Ant Colony Optimization (ACO) ---");
        ACO aco = new ACO(dataSet, dm, seed);
        results.put("Ant Colony (ACO)", aco.run(out, baselineScore));

        out.println("\n--- 6. Running Advanced Simulated Annealing (UpdateSA) ---");
        UpdateSA updateSA = new UpdateSA(dataSet, dm, seed);
        results.put("Advanced SA (UpdateSA)", updateSA.run(out, baselineScore));

        // 打印总结报告 (这部分逻辑不变，因为它本来就能访问 baselineScore)
        out.println("\n==========================================================");
        out.printf("=== Final Score Summary for: [%-20s] ===\n", datasetName);
        out.println("==========================================================");
        out.printf("%-30s | %-15s | %-20s\n", "Algorithm", "Best Score", "Improvement vs Baseline");
        out.println("----------------------------------------------------------------------");

        for (Map.Entry<String, Double> entry : results.entrySet()) {
            String name = entry.getKey();
            double score = entry.getValue();
            if (name.contains("Baseline")) {
                out.printf("%-30s | %-15.4f | %-20s\n", name, score, "N/A");
            } else {
                double improvement = score - baselineScore;
                out.printf("%-30s | %-15.4f | %-20.4f\n", name, score, improvement);
            }
        }
        out.println("==========================================================");
    }

    // runSingleFullExperiment 和 main 方法保持不变...
    public static void runSingleFullExperiment(int runNumber) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String logFileName = String.format("experiment_run-%d_%s.txt", runNumber, timestamp);
        try (FileOutputStream fos = new FileOutputStream(logFileName);
             PrintStream out = new PrintStream(fos, true, "UTF-8")) {
            out.println("实验 #" + runNumber + " 开始时间: " + new Date());
            long randomseed = (long)runNumber * runNumber * runNumber * 1000;
            out.printf("\n<<<<<<<<<< STARTING EXPERIMENT RUN #%d (Seed: %d) >>>>>>>>>>\n", runNumber, randomseed);
            long localCapacity = 500L * 1024L;
            long edgeCapacity = 2000L * 1024L;
            DataManager dataManager = new DataManager(localCapacity, edgeCapacity);
            out.println("从 dataset/processed_nasa_log.csv 加로드了 10000 条数据。");
            runExperimentOnDataset("真实数据 (1万行)", loadDataFromFile("dataset/processed_nasa_log.csv", 10000), dataManager, randomseed, out);
            out.println("从 dataset/processed_nasa_log.csv 加载了 20000 条数据。");
            runExperimentOnDataset("真实数据 (2万行)", loadDataFromFile("dataset/processed_nasa_log.csv", 20000), dataManager, randomseed, out);
            out.println("从 dataset/processed_nasa_log.csv 加载了 40000 条数据。");
            runExperimentOnDataset("真实数据 (4万行)", loadDataFromFile("dataset/processed_nasa_log.csv", 40000), dataManager, randomseed, out);
            DataGenerator generator = new DataGenerator(10000, 500, randomseed);
            runExperimentOnDataset("合成数据-正态分布", generator.generateNormalDistribution(), dataManager, randomseed, out);
            runExperimentOnDataset("合成数据-指数分布", generator.generateExponentialDistribution(), dataManager, randomseed, out);
            out.println("\n\n=== Run #" + runNumber + " Finished. ===");
            out.println("实验 #" + runNumber + " 结束时间: " + new Date());
        } catch (IOException e) {
            System.err.println("运行实验 #" + runNumber + " 时发生严重IO错误:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("主程序启动，开始并行执行所有实验...");
        final int TOTAL_EXPERIMENT_RUNS = 10;
        final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
        System.out.println("将使用 " + NUM_THREADS + " 个线程并行运行 " + TOTAL_EXPERIMENT_RUNS + " 次实验。");
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        for (int i = 1; i <= TOTAL_EXPERIMENT_RUNS; i++) {
            final int runNumber = i;
            executor.submit(() -> {
                System.out.println("任务 #" + runNumber + " 已提交，准备在线程 " + Thread.currentThread().getName() + " 上运行。");
                runSingleFullExperiment(runNumber);
                System.out.println("任务 #" + runNumber + " 已完成。");
            });
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(24, TimeUnit.HOURS)) {
                System.err.println("错误：并非所有任务都在24小时内完成！");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.err.println("等待线程池关闭时被中断！");
            executor.shutdownNow();
        }
        System.out.println("\n所有实验均已执行完毕！程序退出。");
        System.out.println("请检查项目根目录下生成的 experiment_run-X_...txt 文件获取详细结果。");
    }
}