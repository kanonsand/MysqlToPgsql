package org.omono.converter.mybatis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL analysis result containing parsed information from MySQL SQL.
 */
public class SqlAnalysisResult {
    
    private String tableName;                           // Primary table name
    private ConversionContext.StatementType statementType;  // Statement type
    private List<String> columnNames;                   // Column names (for INSERT/UPDATE)
    private boolean hasValuesWithoutColumns;            // INSERT without column names
    private int parameterCount;                         // Parameter count (?)
    private boolean hasLimitOffset;                     // Has LIMIT offset, count syntax
    private Integer limitValue;                         // LIMIT value (if literal)
    private Integer offsetValue;                        // OFFSET value (if literal)
    private List<String> selectColumns;                 // SELECT column list
    private List<String> whereColumns;                  // WHERE clause columns
    private boolean hasWhereClause;                     // Has WHERE clause
    
    // Alias to table name mapping (alias -> tableName)
    // For "FROM t1 alias1, t2 alias2" maps: alias1->t1, alias2->t2
    // For "FROM t1" (no alias) maps: t1->t1
    private Map<String, String> aliasToTable;
    
    public SqlAnalysisResult() {
        this.columnNames = new ArrayList<>();
        this.selectColumns = new ArrayList<>();
        this.whereColumns = new ArrayList<>();
        this.aliasToTable = new HashMap<>();
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
    
    public boolean isHasWhereClause() {
        return hasWhereClause;
    }
    
    public void setHasWhereClause(boolean hasWhereClause) {
        this.hasWhereClause = hasWhereClause;
    }
    
    /**
     * Get the alias-to-table mapping.
     * @return map of alias -> actual table name
     */
    public Map<String, String> getAliasToTable() {
        return aliasToTable;
    }
    
    /**
     * Set the alias-to-table mapping.
     */
    public void setAliasToTable(Map<String, String> aliasToTable) {
        this.aliasToTable = aliasToTable != null ? aliasToTable : new HashMap<>();
    }
    
    /**
     * Add an alias mapping.
     * @param alias the alias (or table name if no alias)
     * @param tableName the actual table name
     */
    public void addAlias(String alias, String tableName) {
        this.aliasToTable.put(alias, tableName);
    }
    
    /**
     * Get the actual table name for an alias.
     * @param alias the alias or table name
     * @return the actual table name, or null if not found
     */
    public String getTableByAlias(String alias) {
        return aliasToTable.get(alias);
    }
    
    /**
     * Check if an alias exists.
     */
    public boolean hasAlias(String alias) {
        return aliasToTable.containsKey(alias);
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
