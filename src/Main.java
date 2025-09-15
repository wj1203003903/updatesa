import java.util.Random;

public class Main {
    public static void main(String[] args) {
    	System.out.println("=== 随机分布 === ");
        // 测试集初始化
    	final int datanum=5000;
        DataManager baseDM=new DataManager(800, 1600);
        DataItem[] testData = new DataItem[datanum];
    	Random random=new Random();
        for (int i = 0; i <datanum; i++) {
            testData[i] = baseDM.generateDataItem(random.nextInt(datanum));
            baseDM.addDataToRemote(testData[i]);
    	}  

        System.out.println("-------GA-------");
        GA ga = new GA(testData,baseDM);
        ga.run(); 
        System.out.println("-------PSO-------");
        PSO pso = new PSO(testData,baseDM);
        pso.run();
        System.out.println("-------ACO-------");
        ACO aco = new ACO(testData,baseDM);
        aco.run();
        System.out.println("-------SA-------");
        SA sa = new SA(testData, baseDM);
        sa.run();
        System.out.println("-------UpdateSA-------");
        UpdateSA updateSA = new UpdateSA(testData, baseDM);
        updateSA.run();

        System.out.println("=== 正态分布 === ");
        for (int i = 0; i <datanum; i++) {
    	    int value;
    	    do {
    	        value = (int) Math.round(random.nextGaussian() * 800 + 2500);
    	    } while (value < 1 || value > 3000);
        testData[i] = baseDM.generateDataItem(value);
        baseDM.addDataToRemote(testData[i]);
	}  

    System.out.println("-------GA-------");
    ga.run(); 
    System.out.println("-------PSO-------");
    pso.run();
    System.out.println("-------ACO-------");
    aco.run();
    System.out.println("-------SA-------");
    sa.run();
    System.out.println("-------UpdateSA-------");
    updateSA.run();
    }       
    }       
