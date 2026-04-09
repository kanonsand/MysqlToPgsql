package org.omono.converter.mybatis.type.handler;

import org.omono.converter.common.TypeCategory;
import java.util.Set;

/**
 * Binary type handler.
 * Handles: BLOB, BINARY, BYTEA
 */
public class BinaryTypeHandler implements TypeConvertHandler {
    
    private static final Set<String> SUPPORTED_METHODS = Set.of(
        "setBytes", "setBlob"
    );
    
    @Override
    public TypeCategory getTypeCategory() {
        return TypeCategory.BINARY;
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
        
        if (value instanceof byte[]) {
            return value;
        }
        
        throw new IllegalArgumentException("Cannot convert to binary: " + value.getClass().getName());
    }
    
    @Override
    public Object adaptValue(Object value) {
        // setBytes() accepts byte[]
        return value;
    }
}
