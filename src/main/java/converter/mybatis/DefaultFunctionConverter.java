package converter.mybatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default function converter with common MySQL to PostgreSQL function mappings.
 */
public class DefaultFunctionConverter implements FunctionConverter {
    
    // Simple function name mappings
    private static final Map<String, String> SIMPLE_MAPPINGS = new HashMap<>();
    
    static {
        SIMPLE_MAPPINGS.put("IFNULL", "COALESCE");
        SIMPLE_MAPPINGS.put("NOW", "CURRENT_TIMESTAMP");
        SIMPLE_MAPPINGS.put("CURDATE", "CURRENT_DATE");
        SIMPLE_MAPPINGS.put("CURTIME", "CURRENT_TIME");
        SIMPLE_MAPPINGS.put("SYSDATE", "CURRENT_TIMESTAMP");
        SIMPLE_MAPPINGS.put("UUID", "GEN_RANDOM_UUID");
        SIMPLE_MAPPINGS.put("RAND", "RANDOM");
        SIMPLE_MAPPINGS.put("SHA1", "SHA1");
        SIMPLE_MAPPINGS.put("SHA2", "SHA256");
        SIMPLE_MAPPINGS.put("LCASE", "LOWER");
        SIMPLE_MAPPINGS.put("UCASE", "UPPER");
        SIMPLE_MAPPINGS.put("STRCMP", "STRCMP");
        SIMPLE_MAPPINGS.put("LOCATE", "POSITION");
        SIMPLE_MAPPINGS.put("SUBSTRING", "SUBSTRING");
        SIMPLE_MAPPINGS.put("CONCAT_WS", "CONCAT_WS");
        SIMPLE_MAPPINGS.put("FIELD", "ARRAY_POSITION");
        SIMPLE_MAPPINGS.put("FIND_IN_SET", "ARRAY_POSITION");
        SIMPLE_MAPPINGS.put("ELT", "CHOOSE");
    }
    
    // MySQL date format to PostgreSQL date format mapping
    private static final Map<String, String> DATE_FORMAT_MAPPINGS = new HashMap<>();
    
    static {
        DATE_FORMAT_MAPPINGS.put("%Y", "YYYY");
        DATE_FORMAT_MAPPINGS.put("%y", "YY");
        DATE_FORMAT_MAPPINGS.put("%m", "MM");
        DATE_FORMAT_MAPPINGS.put("%c", "MM");
        DATE_FORMAT_MAPPINGS.put("%d", "DD");
        DATE_FORMAT_MAPPINGS.put("%e", "DD");
        DATE_FORMAT_MAPPINGS.put("%H", "HH24");
        DATE_FORMAT_MAPPINGS.put("%h", "HH12");
        DATE_FORMAT_MAPPINGS.put("%I", "HH12");
        DATE_FORMAT_MAPPINGS.put("%i", "MI");
        DATE_FORMAT_MAPPINGS.put("%s", "SS");
        DATE_FORMAT_MAPPINGS.put("%S", "SS");
        DATE_FORMAT_MAPPINGS.put("%p", "AM");
        DATE_FORMAT_MAPPINGS.put("%W", "Day");
        DATE_FORMAT_MAPPINGS.put("%a", "Dy");
        DATE_FORMAT_MAPPINGS.put("%M", "Month");
        DATE_FORMAT_MAPPINGS.put("%b", "Mon");
        DATE_FORMAT_MAPPINGS.put("%j", "DDD");
        DATE_FORMAT_MAPPINGS.put("%U", "WW");
        DATE_FORMAT_MAPPINGS.put("%u", "IW");
        DATE_FORMAT_MAPPINGS.put("%k", "HH24");
        DATE_FORMAT_MAPPINGS.put("%l", "HH12");
        DATE_FORMAT_MAPPINGS.put("%r", "HH12:MI:SS AM");
        DATE_FORMAT_MAPPINGS.put("%T", "HH24:MI:SS");
    }
    
    @Override
    public String convert(String functionName, List<String> args, ConversionContext context) {
        // 1. Simple name mappings
        if (SIMPLE_MAPPINGS.containsKey(functionName)) {
            String pgFunction = SIMPLE_MAPPINGS.get(functionName);
            if (args == null || args.isEmpty()) {
                return pgFunction + "()";
            }
            return pgFunction + "(" + String.join(", ", args) + ")";
        }
        
        // 2. Complex conversions
        switch (functionName) {
            case "DATE_FORMAT":
                return convertDateFormat(args);
            case "FROM_UNIXTIME":
                return convertFromUnixTime(args);
            case "UNIX_TIMESTAMP":
                return convertUnixTimestamp(args);
            case "GROUP_CONCAT":
                return convertGroupConcat(args);
            case "IF":
                return convertIf(args);
            case "CONCAT":
                return convertConcat(args);
            case "DATEDIFF":
                return convertDateDiff(args);
            case "DATE_ADD":
            case "ADDDATE":
                return convertDateAdd(args);
            case "DATE_SUB":
            case "SUBDATE":
                return convertDateSub(args);
            case "LAST_INSERT_ID":
                return "LASTVAL()";
            case "AUTO_INCREMENT":
                return "NEXTVAL()";
            case "REGEXP":
                return convertRegexp(args);
            default:
                return null;  // Not handled by this converter
        }
    }
    
