package org.omono.converter.mybatis;

import org.omono.converter.mybatis.function.FunctionConverter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SQL conversion configuration
 */
public class ConversionConfig {
    
    private boolean enabled = true;
    private boolean strictMode = false;
    private Set<String> excludedTables = new HashSet<>();
    private Set<String> excludedSqlIds = new HashSet<>();
    private boolean insertEnabled = true;
    private boolean updateEnabled = true;
    private boolean deleteEnabled = true;
    private boolean selectEnabled = true;
    private boolean cacheEnabled = true;
    private int cacheMaxSize = 1000;
    private List<FunctionConverter> functionConverters = new ArrayList<>();
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isStrictMode() {
        return strictMode;
    }
    
    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }
    
    public Set<String> getExcludedTables() {
        return excludedTables;
    }
    
    public void setExcludedTables(Set<String> excludedTables) {
        this.excludedTables = excludedTables;
    }
    
    public void addExcludedTable(String tableName) {
        this.excludedTables.add(tableName.toLowerCase());
    }
    
    public Set<String> getExcludedSqlIds() {
        return excludedSqlIds;
    }
    
    public void setExcludedSqlIds(Set<String> excludedSqlIds) {
        this.excludedSqlIds = excludedSqlIds;
    }
    
    public void addExcludedSqlId(String sqlId) {
        this.excludedSqlIds.add(sqlId);
    }
    
    public boolean isInsertEnabled() {
        return insertEnabled;
    }
    
    public void setInsertEnabled(boolean insertEnabled) {
        this.insertEnabled = insertEnabled;
    }
    
    public boolean isUpdateEnabled() {
        return updateEnabled;
    }
    
    public void setUpdateEnabled(boolean updateEnabled) {
        this.updateEnabled = updateEnabled;
    }
    
    public boolean isDeleteEnabled() {
        return deleteEnabled;
    }
    
    public void setDeleteEnabled(boolean deleteEnabled) {
        this.deleteEnabled = deleteEnabled;
    }
    
    public boolean isSelectEnabled() {
        return selectEnabled;
    }
    
    public void setSelectEnabled(boolean selectEnabled) {
        this.selectEnabled = selectEnabled;
    }
    
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }
    
    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }
    
    public int getCacheMaxSize() {
        return cacheMaxSize;
    }
    
    public void setCacheMaxSize(int cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }
    
    public boolean isTableExcluded(String tableName) {
        return tableName != null && excludedTables.contains(tableName.toLowerCase());
    }
    
    public boolean isSqlIdExcluded(String sqlId) {
        return sqlId != null && excludedSqlIds.contains(sqlId);
    }
    
    public List<FunctionConverter> getFunctionConverters() {
        return functionConverters;
    }
    
    public void setFunctionConverters(List<FunctionConverter> functionConverters) {
        this.functionConverters = functionConverters != null ? functionConverters : new ArrayList<>();
    }
    
    public void addFunctionConverter(FunctionConverter converter) {
        this.functionConverters.add(converter);
    }
    
    /**
     * Convert function using registered converters
     * @param functionName function name (uppercase)
     * @param args function arguments string
     * @return converted expression, null if no converter handles it
     */
    public String convertFunction(String functionName, String args) {
        for (FunctionConverter converter : functionConverters) {
            String result = converter.convert(functionName, args);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
