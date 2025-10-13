import java.util.*;

class Cathe {
    long capacity;
    long currentSize = 0;
    // --- 确认数据结构：一个类型对应一个按ID排序的列表 ---
    Map<String, List<DataItem>> dataMap = new HashMap<>();
    protected Cathe lowerLevel = null;

    public Cathe(long capacity) { this.capacity = capacity; }
    public void bindLowerLevel(Cathe lower) { this.lowerLevel = lower; }
    public long getCurrentSize() { return currentSize; }

    // --- 修正 get 方法 ---
    // priority 参数虽然还在 DataManager.access 的调用栈中，但在这里不再使用
    public DataItem get(String type, int priority, int id, long currentTime) {
        List<DataItem> list = dataMap.get(type);
        if (list == null) return null;

        // 使用正确的构造函数 (已移除 priority)
        int idx = Collections.binarySearch(list, new DataItem(id, 0, type, 0, 0, 0, 0), (a, b) -> a.id - b.id);
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

        // 获取对应类型的列表
        List<DataItem> list = dataMap.computeIfAbsent(item.type, k -> new ArrayList<>());

        int existingIdx = Collections.binarySearch(list, item, (a, b) -> a.id - b.id);
        if (existingIdx >= 0) {
            list.get(existingIdx).updateAccess(currentTime);
            return;
        }

        while (currentSize + item.size > capacity) {
            if (!evictOne(currentTime)) {
                return;
            }
        }

        // --- 核心修正 ---
        // 在驱逐操作完成后，*必须*重新获取可能已改变的列表，并重新计算索引
        // 因为 evictOne 可能会删除元素，导致 list 的引用或大小发生变化
        list = dataMap.computeIfAbsent(item.type, k -> new ArrayList<>());
        int insertionIdx = Collections.binarySearch(list, item, (a, b) -> a.id - b.id);

        // 健壮性检查：如果在 evictOne 过程中，有其他线程添加了该项
        if(insertionIdx >= 0) {
            // 项目已存在，更新访问时间
            list.get(insertionIdx).updateAccess(currentTime);
            return;
        }

        insertionIdx = -insertionIdx - 1;

        // 插入新项
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

// 辅助类无需修改
class LocalCache extends Cathe { public LocalCache(long c) { super(c); } }
class CloudCache extends Cathe { public CloudCache(long c) { super(c); } }
class RemoteCloud extends Cathe { public RemoteCloud() { super(Long.MAX_VALUE); } }