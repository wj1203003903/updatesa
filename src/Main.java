import java.io.BufferedReader;
import java.io.FileOutputStream; // 新增 import
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;      // 新增 import
import java.text.SimpleDateFormat; // 新增 import
import java.util.ArrayList;
import java.util.Date;             // 新增 import
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {

    // 静态变量 baselineScore 保持不变
    public static double baselineScore = 0;

    private static DataItem[] loadDataFromFile(String filePath) {
        // ... (此方法保持不变)
        List<DataItem> dataList = new ArrayList<>();
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                int id = Integer.parseInt(values[0].replace("\"", ""));
                int size = Integer.parseInt(values[1].replace("\"", ""));
                long arrivalTime = Long.parseLong(values[2].replace("\"", ""));
                long deadline = Long.parseLong(values[3].replace("\"", ""));
                String type = "nasa_request";
                dataList.add(new DataItem(id, size, type, arrivalTime, deadline));
            }
        } catch (IOException e) {
            System.err.println("错误：读取数据文件失败！请检查路径是否正确： " + filePath);
            e.printStackTrace();
        }
        return dataList.toArray(new DataItem[0]);
    }

    public static void main(String[] args) throws IOException { // 声明可能抛出 IOException

        // --- 核心修改 1: 设置文件输出流 ---

        // a. 创建一个带有时间戳的唯一文件名
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String timestamp = sdf.format(new Date());
        String logFileName = "experiment_log_" + timestamp + ".txt";

        // b. 创建文件输出流和打印流
        FileOutputStream fileOutputStream = new FileOutputStream(logFileName);
        PrintStream printStream = new PrintStream(fileOutputStream, true, "UTF-8"); // true for auto-flushing, UTF-8 for encoding

        // c. 保存原始的控制台输出流
        PrintStream originalOut = System.out;

        // d. 将标准输出 (System.out) 重定向到文件打印流
        System.setOut(printStream);

        // --- 从这里开始，所有的 System.out.println() 都会被自动写入文件 ---

        try {
            // --- 您的所有实验代码都放在这个 try 块中 ---

            System.out.println("实验开始时间: " + new Date());
            System.out.println("所有输出将被记录到文件: " + logFileName);

            final int TOTAL_EXPERIMENT_RUNS = 20; // 在这里设置总的实验次数

            for (int i = 1; i <= TOTAL_EXPERIMENT_RUNS; i++) {

                System.out.printf("\n\n<<<<<<<<<< STARTING EXPERIMENT RUN #%d of %d >>>>>>>>>>\n", i, TOTAL_EXPERIMENT_RUNS);

                // --- 数据加载和环境设置 ---
                System.out.println("正在从文件加载数据集...");
                DataItem[] testData = loadDataFromFile("dataset/processed_nasa_log.csv");
                if (testData.length == 0) {
                    System.out.println("错误：无法加载数据或数据为空！");
                    return;
                }
                System.out.println("数据集加载完毕，共 " + testData.length + " 条数据。");

                long localCapacity = 1000L * 1024L;
                long edgeCapacity = 6000L * 1024L;
                DataManager baseDM = new DataManager(localCapacity, edgeCapacity);

                for (DataItem item : testData) {
                    baseDM.registerDataItem(item);
                    baseDM.addDataToRemote(item, item.arrivalTime);
                }

                Map<String, Double> results = new LinkedHashMap<>();

                // ... (所有算法的调用逻辑保持完全不变) ...

                System.out.println("\n=============================================");
                System.out.println("=== 1. Running Random Search (to set Baseline) ===");
                System.out.println("=============================================");
                RandomSearch rs = new RandomSearch(testData, baseDM);
                baselineScore = rs.run();
                results.put("Random Search (Baseline)", baselineScore);

                System.out.println("\n=============================================");
                System.out.println("=== 2. Running Basic Simulated Annealing (SA) ===");
                System.out.println("=============================================");
                SA sa = new SA(testData, baseDM);
                results.put("Simulated Annealing (SA)", sa.run());

                System.out.println("\n=============================================");
                System.out.println("=== 3. Running Particle Swarm Optimization (PSO) ===");
                System.out.println("=============================================");
                PSO pso = new PSO(testData, baseDM);
                results.put("Particle Swarm (PSO)", pso.run());

                System.out.println("\n=============================================");
                System.out.println("=== 4. Running Genetic Algorithm (GA) ===");
                System.out.println("=============================================");
                GA ga = new GA(testData, baseDM);
                results.put("Genetic Algorithm (GA)", ga.run());

                System.out.println("\n=============================================");
                System.out.println("=== 5. Running Ant Colony Optimization (ACO) ===");
                System.out.println("=============================================");
                ACO aco = new ACO(testData, baseDM);
                results.put("Ant Colony (ACO)", aco.run());

                System.out.println("\n=============================================");
                System.out.println("=== 6. Running Advanced Simulated Annealing (UpdateSA) ===");
                System.out.println("=============================================");
                UpdateSA updateSA = new UpdateSA(testData, baseDM);
                results.put("Advanced SA (UpdateSA)", updateSA.run());

                System.out.println("\n\n=== Run #" + i + " Finished. Final Summary for this run: ===");

                // --- 打印本次实验的最终总结报告 ---
                System.out.println("\n=============================================");
                System.out.println("===           Final Score Summary (Run #" + i + ")       ===");
                System.out.println("=============================================");
                System.out.printf("%-30s | %-15s | %-20s\n", "Algorithm", "Best Score", "Improvement vs Baseline");
                System.out.println("----------------------------------------------------------------------");

                for (Map.Entry<String, Double> entry : results.entrySet()) {
                    String name = entry.getKey();
                    double score = entry.getValue();
                    if (name.contains("Baseline")) {
                        System.out.printf("%-30s | %-15.4f | %-20s\n", name, score, "N/A");
                    } else {
                        double improvement = score - baselineScore;
                        System.out.printf("%-30s | %-15.4f | %-20.4f\n", name, score, improvement);
                    }
                }
                System.out.println("=============================================");
            }

            System.out.println("\n\nAll " + TOTAL_EXPERIMENT_RUNS + " experiment runs have been completed.");

        } finally {
            // --- 核心修改 2: 无论程序是否出错，都确保恢复和关闭 ---

            System.out.println("\n实验结束时间: " + new Date());

            // e. 将 System.out 恢复到原始的控制台
            System.setOut(originalOut);

            // f. 关闭文件流
            printStream.close();
            fileOutputStream.close();

            // g. 在控制台打印一条最终的成功消息，告知用户日志已保存
            System.out.println("\n所有实验已完成！");
            System.out.println("详细日志已完整保存到文件: " + logFileName);
        }
    }
}