package org.omono.converter.mybatis.type.handler;

import org.omono.converter.common.TypeCategory;
import java.sql.Timestamp;
import java.util.Set;

/**
 * Date/Time type handler.
 * Handles: DATE, TIME, DATETIME, TIMESTAMP
 */
public class DateTypeHandler implements TypeConvertHandler {
    
    private static final Set<String> SUPPORTED_METHODS = Set.of(
        "setDate", "setTime", "setTimestamp"
    );
    
    @Override
    public TypeCategory getTypeCategory() {
        return TypeCategory.DATE;
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
        
        if (value instanceof Timestamp) {
            return value;
        }
        
        if (value instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) value).getTime());
        }
        
        if (value instanceof Long) {
            return new Timestamp((Long) value);
        }
        
        String str = value.toString().trim();
        if (str.isEmpty()) {
            return null;
        }
        
        // Try ISO format: yyyy-MM-dd HH:mm:ss or yyyy-MM-dd'T'HH:mm:ss
        if (str.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            try {
                return Timestamp.valueOf(str.replace("T", " "));
            } catch (IllegalArgumentException e) {
                // Try with just date part
                if (str.length() >= 10) {
                    return Timestamp.valueOf(str.substring(0, 10) + " 00:00:00");
                }
            }
        }
        
        // Unix timestamp (seconds or milliseconds)
        if (str.matches("\\d+")) {
            long ts = Long.parseLong(str);
            if (ts < 10000000000L) {
                // Probably seconds, convert to milliseconds
                ts *= 1000;
            }
            return new Timestamp(ts);
        }
        
        throw new IllegalArgumentException("Cannot convert to timestamp: " + value);
    }
    
    @Override
    public Object adaptValue(Object value) {
        // setTimestamp() accepts Timestamp
        return value;
    }
}