    @Override
    public String convert(String functionName, String args) {
        return null;  // Use List version instead
    }
    
    @Override
    public boolean supports(String functionName) {
        return SIMPLE_MAPPINGS.containsKey(functionName) ||
               "DATE_FORMAT".equals(functionName) ||
               "FROM_UNIXTIME".equals(functionName) ||
               "UNIX_TIMESTAMP".equals(functionName) ||
               "GROUP_CONCAT".equals(functionName) ||
               "IF".equals(functionName) ||
               "CONCAT".equals(functionName) ||
               "DATEDIFF".equals(functionName) ||
               "DATE_ADD".equals(functionName) ||
               "ADDDATE".equals(functionName) ||
               "DATE_SUB".equals(functionName) ||
               "SUBDATE".equals(functionName) ||
               "LAST_INSERT_ID".equals(functionName) ||
               "REGEXP".equals(functionName);
    }
    
    // DATE_FORMAT(date, format) → TO_CHAR(date, pg_format)
    private String convertDateFormat(List<String> args) {
        if (args == null || args.size() < 2) {
            return null;
        }
        String date = args.get(0);
        String mysqlFormat = args.get(1);
        String pgFormat = convertDateFormatString(mysqlFormat);
        return "TO_CHAR(" + date + ", " + pgFormat + ")";
    }
    
    private String convertDateFormatString(String mysqlFormat) {
        String result = mysqlFormat;
        // Remove quotes if present
        if (result.startsWith("'") && result.endsWith("'")) {
            result = result.substring(1, result.length() - 1);
        }
        
        // Convert MySQL format to PostgreSQL format
        for (Map.Entry<String, String> entry : DATE_FORMAT_MAPPINGS.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        
        return "'" + result + "'";
    }
    
    // FROM_UNIXTIME(timestamp) → TO_TIMESTAMP(timestamp)
    private String convertFromUnixTime(List<String> args) {
        if (args == null || args.isEmpty()) {
            return null;
        }
        String timestamp = args.get(0);
        if (args.size() > 1) {
            // FROM_UNIXTIME(timestamp, format) - need format conversion
            String format = convertDateFormatString(args.get(1));
            return "TO_CHAR(TO_TIMESTAMP(" + timestamp + "), " + format + ")";
        }
        return "TO_TIMESTAMP(" + timestamp + ")";
    }
    
    // UNIX_TIMESTAMP(date) → EXTRACT(EPOCH FROM date)
    private String convertUnixTimestamp(List<String> args) {
        if (args == null || args.isEmpty()) {
            return "EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)";
        }
        return "EXTRACT(EPOCH FROM " + args.get(0) + ")";
    }
    
    // GROUP_CONCAT(col SEPARATOR ',') → STRING_AGG(col, ',')
    private String convertGroupConcat(List<String> args) {
        if (args == null || args.isEmpty()) {
            return null;
        }
        
        // Parse GROUP_CONCAT arguments
        // Simplified: assume first arg is column, optional SEPARATOR
        String column = args.get(0);
        String separator = "','";
        
        // Look for SEPARATOR keyword
        for (int i = 1; i < args.size(); i++) {
            String arg = args.get(i).toUpperCase();
            if (arg.contains("SEPARATOR") && i + 1 < args.size()) {
                separator = args.get(i + 1);
                break;
            }
        }
        
        return "STRING_AGG(" + column + "::TEXT, " + separator + ")";
    }
    
    // IF(condition, true_val, false_val) → CASE WHEN condition THEN true_val ELSE false_val END
    private String convertIf(List<String> args) {
        if (args == null || args.size() < 3) {
            return null;
        }
        return "CASE WHEN " + args.get(0) + " THEN " + args.get(1) + " ELSE " + args.get(2) + " END";
    }
    
    // CONCAT(args...) → args || args (PostgreSQL concatenation)
    private String convertConcat(List<String> args) {
        if (args == null || args.isEmpty()) {
            return null;
        }
        // Use CONCAT function (PostgreSQL also supports it)
        return "CONCAT(" + String.join(", ", args) + ")";
    }
    
    // DATEDIFF(date1, date2) → DATE_PART('day', date1 - date2)
    private String convertDateDiff(List<String> args) {
        if (args == null || args.size() < 2) {
            return null;
        }
        return "DATE_PART('day', " + args.get(0) + " - " + args.get(1) + ")";
    }
    
    // DATE_ADD(date, INTERVAL value unit) → date + INTERVAL 'value unit'
    private String convertDateAdd(List<String> args) {
        if (args == null || args.size() < 2) {
            return null;
        }
        // Simplified handling - assume args[1] is interval expression
        return args.get(0) + " + " + args.get(1);
    }
    
    // DATE_SUB(date, INTERVAL value unit) → date - INTERVAL 'value unit'
    private String convertDateSub(List<String> args) {
        if (args == null || args.size() < 2) {
            return null;
        }
        return args.get(0) + " - " + args.get(1);
    }
    
    // REGEXP → ~ (PostgreSQL regex operator)
    private String convertRegexp(List<String> args) {
        if (args == null || args.size() < 2) {
            return null;
        }
        return args.get(0) + " ~ " + args.get(1);
    }
}
