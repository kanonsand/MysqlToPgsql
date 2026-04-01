package converter.mybatis;

/**
 * Function converter interface for custom function conversion
 */
public interface FunctionConverter {
    
    /**
     * Convert function
     * @param functionName function name
     * @param args argument list string
     * @return converted expression, return null to skip
     */
    String convert(String functionName, String args);
}
