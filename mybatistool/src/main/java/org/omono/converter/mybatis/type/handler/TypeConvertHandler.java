package org.omono.converter.mybatis.type.handler;

import org.omono.converter.common.TypeCategory;
import java.util.Set;

/**
 * Type conversion handler interface.
 * Each handler is responsible for converting values to a specific type category.
 */
public interface TypeConvertHandler {
    
    /**
     * Get the type category this handler processes
     */
    TypeCategory getTypeCategory();
    
    /**
     * Get the JDBC setXxx methods that are compatible with this type.
     * If the called method is in this set, no conversion is needed.
     * 
     * @return set of supported JDBC method names
     */
    Set<String> getSupportedMethods();
    
    /**
     * Convert value to target type
     * @param value the original value
     * @return converted value, or null if value is null
     * @throws IllegalArgumentException if conversion fails
     */
    Object convert(Object value);
    
    /**
     * Adapt value to match JDBC method parameter type
     * For example, BigInteger -> long for setLong()
     * 
     * @param value the converted value
     * @return adapted value ready for JDBC method
     */
    default Object adaptValue(Object value) {
        return value;
    }
    
    /**
     * Check if this handler supports the given JDBC method
     * @param methodName the JDBC method name
     * @return true if the method is supported (no conversion needed)
     */
    default boolean supportsMethod(String methodName) {
        return getSupportedMethods().contains(methodName);
    }
}
