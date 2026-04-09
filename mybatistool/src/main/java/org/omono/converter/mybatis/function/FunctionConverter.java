package org.omono.converter.mybatis.function;

import org.omono.converter.mybatis.ConversionContext;

import java.util.List;

/**
 * SQL function converter interface for custom function conversion.
 * Used to convert MySQL functions to PostgreSQL equivalents.
 */
public interface FunctionConverter {
    
    /**
     * Convert SQL function
     * 
     * @param functionName MySQL function name (uppercase)
     * @param args function argument list (already parsed)
     * @param context conversion context (may be null for standalone usage)
     * @return converted function expression, return null to skip this converter
     */
    String convert(String functionName, List<String> args, ConversionContext context);
    
    /**
     * Convert SQL function (simplified version for backward compatibility)
     * 
     * @param functionName function name (uppercase)
     * @param args function arguments as string
     * @return converted expression, return null to skip this converter
     */
    default String convert(String functionName, String args) {
        return null;
    }
    
    /**
     * Check if this converter supports the given function
     * 
     * @param functionName function name (uppercase)
     * @return true if this converter can handle the function
     */
    default boolean supports(String functionName) {
        return true;
    }
}