package org.omono.converter.schema.exception;

/**
 * Schema 验证异常
 */
public class SchemaValidationException extends Exception {
    
    public SchemaValidationException(String message) {
        super(message);
    }
    
    public SchemaValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
