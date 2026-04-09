package org.omono.converter.schema;

/**
 * 列定义信息
 */
public class ColumnDefinition {
    
    private String name;           // 列名
    private String type;           // 数据类型
    private int ordinal;           // 列顺序（从0开始）
    private boolean nullable;      // 是否可空
    private String defaultValue;   // 默认值
    private boolean primaryKey;    // 是否主键
    
    public ColumnDefinition() {
    }
    
    public ColumnDefinition(String name, String type, int ordinal) {
        this.name = name;
        this.type = type;
        this.ordinal = ordinal;
    }
    
    // Getters and Setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public int getOrdinal() {
        return ordinal;
    }
    
    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }
    
    public boolean isNullable() {
        return nullable;
    }
    
    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public boolean isPrimaryKey() {
        return primaryKey;
    }
    
    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }
    
    @Override
    public String toString() {
        return "ColumnDefinition{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", ordinal=" + ordinal +
                ", nullable=" + nullable +
                '}';
    }
}
