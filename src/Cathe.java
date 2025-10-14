import java.util.*;

class Cathe {
    long capacity;
    long currentSize = 0;
    Map<String, List<DataItem>> dataMap = new HashMap<>();
    protected Cathe lowerLevel = null;

    public Cathe(long capacity) { this.capacity = capacity; }
    public void bindLowerLevel(Cathe lower) { this.lowerLevel = lower; }
    public long getCurrentSize() { return currentSize; }

    public DataItem get(String type, int id, long currentTime) {
        List<DataItem> list = dataMap.get(type);
        if (list == null) return null;

        // --- 使用新的辅助构造函数进行查找 ---
        int idx = Collections.binarySearch(list, new DataItem(id), (a, b) -> a.id - b.id);
        if (idx >= 0) {
            DataItem item = list.get(idx);
            item.updateAccess(currentTime); // 触发 frequency 增加
            return item;
        }
        return null;
    }

    public void put(DataItem item, long currentTime) {
        if (item.size > this.capacity) {
            degrade(item, currentTime);
            return;
        }

        List<DataItem> list = dataMap.computeIfAbsent(item.type, k -> new ArrayList<>());

        int existingIdx = Collections.binarySearch(list, item, (a, b) -> a.id - b.id);
        if (existingIdx >= 0) {
            list.get(existingIdx).updateAccess(currentTime); // 触发 frequency 增加
            return;
        }

        while (currentSize + item.size > capacity) {
            if (!evictOne(currentTime)) {
                return;
            }
        }

        list = dataMap.computeIfAbsent(item.type, k -> new ArrayList<>());
        int insertionIdx = Collections.binarySearch(list, item, (a, b) -> a.id - b.id);

        if(insertionIdx >= 0) {
            list.get(insertionIdx).updateAccess(currentTime);
            return;
        }

        insertionIdx = -insertionIdx - 1;

        list.add(insertionIdx, item);
        currentSize += item.size;
    }

    protected boolean evictOne(long currentTime) {
        DataItem toEvict = null;
        double minScore = Double.MAX_VALUE;
        String typeOfToEvict = null;

        for (Map.Entry<String, List<DataItem>> entry : dataMap.entrySet()) {
            for (DataItem item : entry.getValue()) {
                double score = item.getCacheScore(currentTime);
                if (score < minScore) {
                    minScore = score;
                    toEvict = item;
                    typeOfToEvict = entry.getKey();
                }
            }
        }

        if (toEvict != null) {
            List<DataItem> list = dataMap.get(typeOfToEvict);
            if (list != null && list.remove(toEvict)) {
                currentSize -= toEvict.size;
                degrade(toEvict, currentTime);
                return true;
            }
        }
        return false;
    }

    protected void degrade(DataItem item, long currentTime) { if (lowerLevel != null) { lowerLevel.put(item, currentTime); } }
    public void clear() { dataMap.clear(); currentSize = 0; }
}

class LocalCache extends Cathe { public LocalCache(long c) { super(c); } }
class CloudCache extends Cathe { public CloudCache(long c) { super(c); } }
class RemoteCloud extends Cathe { public RemoteCloud() { super(Long.MAX_VALUE); } }