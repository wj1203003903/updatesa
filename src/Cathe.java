import java.util.*;
// 数据结构
// 存储类，分为不同级别缓存
class Cathe {
    int capacity;
    Map<String, Map<Integer, List<DataItem>>> dataMap = new HashMap<>();
    protected Cathe lowerLevel = null;

    public Cathe(int capacity) {
        this.capacity = capacity;
    }

    // 绑定下一级缓存
    public void bindLowerLevel(Cathe lower) {
        this.lowerLevel = lower;
    }

    // 获取当前缓存中数据项的总数
    private int totalSize() {
        int size = 0;
        for (Map<Integer, List<DataItem>> map : dataMap.values()) {
            for (List<DataItem> list : map.values()) {
                size += list.size();
            }
        }
        return size;
    }

    // 获取数据
    public DataItem get(String type, int priority, int id) {
        Map<Integer, List<DataItem>> typeMap = dataMap.get(type);
        if (typeMap == null) return null;
        List<DataItem> list = typeMap.get(priority);
        if (list == null) return null;
        int idx = Collections.binarySearch(list, new DataItem(id, 0, type, 0, priority), (a, b) -> a.id - b.id);
        if (idx >= 0) {
            DataItem item = list.get(idx);
            item.updateAccess();
            return item;
        }
        return null;
    }

    // 插入数据
    public void put(DataItem item) {
        if (!dataMap.containsKey(item.type)) {
            dataMap.put(item.type, new HashMap<>());
        }

        Map<Integer, List<DataItem>> typeMap = dataMap.get(item.type);
        if (!typeMap.containsKey(item.Priority)) {
            typeMap.put(item.Priority, new ArrayList<>());
        }

        List<DataItem> list = typeMap.get(item.Priority);

        int idx = Collections.binarySearch(list, item, (a, b) -> a.id - b.id);
        if (idx >= 0) {
            list.get(idx).updateAccess();
            return;
        }
        idx = -idx - 1;
        if (totalSize() >= capacity) {
            instead(list, item, idx);
        } else {
            list.add(idx, item);
        }
    }

    // 替换数据
    protected void instead(List<DataItem> list, DataItem newItem, int idx) {
        int minIndex = -1;
        double minScore = Double.MAX_VALUE;

        // 查找评分最低的数据项
        for (int i = 0; i < list.size(); i++) {
            double score = list.get(i).score();
            if (score < minScore) {
                minScore = score;
                minIndex = i;
            }
        }

        if (minIndex != -1 && newItem.score() > minScore) {
            list.add(idx, newItem);
            DataItem evicted = list.remove(minIndex);
            degrade(evicted);
        }
    }

    protected void degrade(DataItem item) {
        if (lowerLevel != null) {
            lowerLevel.put(item);
        }
    }
}