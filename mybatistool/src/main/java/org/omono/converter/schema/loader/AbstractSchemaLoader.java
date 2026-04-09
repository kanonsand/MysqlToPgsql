package org.omono.converter.schema.loader;

import com.alibaba.druid.sql.ast.statement.SQLColumnDefinition;
import com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement;
import org.omono.converter.schema.ColumnDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract schema loader providing common column extraction logic
 */
public abstract class AbstractSchemaLoader {
    
    /**
     * Extract table name from CREATE TABLE statement
     * @param stmt the CREATE TABLE statement
     * @return table name (without quotes)
     */
    protected static String extractTableName(SQLCreateTableStatement stmt) {
        String name = stmt.getTableName();
        // Remove quotes (both backticks and double quotes)
        return name.replace("`", "").replace("\"", "");
    }
    
    /**
     * Extract column definitions from CREATE TABLE statement
     * @param stmt the CREATE TABLE statement
     * @return list of column definitions
     */
    protected static List<ColumnDefinition> extractColumns(SQLCreateTableStatement stmt) {
        List<ColumnDefinition> columns = new ArrayList<>();
        
        int ordinal = 0;
        for (SQLColumnDefinition colDef : stmt.getColumnDefinitions()) {
            ColumnDefinition col = convertColumn(colDef, ordinal++);
            columns.add(col);
        }
        
        return columns;
    }
    
    /**
     * Convert SQLColumnDefinition to ColumnDefinition
     * @param colDef SQL column definition
     * @param ordinal column ordinal position
     * @return ColumnDefinition
     */
    protected static ColumnDefinition convertColumn(SQLColumnDefinition colDef, int ordinal) {
        ColumnDefinition col = new ColumnDefinition();
        
        // Column name (remove quotes)
        String colName = colDef.getColumnName().replace("`", "").replace("\"", "");
        col.setName(colName);
        
        // Data type
        String dataType = colDef.getDataType() != null ? colDef.getDataType().getName() : "UNKNOWN";
        col.setType(dataType);
        
        // Column ordinal
        col.setOrdinal(ordinal);
        
        // Nullable - use containsNotNullConstraint() to check
        col.setNullable(!colDef.containsNotNullConstaint());
        
        // Default value
        if (colDef.getDefaultExpr() != null) {
            col.setDefaultValue(colDef.getDefaultExpr().toString());
        }
        
        return col;
    }
}
