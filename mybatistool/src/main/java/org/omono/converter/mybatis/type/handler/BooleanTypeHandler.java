package org.omono.converter.mybatis.type.handler;

import org.omono.converter.common.TypeCategory;
import java.util.Set;

/**
 * Boolean type handler.
 * Handles: BOOLEAN, BIT
 */
public class BooleanTypeHandler implements TypeConvertHandler {
    
    private static final Set<String> SUPPORTED_METHODS = Set.of(
        "setBoolean"
    );
    
    @Override
    public TypeCategory getTypeCategory() {
        return TypeCategory.BOOLEAN;
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
        
        if (value instanceof Boolean) {
            return value;
        }
        
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        
        String str = value.toString().trim().toLowerCase();
        if ("true".equals(str) || "1".equals(str) || "yes".equals(str) || "on".equals(str)) {
            return true;
        }
        if ("false".equals(str) || "0".equals(str) || "no".equals(str) || "off".equals(str)) {
            return false;
        }
        
        throw new IllegalArgumentException("Cannot convert to boolean: " + value);
    }
    
    @Override
    public Object adaptValue(Object value) {
        // setBoolean() accepts boolean primitive
        return value;
    }
}
