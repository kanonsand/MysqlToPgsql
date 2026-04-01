package converter.schema;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Schema 注册表，管理所有表映射关系
 */
public class SchemaRegistry {
    
    private final Map<String, TableMapping> tableMappings;
    private boolean strictMode = false;
    
    public SchemaRegistry() {
        this.tableMappings = new ConcurrentHashMap<>();
    }
    
    public SchemaRegistry(boolean strictMode) {
        this();
        this.strictMode = strictMode;
    }
    
    /**
     * 从 SchemaProvider 加载表映射
     * @param provider Schema 提供者
     * @throws SchemaLoadException 加载失败
     * @throws SchemaValidationException strict 模式下验证失败
     */
    public void load(SchemaProvider provider) throws SchemaLoadException, SchemaValidationException {
        Map<String, TableMapping> mappings = provider.load();
        registerAll(mappings);
    }
    
    /**
     * 注册单个表映射
     * @param mapping 表映射
     * @throws SchemaValidationException strict 模式下验证失败
     */
    public void register(TableMapping mapping) throws SchemaValidationException {
        if (strictMode) {
            mapping.validate();
        }
        tableMappings.put(mapping.getTableName().toLowerCase(), mapping);
    }
    
    /**
     * 批量注册表映射
     * @param mappings 表映射集合
     * @throws SchemaValidationException strict 模式下验证失败
     */
    public void registerAll(Map<String, TableMapping> mappings) throws SchemaValidationException {
        for (Map.Entry<String, TableMapping> entry : mappings.entrySet()) {
            register(entry.getValue());
        }
    }
    
    /**
     * 获取表映射
     * @param tableName 表名
     * @return 表映射，不存在返回 null
     */
    public TableMapping getTableMapping(String tableName) {
        if (tableName == null) {
            return null;
        }
        // 尝试多种表名格式
        TableMapping mapping = tableMappings.get(tableName.toLowerCase());
        if (mapping == null) {
            // 移除引号再尝试
            String cleanName = tableName.replace("\"", "").replace("`", "");
            mapping = tableMappings.get(cleanName.toLowerCase());
        }
        return mapping;
    }
    
    /**
     * 检查表映射是否存在
     * @param tableName 表名
     * @return 是否存在
     */
    public boolean hasTableMapping(String tableName) {
        return getTableMapping(tableName) != null;
    }
    
    /**
     * 获取所有表名
     * @return 表名集合
     */
    public java.util.Set<String> getTableNames() {
        return tableMappings.keySet();
    }
    
    /**
     * 获取表映射数量
     * @return 数量
     */
    public int size() {
        return tableMappings.size();
    }
    
    /**
     * 清空所有表映射
     */
    public void clear() {
        tableMappings.clear();
    }
    
    /**
     * 验证所有表映射（strict mode）
     * @throws SchemaValidationException 验证失败
     */
    public void validateAll() throws SchemaValidationException {
        for (TableMapping mapping : tableMappings.values()) {
            mapping.validate();
        }
    }
    
    // Getters and Setters
    
    public boolean isStrictMode() {
        return strictMode;
    }
    
    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }
}
