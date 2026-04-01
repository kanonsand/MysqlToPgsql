package converter.schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 SQL 文件的 Schema 提供者
 * 从 MySQL 和 PostgreSQL DDL 文件加载表结构定义
 */
public class SqlFileSchemaProvider implements SchemaProvider {
    
    private final String mysqlDdlPath;
    private final String postgresDdlPath;
    
    public SqlFileSchemaProvider(String mysqlDdlPath, String postgresDdlPath) {
        this.mysqlDdlPath = mysqlDdlPath;
        this.postgresDdlPath = postgresDdlPath;
    }
    
    @Override
    public Map<String, TableMapping> load() throws SchemaLoadException {
        // 加载 MySQL 表结构
        Map<String, List<ColumnDefinition>> mysqlSchemas = MySqlSchemaLoader.loadFromFile(mysqlDdlPath);
        
        // 加载 PostgreSQL 表结构
        Map<String, List<ColumnDefinition>> pgSchemas = PostgresSchemaLoader.loadFromFile(postgresDdlPath);
        
        // 创建表映射
        Map<String, TableMapping> mappings = new HashMap<>();
        
        // 遍历 MySQL 表，创建映射
        for (Map.Entry<String, List<ColumnDefinition>> entry : mysqlSchemas.entrySet()) {
            String tableName = entry.getKey();
            List<ColumnDefinition> mysqlColumns = entry.getValue();
            
            TableMapping mapping = new TableMapping(tableName);
            mapping.setMysqlColumns(mysqlColumns);
            
            // 查找对应的 PostgreSQL 表结构
            List<ColumnDefinition> pgColumns = pgSchemas.get(tableName.toLowerCase());
            if (pgColumns != null) {
                mapping.setPgColumns(pgColumns);
            }
            
            mappings.put(tableName.toLowerCase(), mapping);
        }
        
        // 检查是否有 PostgreSQL 独有的表
        for (String pgTableName : pgSchemas.keySet()) {
            if (!mappings.containsKey(pgTableName.toLowerCase())) {
                // PostgreSQL 有但 MySQL 没有的表，也创建映射（可能不需要转换）
                TableMapping mapping = new TableMapping(pgTableName);
                mapping.setPgColumns(pgSchemas.get(pgTableName));
                mappings.put(pgTableName.toLowerCase(), mapping);
            }
        }
        
        return mappings;
    }
    
    @Override
    public String getSourceDescription() {
        return "MySQL DDL: " + mysqlDdlPath + ", PostgreSQL DDL: " + postgresDdlPath;
    }
}
