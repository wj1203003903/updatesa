import java.util.Random;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        final int dataNum = 2000;

        // ������λ���ֽ�(B): 800KB for local, 1600KB for edge
        long localCapacity = 800L * 1024L;
        long edgeCapacity = 1600L * 1024L;
        DataManager baseDM = new DataManager(localCapacity, edgeCapacity);

        DataItem[] testData = new DataItem[dataNum];
        long baseTime = System.currentTimeMillis();

        System.out.println("��������ģ�����ݼ�...");
        for (int i = 0; i < dataNum; i++) {
            testData[i] = baseDM.generateDataItem(i, baseTime);
            // �����������䵽��ʱ����ӵ�Զ����
            baseDM.addDataToRemote(testData[i], testData[i].arrivalTime);
        }
        System.out.println("���ݼ�������ϣ��� " + dataNum + " �����ݡ�");

        SA sa = new SA(testData, baseDM);

        System.out.println("\n=============================================");
        System.out.println("=== ��ʼ�¼�����ģ���µ�Ȩ���Ż� ===");
        System.out.println("=============================================");

        double bestScore = sa.run();

        System.out.println("\nSA �㷨�ҵ�������ϵͳ����Ϊ: " + bestScore);
    }
}