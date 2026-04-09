package org.omono.converter.mybatis.type.handler;

import org.omono.converter.common.TypeCategory;
import java.util.Set;

/**
 * Long type handler for integer types.
 * Handles: TINYINT, SMALLINT, INT, BIGINT, SERIAL, BIGSERIAL
 */
public class LongTypeHandler implements TypeConvertHandler {
    
    private static final Set<String> SUPPORTED_METHODS = Set.of(
        "setByte", "setShort", "setInt", "setLong"
    );
    
    @Override
    public TypeCategory getTypeCategory() {
        return TypeCategory.LONG;
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
            return ((Number) value).longValue();
        }
        
        String str = value.toString().trim();
        if (str.isEmpty()) {
            return null;
        }
        
        // Handle scientific notation
        if (str.toLowerCase().contains("e")) {
            return Long.valueOf(Double.valueOf(str).longValue());
        }
        
        return Long.parseLong(str);
    }
    
    @Override
    public Object adaptValue(Object value) {
        if (value == null) {
            return null;
        }
        // setLong() requires long primitive type
        if (value instanceof Long) {
            return value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return value;
    }
}
