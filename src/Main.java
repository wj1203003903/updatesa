import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {

    // --- 核心修改：定义一个公共静态变量来存储基准分数 ---
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

    public static void main(String[] args) {
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

        // --- 步骤1: 运行基准测试，并为静态变量赋值 ---
        System.out.println("\n=============================================");
        System.out.println("=== 1. Running Random Search (to set Baseline) ===");
        System.out.println("=============================================");
        RandomSearch rs = new RandomSearch(testData, baseDM);
        baselineScore = rs.run(); // 为静态变量赋值
        results.put("Random Search (Baseline)", baselineScore);


        // --- 步骤2: 依次运行其他算法，它们将直接使用 Main.baselineScore ---
        // 注意：现在所有 run() 方法都没有参数了
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

        System.out.println("\n\n=== All algorithms finished. ===");

        // --- 最终总结报告 (无需修改) ---
        System.out.println("\n=============================================");
        System.out.println("===           Final Score Summary           ===");
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
}