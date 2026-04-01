package converter.schema;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL DDL 解析器
 * 从 CREATE TABLE 语句中提取列定义
 */
public class MySqlSchemaLoader extends AbstractSchemaLoader {
    
    /**
     * 从 SQL 文件加载 MySQL 表结构
     * @param filePath SQL 文件路径
     * @return 表名 -> 列定义列表
     * @throws SchemaLoadException 加载失败
     */
    public static Map<String, List<ColumnDefinition>> loadFromFile(String filePath) throws SchemaLoadException {
        try {
            String sql = new String(Files.readAllBytes(Paths.get(filePath)));
            return parse(sql);
        } catch (IOException e) {
            throw new SchemaLoadException("Failed to read MySQL DDL file: " + filePath, e);
        }
    }
    
    /**
     * 解析 MySQL DDL SQL
     * @param sql DDL SQL 字符串
     * @return 表名 -> 列定义列表
     * @throws SchemaLoadException 解析失败
     */
    public static Map<String, List<ColumnDefinition>> parse(String sql) throws SchemaLoadException {
        Map<String, List<ColumnDefinition>> result = new HashMap<>();
        
        try {
            List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
            
            for (SQLStatement statement : statements) {
                if (statement instanceof MySqlCreateTableStatement) {
                    MySqlCreateTableStatement createStmt = (MySqlCreateTableStatement) statement;
                    String tableName = extractTableName(createStmt);
                    List<ColumnDefinition> columns = extractColumns(createStmt);
                    result.put(tableName, columns);
                }
            }
            
        } catch (Exception e) {
            throw new SchemaLoadException("Failed to parse MySQL DDL: " + e.getMessage(), e);
        }
        
        return result;
    }
}