import java.io.File;
import java.io.FilenameFilter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class LogProcessor {

    // --- 【核心配置】---
    // 请将此路径设置为您的日志文件所在的目录。
    // 如果您的日志文件在根目录下的 "dataset" 文件夹里, 就改为 "dataset/"
    private static final String LOG_DIRECTORY_PATH = ".";

    private static final String[] ALGORITHMS = {
            "Simulated Annealing (SA)",
            "Particle Swarm (PSO)",
            "Genetic Algorithm (GA)",
            "Ant Colony (ACO)",
            "Advanced SA (UpdateSA)"
    };

    private static final int MAX_GENERATIONS = 150;

    // --- 实验分组键名 (确保与 Main.java 日志输出一致) ---
    private static final Map<String, String> EXPERIMENT_KEYS = new LinkedHashMap<>();
    static {
        EXPERIMENT_KEYS.put("真实数据 (1万行)", "10k_Tasks_Data");
        EXPERIMENT_KEYS.put("真实数据 (1.5万行)", "15k_Tasks_Data");
        EXPERIMENT_KEYS.put("真实数据 (2万行)", "20k_Tasks_Data");
        // 【重要】确保这个名字和您 Main.java 中打印的标题完全一致
        EXPERIMENT_KEYS.put("合成数据-zip分布", "Synthetic_Zipf_Data");
    }

    // --- 数据结构 ---
    private static final Map<String, Map<String, List<Map<Integer, Double>>>> groupedRunsData = new LinkedHashMap<>();
    private static final Map<String, Map<String, List<FinalMetrics>>> groupedRunsMetrics = new LinkedHashMap<>();

    private static class FinalMetrics {
        double completionRate = 0.0, localCacheHitRate = 0.0, edgeCloudHitRate = 0.0, remoteAccessRate = 0.0, avgLatency = 0.0;
    }

    /**
     * 【已重构】主方法，用于动态查找并处理日志文件。
     */
    public static void main(String[] args) {
        File logDir = new File(LOG_DIRECTORY_PATH);

        // 创建一个文件名过滤器，只匹配 "experiment_run-X_....txt" 格式的文件
        FilenameFilter logFileFilter = (dir, name) -> name.startsWith("experiment_run-") && name.endsWith(".txt");
        File[] logFiles = logDir.listFiles(logFileFilter);

        if (logFiles == null || logFiles.length == 0) {
            System.err.println("错误：在目录 '" + logDir.getAbsolutePath() + "' 中未找到任何日志文件！");
            System.err.println("请确认：");
            System.err.println("1. LOG_DIRECTORY_PATH 常量设置是否正确。");
            System.err.println("2. 您的 Main 程序是否已成功在指定目录下生成了 .txt 日志文件。");
            return;
        }

        // 按文件名排序，确保处理顺序是从 run-1, run-2, ...
        Arrays.sort(logFiles, Comparator.comparing(File::getName));
        initializeDataStructures(logFiles.length);

        try {
            for (int i = 0; i < logFiles.length; i++) {
                File logFile = logFiles[i];
                System.out.println("--- Processing file #" + (i + 1) + ": " + logFile.getPath() + " ---");
                parseLogFile(logFile.getPath(), i);
            }
            System.out.println("\n\n<<<<<<<<<< ALL LOG FILES PROCESSED. GENERATING FINAL RESULTS... >>>>>>>>>>");
            generateGroupedCsvOutputs();
        } catch (IOException e) {
            System.err.println("处理日志文件时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 根据动态发现的日志文件总数来初始化数据结构。
     * @param numRuns 找到的日志文件总数。
     */
    private static void initializeDataStructures(int numRuns) {
        for (String groupKey : EXPERIMENT_KEYS.values()) {
            Map<String, List<Map<Integer, Double>>> groupData = new LinkedHashMap<>();
            Map<String, List<FinalMetrics>> groupMetrics = new LinkedHashMap<>();

            for (String algo : ALGORITHMS) {
                List<Map<Integer, Double>> runsList = new ArrayList<>();
                for (int i = 0; i < numRuns; i++) {
                    runsList.add(new HashMap<>());
                }
                groupData.put(algo, runsList);
            }

            String[] allAlgosForMetrics = {"Random Search (Baseline)", "Simulated Annealing (SA)", "Particle Swarm (PSO)", "Genetic Algorithm (GA)", "Ant Colony (ACO)", "Advanced SA (UpdateSA)"};
            for (String algo : allAlgosForMetrics) {
                groupMetrics.put(algo, new ArrayList<>());
            }

            groupedRunsData.put(groupKey, groupData);
            groupedRunsMetrics.put(groupKey, groupMetrics);
        }
    }

    private static void parseLogFile(String logFilePath, int runIndex) throws IOException {
        List<String> lines = new ArrayList<>();
        // 明确使用UTF-8编码读取，避免因系统默认编码不同导致的乱码问题
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath, StandardCharsets.UTF_8))) {
            reader.lines().forEach(lines::add);
        }

        String currentExperimentKey = null;
        String currentAlgorithm = null;

        for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
            String line = lines.get(lineNum);

            // 更新当前实验组的键
            for(Map.Entry<String, String> entry : EXPERIMENT_KEYS.entrySet()){
                if(line.contains(entry.getKey())){
                    currentExperimentKey = entry.getValue();
                    break;
                }
            }

            if (currentExperimentKey == null) continue;

            // 更新当前算法
            if (line.contains("--- ") && (line.contains("Running") || line.contains("Finished"))) {
                if (line.contains("Simulated Annealing (SA)")) currentAlgorithm = "Simulated Annealing (SA)";
                else if (line.contains("Particle Swarm Optimization (PSO)")) currentAlgorithm = "Particle Swarm (PSO)";
                else if (line.contains("Genetic Algorithm (GA)")) currentAlgorithm = "Genetic Algorithm (GA)";
                else if (line.contains("Ant Colony Optimization (ACO)")) currentAlgorithm = "Ant Colony (ACO)";
                else if (line.contains("Advanced Simulated Annealing (UpdateSA)")) currentAlgorithm = "Advanced SA (UpdateSA)";
                else if (line.contains("Random Search (to set Baseline)")) currentAlgorithm = "Random Search (Baseline)";
            }

            if (currentAlgorithm != null) {
                // 解析逐代数据
                if (line.trim().startsWith("Gen ") && line.contains("Improvement =")) {
                    Map<String, List<Map<Integer, Double>>> currentGroupData = groupedRunsData.get(currentExperimentKey);
                    if (currentGroupData != null && currentGroupData.containsKey(currentAlgorithm)) {
                        try {
                            String valueStr = line.split("Improvement =")[1].trim();
                            double improvement = Double.parseDouble(valueStr);
                            Map<Integer, Double> runData = currentGroupData.get(currentAlgorithm).get(runIndex);
                            runData.put(runData.size() + 1, improvement);
                        } catch (Exception e) { /* Ignore parse error */ }
                    }
                }

                // 解析性能指标
                if (line.contains("---- 与最高分对应的系统性能统计 ----")) {
                    FinalMetrics metrics = new FinalMetrics();
                    String owner = findBlockOwner(lines, lineNum);
                    Map<String, List<FinalMetrics>> currentGroupMetrics = groupedRunsMetrics.get(currentExperimentKey);
                    if (owner != null && currentGroupMetrics != null && currentGroupMetrics.containsKey(owner)) {
                        for (int i = 1; i <= 6; i++) {
                            if (lineNum + i < lines.size()) {
                                String metricLine = lines.get(lineNum + i);
                                try {
                                    String[] parts = metricLine.split(":");
                                    if (parts.length > 1) {
                                        String valueStr = parts[1].trim().split(" ")[0];
                                        double value = Double.parseDouble(valueStr);
                                        if (metricLine.contains("任务完成率")) metrics.completionRate = value;
                                        else if (metricLine.contains("全局本地缓存命中率")) metrics.localCacheHitRate = value;
                                        else if (metricLine.contains("全局边缘云命中率")) metrics.edgeCloudHitRate = value;
                                        else if (metricLine.contains("全局远端访问率")) metrics.remoteAccessRate = value;
                                        else if (metricLine.contains("ms")) metrics.avgLatency = value;
                                    }
                                } catch (Exception e) { /* ignore parse error */ }
                            }
                        }
                        currentGroupMetrics.get(owner).add(metrics);
                    }
                }
            }
        }
    }

    private static String findBlockOwner(List<String> lines, int currentIndex) {
        for (int i = currentIndex; i >= 0; i--) {
            String line = lines.get(i);
            if (line.contains("=== SA with GA-Rescue Finished ===")) return "Advanced SA (UpdateSA)";
            if (line.contains("=== SA (Standard Version) Finished ===")) return "Simulated Annealing (SA)";
            if (line.contains("=== PSO Finished ===")) return "Particle Swarm (PSO)";
            if (line.contains("=== GA Finished ===")) return "Genetic Algorithm (GA)";
            if (line.contains("=== ACO Finished ===")) return "Ant Colony (ACO)";
            if (line.contains("=== Single Random Attempt Finished (Baseline) ===")) return "Random Search (Baseline)";
        }
        return null;
    }

    private static void generateGroupedCsvOutputs() {
        for (String groupKey : EXPERIMENT_KEYS.values()) {
            System.out.println("\n\n========================================================================");
            System.out.println("<<<<<<<<<< RESULTS FOR GROUP: " + groupKey.replace("_", " ") + " >>>>>>>>>>");
            System.out.println("========================================================================");
            Map<String, List<Map<Integer, Double>>> dataForGroup = groupedRunsData.get(groupKey);
            Map<String, List<FinalMetrics>> metricsForGroup = groupedRunsMetrics.get(groupKey);
            if (dataForGroup == null || metricsForGroup == null) {
                System.out.println("No data found for this group.");
                continue;
            }
            System.out.println("\n--- 1. Average Improvement per Generation (Non-padded, Monotonic) ---");
            System.out.println(generateGenerationalImprovementCSV(dataForGroup));
            System.out.println("\n--- 2 & 3. Final Average Improvement and Standard Deviation (CSV Format) ---");
            System.out.println(generateFinalStatsCSV(dataForGroup));
            System.out.println("\n--- 4. Average Performance Metrics (CSV Format) ---");
            System.out.println(generatePerformanceMetricsCSV(metricsForGroup));
        }
    }

    private static String generateGenerationalImprovementCSV(Map<String, List<Map<Integer, Double>>> allRunsData) {
        StringBuilder csv = new StringBuilder("Generation");
        for (String algo : ALGORITHMS) {
            csv.append(",").append(algo.replace(" ", "_"));
        }
        csv.append("\n");
        int maxGen = 0;
        for (List<Map<Integer, Double>> runs : allRunsData.values()) {
            for (Map<Integer, Double> runData : runs) {
                if (!runData.isEmpty()) {
                    maxGen = Math.max(maxGen, runData.keySet().stream().max(Integer::compareTo).orElse(0));
                }
            }
        }
        maxGen = Math.min(maxGen, MAX_GENERATIONS);
        for (int gen = 1; gen <= maxGen; gen++) {
            csv.append(gen);
            for (String algo : ALGORITHMS) {
                double sumOfMaxes = 0.0;
                int validRuns = 0;
                List<Map<Integer, Double>> runsForAlgo = allRunsData.get(algo);
                if (runsForAlgo != null) {
                    for (Map<Integer, Double> runData : runsForAlgo) {
                        if (runData.isEmpty()) continue;
                        double maxImprovementForRun = 0.0;
                        for (int i = 1; i <= gen; i++) {
                            maxImprovementForRun = Math.max(maxImprovementForRun, runData.getOrDefault(i, 0.0));
                        }
                        sumOfMaxes += maxImprovementForRun;
                        validRuns++;
                    }
                }
                if (validRuns > 0) {
                    csv.append(String.format(",%.5f", sumOfMaxes / validRuns));
                } else {
                    csv.append(",0.0");
                }
            }
            csv.append("\n");
        }
        return csv.toString();
    }

    private static String generateFinalStatsCSV(Map<String, List<Map<Integer, Double>>> allRunsData) {
        StringBuilder csv = new StringBuilder("Algorithm,Average_Improvement,Standard_Deviation\n");
        for (String algo : ALGORITHMS) {
            List<Map<Integer, Double>> runsForAlgo = allRunsData.get(algo);
            if (runsForAlgo == null) continue;
            List<Double> finalImprovements = new ArrayList<>();
            for (Map<Integer, Double> runData : runsForAlgo) {
                if (runData.isEmpty()) continue;
                finalImprovements.add(Collections.max(runData.values()));
            }
            int numItems = finalImprovements.size();
            if (numItems == 0) continue;
            double sum = finalImprovements.stream().mapToDouble(Double::doubleValue).sum();
            double average = sum / numItems;
            if (numItems <= 1) {
                csv.append(String.format("%s,%.5f,0.00000\n", algo.replace(" ", "_"), average));
                continue;
            }
            double stdDevSum = finalImprovements.stream().mapToDouble(i -> Math.pow(i - average, 2)).sum();
            double stdDev = Math.sqrt(stdDevSum / (numItems - 1));
            csv.append(String.format("%s,%.5f,%.5f\n", algo.replace(" ", "_"), average, stdDev));
        }
        return csv.toString();
    }

    private static String generatePerformanceMetricsCSV(Map<String, List<FinalMetrics>> allRunsMetrics) {
        StringBuilder csv = new StringBuilder("Algorithm,Avg_Completion_Rate,Avg_Local_Cache_Hit_Rate,Avg_Edge_Cloud_Hit_Rate,Avg_Remote_Access_Rate,Avg_Task_Latency_ms\n");
        String[] allAlgosForMetrics = {"Random Search (Baseline)", "Simulated Annealing (SA)", "Particle Swarm (PSO)", "Genetic Algorithm (GA)", "Ant Colony (ACO)", "Advanced SA (UpdateSA)"};
        for (String algo : allAlgosForMetrics) {
            List<FinalMetrics> metricsList = allRunsMetrics.get(algo);
            if (metricsList == null || metricsList.isEmpty()) continue;
            int numItems = metricsList.size();
            double totalCompletion = 0, totalLocalHit = 0, totalEdgeHit = 0, totalRemote = 0, totalLatency = 0;
            for (FinalMetrics metrics : metricsList) {
                totalCompletion += metrics.completionRate;
                totalLocalHit += metrics.localCacheHitRate;
                totalEdgeHit += metrics.edgeCloudHitRate;
                totalRemote += metrics.remoteAccessRate;
                totalLatency += metrics.avgLatency;
            }
            if (numItems > 0) {
                csv.append(String.format("%s,%.4f,%.4f,%.4f,%.4f,%.4f\n",
                        algo.replace(" ", "_"),
                        totalCompletion / numItems, totalLocalHit / numItems, totalEdgeHit / numItems,
                        totalRemote / numItems, totalLatency / numItems));
            }
        }
        return csv.toString();
    }
}