import java.util.Random;

public class Main {

    // --- 辅助方法，用于生成不同分布的随机数 (已包含泊松分布大Lambda值的修复) ---

    public static int getPoissonRandom(double lambda, Random random) {
        if (lambda >= 40.0) {
            double mean = lambda;
            double stdDev = Math.sqrt(lambda);
            double normalSample;
            int poissonSample;
            do {
                normalSample = random.nextGaussian() * stdDev + mean;
                poissonSample = (int) Math.round(normalSample);
            } while (poissonSample < 0);
            return poissonSample;
        }
        int k = 0;
        double p = 1.0;
        double expLambda = Math.exp(-lambda);
        do {
            p *= random.nextDouble();
            k++;
        } while (p >= expLambda);
        return k - 1;
    }

    public static double getExponentialRandom(double lambda, Random random) {
        return -Math.log(1.0 - random.nextDouble()) / lambda;
    }

    public static double getLogNormalRandom(double mu, double sigma, Random random) {
        return Math.exp(mu + sigma * random.nextGaussian());
    }

    public static double getWeibullRandom(double shape, double scale, Random random) {
        return scale * Math.pow(-Math.log(1.0 - random.nextDouble()), 1.0 / shape);
    }


    public static void main(String[] args) {
        // --- 公共初始化部分 ---
        final int datanum = 5000;
        DataManager baseDM = new DataManager(800, 1600);
        DataItem[] testData = new DataItem[datanum];
        Random random = new Random();

        // --- 算法实例初始化 ---
        Nomal nomal = new Nomal(testData, baseDM);
        GA ga = new GA(testData, baseDM);
        PSO pso = new PSO(testData, baseDM);
        ACO aco = new ACO(testData, baseDM);
        SA sa = new SA(testData, baseDM);
        UpdateSA updateSA = new UpdateSA(testData, baseDM);

        double currentScore;
        double nomalBestScore;


        System.out.println("=========================================");
        System.out.println("=== 1. 随机分布 (Uniform Distribution) ===");
        System.out.println("=========================================");
        for (int i = 0; i < datanum; i++) {
            testData[i] = baseDM.generateDataItem(random.nextInt(datanum));
            baseDM.addDataToRemote(testData[i]);
        }
        System.out.println("-------Nomal-------");
        nomalBestScore = nomal.run();
        System.out.println("Nomal Best Score: " + nomalBestScore);

        System.out.println("-------GA-------");
        currentScore = ga.run();
        System.out.println("GA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------PSO-------");
        currentScore = pso.run();
        System.out.println("PSO Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------ACO-------");
        currentScore = aco.run();
        System.out.println("ACO Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------SA-------");
        currentScore = sa.run();
        System.out.println("SA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------UpdateSA-------");
        currentScore = updateSA.run();
        System.out.println("UpdateSA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));


        System.out.println("\n=========================================");
        System.out.println("=== 2. 正态分布 (Normal Distribution) ===");
        System.out.println("=========================================");
        for (int i = 0; i < datanum; i++) {
            int value;
            do {
                value = (int) Math.round(random.nextGaussian() * 800 + 2500);
            } while (value < 1 || value > 5000);
            testData[i] = baseDM.generateDataItem(value);
            baseDM.addDataToRemote(testData[i]);
        }
        System.out.println("-------Nomal-------");
        nomalBestScore = nomal.run();
        System.out.println("Nomal Best Score: " + nomalBestScore);

        System.out.println("-------GA-------");
        currentScore = ga.run();
        System.out.println("GA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------PSO-------");
        currentScore = pso.run();
        System.out.println("PSO Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------ACO-------");
        currentScore = aco.run();
        System.out.println("ACO Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------SA-------");
        currentScore = sa.run();
        System.out.println("SA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------UpdateSA-------");
        currentScore = updateSA.run();
        System.out.println("UpdateSA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));


        System.out.println("\n==========================================");
        System.out.println("=== 3. 泊松分布 (Poisson Distribution) ===");
        System.out.println("==========================================");
        double lambda_poisson = 500;
        for (int i = 0; i < datanum; i++) {
            int value = getPoissonRandom(lambda_poisson, random);
            testData[i] = baseDM.generateDataItem(value);
            baseDM.addDataToRemote(testData[i]);
        }
        System.out.println("-------Nomal-------");
        nomalBestScore = nomal.run();
        System.out.println("Nomal Best Score: " + nomalBestScore);

        System.out.println("-------GA-------");
        currentScore = ga.run();
        System.out.println("GA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------PSO-------");
        currentScore = pso.run();
        System.out.println("PSO Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------ACO-------");
        currentScore = aco.run();
        System.out.println("ACO Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------SA-------");
        currentScore = sa.run();
        System.out.println("SA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------UpdateSA-------");
        currentScore = updateSA.run();
        System.out.println("UpdateSA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));


        System.out.println("\n=============================================");
        System.out.println("=== 4. 指数分布 (Exponential Distribution) ===");
        System.out.println("=============================================");
        double mean_interval = 1000;
        double lambda_exp = 1.0 / mean_interval;
        for (int i = 0; i < datanum; i++) {
            int value = (int) Math.round(getExponentialRandom(lambda_exp, random));
            testData[i] = baseDM.generateDataItem(value);
            baseDM.addDataToRemote(testData[i]);
        }
        System.out.println("-------Nomal-------");
        nomalBestScore = nomal.run();
        System.out.println("Nomal Best Score: " + nomalBestScore);

        System.out.println("-------GA-------");
        currentScore = ga.run();
        System.out.println("GA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------PSO-------");
        currentScore = pso.run();
        System.out.println("PSO Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------ACO-------");
        currentScore = aco.run();
        System.out.println("ACO Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------SA-------");
        currentScore = sa.run();
        System.out.println("SA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------UpdateSA-------");
        currentScore = updateSA.run();
        System.out.println("UpdateSA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));


        System.out.println("\n=================================================");
        System.out.println("=== 5. 对数正态分布 (Log-Normal Distribution) ===");
        System.out.println("=================================================");
        double mu_log = Math.log(1000);
        double sigma_log = 0.8;
        for (int i = 0; i < datanum; i++) {
            int value = (int) Math.round(getLogNormalRandom(mu_log, sigma_log, random));
            testData[i] = baseDM.generateDataItem(value);
            baseDM.addDataToRemote(testData[i]);
        }
        System.out.println("-------Nomal-------");
        nomalBestScore = nomal.run();
        System.out.println("Nomal Best Score: " + nomalBestScore);

        System.out.println("-------GA-------");
        currentScore = ga.run();
        System.out.println("GA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------PSO-------");
        currentScore = pso.run();
        System.out.println("PSO Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------ACO-------");
        currentScore = aco.run();
        System.out.println("ACO Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------SA-------");
        currentScore = sa.run();
        System.out.println("SA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------UpdateSA-------");
        currentScore = updateSA.run();
        System.out.println("UpdateSA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));


        System.out.println("\n===========================================");
        System.out.println("=== 6. 韦伯分布 (Weibull Distribution) ===");
        System.out.println("===========================================");
        double shape_weibull = 2.0;
        double scale_weibull = 1500;
        for (int i = 0; i < datanum; i++) {
            int value = (int) Math.round(getWeibullRandom(shape_weibull, scale_weibull, random));
            testData[i] = baseDM.generateDataItem(value);
            baseDM.addDataToRemote(testData[i]);
        }
        System.out.println("-------Nomal-------");
        nomalBestScore = nomal.run();
        System.out.println("Nomal Best Score: " + nomalBestScore);

        System.out.println("-------GA-------");
        currentScore = ga.run();
        System.out.println("GA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------PSO-------");
        currentScore = pso.run();
        System.out.println("PSO Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------ACO-------");
        currentScore = aco.run();
        System.out.println("ACO Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------SA-------");
        currentScore = sa.run();
        System.out.println("SA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));

        System.out.println("-------UpdateSA-------");
        currentScore = updateSA.run();
        System.out.println("UpdateSA Best Score: " + currentScore + ", Difference from Nomal (*100): " + ((currentScore - nomalBestScore) * 100));
    }
}