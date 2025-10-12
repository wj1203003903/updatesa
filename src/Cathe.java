import java.util.*;

// �洢�࣬��Ϊ��ͬ���𻺴�
class Cathe {
    long capacity; // ������λ: B (�ֽ�)
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

        // �����Ŀ�Ƿ��Ѵ��ڡ�������ڣ�����²����ء�
        int existingIdx = Collections.binarySearch(list, item, (a, b) -> a.id - b.id);
        if (existingIdx >= 0) {
            list.get(existingIdx).updateAccess(currentTime);
            return;
        }

        // --- �������� ---
        // ���� 1: ����ִ�����б�Ҫ�����������ȷ�����㹻�Ŀռ䡣
        while (currentSize + item.size > capacity) {
            if (!evictOne(currentTime)) {
                // ����޷��ڳ��ռ䣨���磬��Ϊ��Ŀ̫��򻺴�Ϊ�գ�����������롣
                return;
            }
        }

        // ���� 2: �����п����޸��б��С�Ĳ�����ɺ��ټ������������
        // ��ʱ���б�״̬�����յģ�������������ǰ�ȫ�ġ�
        int insertionIdx = Collections.binarySearch(list, item, (a, b) -> a.id - b.id);
        // ����������ڼ䣬ͬһ�� item ����ĳ�ַ�ʽ��ӣ���̫���ܵ�Ϊ�˽�׳�ԣ�����ֱ�ӷ��ء�
        if(insertionIdx >= 0) {
            return;
        }

        // �� binarySearch �ĸ�������ֵת��Ϊ��ȷ�Ĳ���㡣
        insertionIdx = -insertionIdx - 1;

        // ���� 3: ʹ�øոռ�����ġ����ڿ϶���Ч������������ӡ�
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