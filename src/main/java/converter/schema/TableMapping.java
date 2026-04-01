package converter.schema;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 表映射关系，包含 MySQL 和 PostgreSQL 的列定义映射
 */
public class TableMapping {
    
    private String tableName;                          // 表名
    private List<ColumnDefinition> mysqlColumns;       // MySQL 列定义（按原始顺序）
    private List<ColumnDefinition> pgColumns;          // PostgreSQL 列定义（按原始顺序）
    private Map<String, String> columnNameMapping;     // MySQL列名 -> PostgreSQL列名（可选映射）
    
    // 缓存：列名 -> 列定义
    private Map<String, ColumnDefinition> mysqlColumnMap;
    private Map<String, ColumnDefinition> pgColumnMap;
    
    public TableMapping() {
        this.mysqlColumns = new ArrayList<>();
        this.pgColumns = new ArrayList<>();
        this.columnNameMapping = new HashMap<>();
    }
    
    public TableMapping(String tableName) {
        this();
        this.tableName = tableName;
    }
    
    /**
     * 根据 MySQL 列名获取对应的 PostgreSQL 列名
     * @param mysqlColumnName MySQL 列名
     * @return PostgreSQL 列名
     */
    public String getPostgresColumnName(String mysqlColumnName) {
        // 优先使用自定义映射，否则使用相同列名
        return columnNameMapping.getOrDefault(mysqlColumnName, mysqlColumnName);
    }
    
    /**
     * 根据 MySQL 列顺序获取 PostgreSQL 列顺序
     * @param mysqlColumnNames MySQL 列名列表（按顺序）
     * @return PostgreSQL 列名列表（按对应顺序）
     */
    public List<String> getPostgresColumnOrder(List<String> mysqlColumnNames) {
        if (mysqlColumnNames == null || mysqlColumnNames.isEmpty()) {
            // 无列名时，返回 PostgreSQL 所有列的顺序
            return pgColumns.stream()
                    .map(ColumnDefinition::getName)
                    .collect(Collectors.toList());
        }
        
        return mysqlColumnNames.stream()
                .map(this::getPostgresColumnName)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取 MySQL 列顺序对应的 PostgreSQL 列序号
     * @param mysqlColumnNames MySQL 列名列表
     * @return PostgreSQL 列序号数组
     */
    public int[] getPostgresColumnOrdinals(List<String> mysqlColumnNames) {
        if (mysqlColumnNames == null || mysqlColumnNames.isEmpty()) {
            // 无列名时，假设按照 MySQL 列顺序插入，需要映射到 PostgreSQL 列序号
            int[] ordinals = new int[mysqlColumns.size()];
            for (int i = 0; i < mysqlColumns.size(); i++) {
                String mysqlColName = mysqlColumns.get(i).getName();
                String pgColName = getPostgresColumnName(mysqlColName);
                ordinals[i] = getPostgresColumnOrdinal(pgColName);
            }
            return ordinals;
        }
        
        int[] ordinals = new int[mysqlColumnNames.size()];
        for (int i = 0; i < mysqlColumnNames.size(); i++) {
            String pgColName = getPostgresColumnName(mysqlColumnNames.get(i));
            ordinals[i] = getPostgresColumnOrdinal(pgColName);
        }
        return ordinals;
    }
    
    /**
     * 获取 PostgreSQL 列的序号
     * @param pgColumnName PostgreSQL 列名
     * @return 列序号，不存在返回 -1
     */
    public int getPostgresColumnOrdinal(String pgColumnName) {
        ColumnDefinition col = getPgColumnMap().get(pgColumnName.toLowerCase());
        return col != null ? col.getOrdinal() : -1;
    }
    
    /**
     * 获取 MySQL 列的序号
     * @param mysqlColumnName MySQL 列名
     * @return 列序号，不存在返回 -1
     */
    public int getMySqlColumnOrdinal(String mysqlColumnName) {
        ColumnDefinition col = getMysqlColumnMap().get(mysqlColumnName.toLowerCase());
        return col != null ? col.getOrdinal() : -1;
    }
    
    /**
     * 检查列映射是否有效（strict mode）
     * @throws SchemaValidationException 如果验证失败
     */
    public void validate() throws SchemaValidationException {
        List<String> errors = new ArrayList<>();
        
        // 检查列数量是否一致
        if (mysqlColumns.size() != pgColumns.size()) {
            errors.add(String.format("Table '%s': column count mismatch - MySQL: %d, PostgreSQL: %d",
                    tableName, mysqlColumns.size(), pgColumns.size()));
        }
        
        // 检查所有 MySQL 列是否都能映射到 PostgreSQL 列
        for (ColumnDefinition mysqlCol : mysqlColumns) {
            String pgColName = getPostgresColumnName(mysqlCol.getName());
            if (!getPgColumnMap().containsKey(pgColName.toLowerCase())) {
                errors.add(String.format("Table '%s': MySQL column '%s' maps to non-existent PostgreSQL column '%s'",
                        tableName, mysqlCol.getName(), pgColName));
            }
        }
        
        // 检查自定义映射中的目标列是否存在
        for (Map.Entry<String, String> entry : columnNameMapping.entrySet()) {
            if (!getPgColumnMap().containsKey(entry.getValue().toLowerCase())) {
                errors.add(String.format("Table '%s': mapped column '%s' -> '%s' but '%s' does not exist in PostgreSQL",
                        tableName, entry.getKey(), entry.getValue(), entry.getValue()));
            }
        }
        
        if (!errors.isEmpty()) {
            throw new SchemaValidationException("Schema validation failed for table '" + tableName + "':\n" + 
                    String.join("\n", errors));
        }
    }
    
    // 内部方法：获取列名 -> 列定义映射
    private Map<String, ColumnDefinition> getMysqlColumnMap() {
        if (mysqlColumnMap == null) {
            mysqlColumnMap = new HashMap<>();
            for (ColumnDefinition col : mysqlColumns) {
                mysqlColumnMap.put(col.getName().toLowerCase(), col);
            }
        }
        return mysqlColumnMap;
    }
    
    private Map<String, ColumnDefinition> getPgColumnMap() {
        if (pgColumnMap == null) {
            pgColumnMap = new HashMap<>();
            for (ColumnDefinition col : pgColumns) {
                pgColumnMap.put(col.getName().toLowerCase(), col);
            }
        }
        return pgColumnMap;
    }
    
    // Getters and Setters
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public List<ColumnDefinition> getMysqlColumns() {
        return mysqlColumns;
    }
    
    public void setMysqlColumns(List<ColumnDefinition> mysqlColumns) {
        this.mysqlColumns = mysqlColumns;
        this.mysqlColumnMap = null; // 清除缓存
    }
    
    public List<ColumnDefinition> getPgColumns() {
        return pgColumns;
    }
    
    public void setPgColumns(List<ColumnDefinition> pgColumns) {
        this.pgColumns = pgColumns;
        this.pgColumnMap = null; // 清除缓存
    }
    
    public Map<String, String> getColumnNameMapping() {
        return columnNameMapping;
    }
    
    public void setColumnNameMapping(Map<String, String> columnNameMapping) {
        this.columnNameMapping = columnNameMapping;
    }
    
    public void addColumnMapping(String mysqlName, String pgName) {
        this.columnNameMapping.put(mysqlName, pgName);
    }
    
    @Override
    public String toString() {
        return "TableMapping{" +
                "tableName='" + tableName + '\'' +
                ", mysqlColumns=" + mysqlColumns.size() +
                ", pgColumns=" + pgColumns.size() +
                '}';
    }
}
