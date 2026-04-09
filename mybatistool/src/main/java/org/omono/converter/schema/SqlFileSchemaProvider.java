package org.omono.converter.schema;

import org.omono.converter.schema.exception.SchemaLoadException;
import org.omono.converter.schema.loader.MySqlSchemaLoader;
import org.omono.converter.schema.loader.PostgresSchemaLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 SQL 文件的 Schema 提供者
 * 从 MySQL 和 PostgreSQL DDL 文件加载表结构定义
 */
public class SqlFileSchemaProvider implements SchemaProvider {
    
    private final String mysqlDdl;
    private final String postgresDdl;
    private final boolean isContent;  // true if strings are SQL content, false if file paths
    
    /**
     * Constructor with file paths
     * @param mysqlDdlPath path to MySQL DDL file
     * @param postgresDdlPath path to PostgreSQL DDL file
     */
    public SqlFileSchemaProvider(String mysqlDdlPath, String postgresDdlPath) {
        this.mysqlDdl = mysqlDdlPath;
        this.postgresDdl = postgresDdlPath;
        this.isContent = false;
    }
    
    /**
     * Constructor with SQL content
     * @param mysqlDdlContent MySQL DDL SQL content
     * @param postgresDdlContent PostgreSQL DDL SQL content
     * @param isContent must be true to indicate these are SQL content strings
     */
    public SqlFileSchemaProvider(String mysqlDdlContent, String postgresDdlContent, boolean isContent) {
        this.mysqlDdl = mysqlDdlContent;
        this.postgresDdl = postgresDdlContent;
        this.isContent = isContent;
    }
    
    /**
     * Create provider from SQL content strings
     * @param mysqlDdlContent MySQL DDL SQL content
     * @param postgresDdlContent PostgreSQL DDL SQL content
     * @return SqlFileSchemaProvider instance
     */
    public static SqlFileSchemaProvider fromContent(String mysqlDdlContent, String postgresDdlContent) {
        return new SqlFileSchemaProvider(mysqlDdlContent, postgresDdlContent, true);
    }
    
    @Override
    public Map<String, TableMapping> load() throws SchemaLoadException {
        // 加载 MySQL 表结构
        Map<String, List<ColumnDefinition>> mysqlSchemas = isContent 
                ? MySqlSchemaLoader.parse(mysqlDdl)
                : MySqlSchemaLoader.loadFromFile(mysqlDdl);
        
        // 加载 PostgreSQL 表结构
        Map<String, List<ColumnDefinition>> pgSchemas = isContent
                ? PostgresSchemaLoader.parse(postgresDdl)
                : PostgresSchemaLoader.loadFromFile(postgresDdl);
        
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
                
                // Auto-detect column name mappings based on ordinal position
                detectColumnMappings(mapping, mysqlColumns, pgColumns);
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
    
    /**
     * Auto-detect column name mappings based on ordinal position.
     * When MySQL column name differs from PostgreSQL column name at the same position,
     * a mapping is created.
     */
    private void detectColumnMappings(TableMapping mapping, 
                                       List<ColumnDefinition> mysqlColumns, 
                                       List<ColumnDefinition> pgColumns) {
        int minSize = Math.min(mysqlColumns.size(), pgColumns.size());
        String tableName = mapping.getTableName();
        
        for (int i = 0; i < minSize; i++) {
            ColumnDefinition mysqlCol = mysqlColumns.get(i);
            ColumnDefinition pgCol = pgColumns.get(i);
            
            String mysqlName = mysqlCol.getName().toLowerCase();
            String pgName = pgCol.getName().toLowerCase();
            
            // If names differ at the same ordinal position, create mapping
            if (!mysqlName.equals(pgName)) {
                mapping.addColumnMapping(mysqlCol.getName(), pgCol.getName());
                System.out.println("Detected column mapping: " + tableName + "." + 
                        mysqlCol.getName() + " -> " + pgCol.getName());
            }
        }
    }
    
    @Override
    public String getSourceDescription() {
        return "MySQL DDL: " + mysqlDdl + ", PostgreSQL DDL: " + postgresDdl;
    }
}
