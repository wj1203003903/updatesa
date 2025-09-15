import java.util.List;

// 远程云缓存
class RemoteCloud extends Cathe {
    public RemoteCloud() {
        super(Integer.MAX_VALUE);
    }

    @Override
    protected void instead(List<DataItem> list, DataItem newItem, int idx) {
        // 不进行替换
    }

    @Override
    protected void degrade(DataItem item) {
        // 不进行降级
    }
}