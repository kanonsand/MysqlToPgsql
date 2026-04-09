package org.omono.converter.schema.exception;

/**
 * Schema 加载异常
 */
public class SchemaLoadException extends Exception {
    
    public SchemaLoadException(String message) {
        super(message);
    }
    
    public SchemaLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
