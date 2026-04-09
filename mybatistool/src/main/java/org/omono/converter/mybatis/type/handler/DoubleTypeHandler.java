package org.omono.converter.mybatis.type.handler;

import org.omono.converter.common.TypeCategory;
import java.util.Set;

/**
 * Double type handler for floating point types.
 * Handles: FLOAT, DOUBLE, REAL
 */
public class DoubleTypeHandler implements TypeConvertHandler {
    
    private static final Set<String> SUPPORTED_METHODS = Set.of(
        "setFloat", "setDouble"
    );
    
    @Override
    public TypeCategory getTypeCategory() {
        return TypeCategory.DOUBLE;
    }
    
    @Override
    public Set<String> getSupportedMethods() {
        return SUPPORTED_METHODS;
    }
    
    @Override
    public Object convert(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        
        String str = value.toString().trim();
        if (str.isEmpty()) {
            return null;
        }
        
        return Double.parseDouble(str);
    }
    
    @Override
    public Object adaptValue(Object value) {
        if (value == null) {
            return null;
        }
        // setDouble() requires double primitive type
        if (value instanceof Double) {
            return value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return value;
    }
}
