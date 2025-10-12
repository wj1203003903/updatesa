import java.util.*;

// 存储类，分为不同级别缓存
class Cathe {
    long capacity; // 容量单位: B (字节)
    long currentSize = 0;
    Map<String, Map<Integer, List<DataItem>>> dataMap = new HashMap<>();
    protected Cathe lowerLevel = null;

    public Cathe(long capacity) {
        this.capacity = capacity;
    }

    public void bindLowerLevel(Cathe lower) {
        this.lowerLevel = lower;
    }

    public long getCurrentSize() {
        return currentSize;
    }

    public DataItem get(String type, int priority, int id, long currentTime) {
        Map<Integer, List<DataItem>> typeMap = dataMap.get(type);
        if (typeMap == null) return null;
        List<DataItem> list = typeMap.get(priority);
        if (list == null) return null;
        int idx = Collections.binarySearch(list, new DataItem(id, 0, type, 0, priority, 0, 0, 0), (a, b) -> a.id - b.id);
        if (idx >= 0) {
            DataItem item = list.get(idx);
            item.updateAccess(currentTime);
            return item;
        }
        return null;
    }

    public void put(DataItem item, long currentTime) {
        if (item.size > this.capacity) {
            degrade(item, currentTime);
            return;
        }

        Map<Integer, List<DataItem>> typeMap = dataMap.computeIfAbsent(item.type, k -> new HashMap<>());
        List<DataItem> list = typeMap.computeIfAbsent(item.Priority, k -> new ArrayList<>());

        // 检查项目是否已存在。如果存在，则更新并返回。
        int existingIdx = Collections.binarySearch(list, item, (a, b) -> a.id - b.id);
        if (existingIdx >= 0) {
            list.get(existingIdx).updateAccess(currentTime);
            return;
        }

        // --- 核心修正 ---
        // 步骤 1: 首先执行所有必要的驱逐操作，确保有足够的空间。
        while (currentSize + item.size > capacity) {
            if (!evictOne(currentTime)) {
                // 如果无法腾出空间（例如，因为项目太大或缓存为空），则放弃插入。
                return;
            }
        }

        // 步骤 2: 在所有可能修改列表大小的操作完成后，再计算插入索引。
        // 此时的列表状态是最终的，计算出的索引是安全的。
        int insertionIdx = Collections.binarySearch(list, item, (a, b) -> a.id - b.id);
        // 如果在驱逐期间，同一个 item 被以某种方式添加（不太可能但为了健壮性），则直接返回。
        if(insertionIdx >= 0) {
            return;
        }

        // 将 binarySearch 的负数返回值转换为正确的插入点。
        insertionIdx = -insertionIdx - 1;

        // 步骤 3: 使用刚刚计算出的、现在肯定有效的索引进行添加。
        list.add(insertionIdx, item);
        currentSize += item.size;
    }

    protected boolean evictOne(long currentTime) {
        DataItem toEvict = null;
        double minScore = Double.MAX_VALUE;
        for (Map<Integer, List<DataItem>> typeMap : dataMap.values()) {
            for (List<DataItem> list : typeMap.values()) {
                for (DataItem item : list) {
                    double score = item.getCacheScore(currentTime);
                    if (score < minScore) {
                        minScore = score;
                        toEvict = item;
                    }
                }
            }
        }
        if (toEvict != null) {
            List<DataItem> list = dataMap.get(toEvict.type).get(toEvict.Priority);
            if (list.remove(toEvict)) {
                currentSize -= toEvict.size;
                degrade(toEvict, currentTime);
                return true;
            }
        }
        return false;
    }

    protected void degrade(DataItem item, long currentTime) {
        if (lowerLevel != null) {
            lowerLevel.put(item, currentTime);
        }
    }

    public void clear() {
        dataMap.clear();
        currentSize = 0;
    }
}