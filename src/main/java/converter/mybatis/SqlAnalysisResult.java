package converter.mybatis;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL analysis result containing parsed information from MySQL SQL.
 */
public class SqlAnalysisResult {
    
    private String tableName;                           // Table name
    private ConversionContext.StatementType statementType;  // Statement type
    private List<String> columnNames;                   // Column names (for INSERT/UPDATE)
    private boolean hasValuesWithoutColumns;            // INSERT without column names
    private int parameterCount;                         // Parameter count (?)
    private boolean hasLimitOffset;                     // Has LIMIT offset, count syntax
    private Integer limitValue;                         // LIMIT value (if literal)
    private Integer offsetValue;                        // OFFSET value (if literal)
    private List<String> selectColumns;                 // SELECT column list
    private List<String> whereColumns;                  // WHERE clause columns
    
    public SqlAnalysisResult() {
        this.columnNames = new ArrayList<>();
        this.selectColumns = new ArrayList<>();
        this.whereColumns = new ArrayList<>();
        this.hasValuesWithoutColumns = false;
        this.hasLimitOffset = false;
    }
    
    /**
     * Check if this is an INSERT statement
     */
    public boolean isInsert() {
        return statementType == ConversionContext.StatementType.INSERT;
    }
    
    /**
     * Check if this is an UPDATE statement
     */
    public boolean isUpdate() {
        return statementType == ConversionContext.StatementType.UPDATE;
    }
    
    /**
     * Check if this is a DELETE statement
     */
    public boolean isDelete() {
        return statementType == ConversionContext.StatementType.DELETE;
    }
    
    /**
     * Check if this is a SELECT statement
     */
    public boolean isSelect() {
        return statementType == ConversionContext.StatementType.SELECT;
    }
    
    /**
     * Check if column names need to be added (INSERT without columns)
     */
    public boolean needsColumnNames() {
        return isInsert() && hasValuesWithoutColumns;
    }
    
    /**
     * Check if LIMIT/OFFSET conversion is needed
     */
    public boolean needsLimitOffsetConversion() {
        return hasLimitOffset;
    }
    
    // Getters and Setters
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public ConversionContext.StatementType getStatementType() {
        return statementType;
    }
    
    public void setStatementType(ConversionContext.StatementType statementType) {
        this.statementType = statementType;
    }
    
    public List<String> getColumnNames() {
        return columnNames;
    }
    
    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames != null ? columnNames : new ArrayList<>();
    }
    
    public void addColumnName(String columnName) {
        this.columnNames.add(columnName);
    }
    
    public boolean isHasValuesWithoutColumns() {
        return hasValuesWithoutColumns;
    }
    
    public void setHasValuesWithoutColumns(boolean hasValuesWithoutColumns) {
        this.hasValuesWithoutColumns = hasValuesWithoutColumns;
    }
    
    public int getParameterCount() {
        return parameterCount;
    }
    
    public void setParameterCount(int parameterCount) {
        this.parameterCount = parameterCount;
    }
    
    public boolean isHasLimitOffset() {
        return hasLimitOffset;
    }
    
    public void setHasLimitOffset(boolean hasLimitOffset) {
        this.hasLimitOffset = hasLimitOffset;
    }
    
    public Integer getLimitValue() {
        return limitValue;
    }
    
    public void setLimitValue(Integer limitValue) {
        this.limitValue = limitValue;
    }
    
    public Integer getOffsetValue() {
        return offsetValue;
    }
    
    public void setOffsetValue(Integer offsetValue) {
        this.offsetValue = offsetValue;
    }
    
    public List<String> getSelectColumns() {
        return selectColumns;
    }
    
    public void setSelectColumns(List<String> selectColumns) {
        this.selectColumns = selectColumns != null ? selectColumns : new ArrayList<>();
    }
    
    public void addSelectColumn(String columnName) {
        this.selectColumns.add(columnName);
    }
    
    public List<String> getWhereColumns() {
        return whereColumns;
    }
    
    public void setWhereColumns(List<String> whereColumns) {
        this.whereColumns = whereColumns != null ? whereColumns : new ArrayList<>();
    }
    
    public void addWhereColumn(String columnName) {
        this.whereColumns.add(columnName);
    }
    
    @Override
    public String toString() {
        return "SqlAnalysisResult{" +
                "tableName='" + tableName + '\'' +
                ", statementType=" + statementType +
                ", columnNames=" + columnNames +
                ", hasValuesWithoutColumns=" + hasValuesWithoutColumns +
                ", parameterCount=" + parameterCount +
                ", hasLimitOffset=" + hasLimitOffset +
                '}';
    }
}
