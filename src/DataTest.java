import java.util.*;




public class DataTest {
    public static double score(double[] w,DataItem[] testData,DataManager dataManager) {
     	dataManager.reset();
        DataItem.setWeights(w);
        for (DataItem item : testData) {
            dataManager.access(item.type, item.Priority, item.id);
        }
        return dataManager.getSystemScore();
        
    }
}
