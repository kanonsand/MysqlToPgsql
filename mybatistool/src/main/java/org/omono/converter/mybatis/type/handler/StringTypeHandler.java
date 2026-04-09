package org.omono.converter.mybatis.type.handler;

import org.omono.converter.common.TypeCategory;
import java.util.Set;

/**
 * String type handler for text types.
 * Handles: VARCHAR, CHAR, TEXT, CLOB
 */
public class StringTypeHandler implements TypeConvertHandler {
    
    private static final Set<String> SUPPORTED_METHODS = Set.of(
        "setString", "setNString"
    );
    
    @Override
    public TypeCategory getTypeCategory() {
        return TypeCategory.STRING;
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
        
        if (value instanceof String) {
            return value;
        }
        
        return value.toString();
    }
    
    @Override
    public Object adaptValue(Object value) {
        // setString() accepts String, conversion already produces String
        return value;
    }
}
