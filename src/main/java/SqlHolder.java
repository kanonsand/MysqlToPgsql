import java.util.List;

public class SqlHolder {
    private List<String> dropList;
    private List<String> createList;
    private List<String> rawMysqlInsert;

    public List<String> getDropList() {
        return dropList;
    }

    public SqlHolder setDropList(List<String> dropList) {
        this.dropList = dropList;
        return this;
    }

    public List<String> getCreateList() {
        return createList;
    }

    public SqlHolder setCreateList(List<String> createList) {
        this.createList = createList;
        return this;
    }

    public List<String> getRawMysqlInsert() {
        return rawMysqlInsert;
    }

    public SqlHolder setRawMysqlInsert(List<String> rawMysqlInsert) {
        this.rawMysqlInsert = rawMysqlInsert;
        return this;
    }
}
