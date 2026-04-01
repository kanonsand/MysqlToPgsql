package converter.schema;

import java.util.Map;

/**
 * Schema 数据提供者接口
 * 支持多种来源加载表结构定义
 */
public interface SchemaProvider {
    
    /**
     * 加载并返回表映射
     * @return 表名 -> TableMapping
     * @throws SchemaLoadException 加载失败时抛出
     */
    Map<String, TableMapping> load() throws SchemaLoadException;
    
    /**
     * 获取来源描述（用于日志）
     * @return 来源描述
     */
    String getSourceDescription();
}
