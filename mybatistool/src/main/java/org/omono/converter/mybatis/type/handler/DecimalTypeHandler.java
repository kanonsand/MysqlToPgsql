package org.omono.converter.mybatis.type.handler;

import org.omono.converter.common.TypeCategory;
import java.math.BigDecimal;
import java.util.Set;

/**
 * Decimal type handler for precise numeric types.
 * Handles: DECIMAL, NUMERIC, NUMBER
 */
public class DecimalTypeHandler implements TypeConvertHandler {
    
    private static final Set<String> SUPPORTED_METHODS = Set.of(
        "setBigDecimal"
    );
    
    @Override
    public TypeCategory getTypeCategory() {
        return TypeCategory.DECIMAL;
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
        
        if (value instanceof BigDecimal) {
            return value;
        }
        
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        
        String str = value.toString().trim();
        if (str.isEmpty()) {
            return null;
        }
        
        return new BigDecimal(str);
    }
    
    @Override
    public Object adaptValue(Object value) {
        // setBigDecimal() accepts BigDecimal, no adaptation needed
        return value;
    }
}
