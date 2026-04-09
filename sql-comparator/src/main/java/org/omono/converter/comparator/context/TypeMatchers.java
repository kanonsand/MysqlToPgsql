package org.omono.converter.comparator.context;

import java.util.function.BiPredicate;

/**
 * Factory class for common type matchers used in SQL comparison.
 * These matchers handle value equivalence between different TypeCategories.
 */
public final class TypeMatchers {
    
    private TypeMatchers() {}
    
    /**
     * Matcher for LONG (0/1) to BOOLEAN (false/true) conversion.
     * MySQL often uses 0/1 for boolean, while PostgreSQL uses true/false.
     */
    public static BiPredicate<Object, Object> longToBoolean() {
        return (a, b) -> {
            Long l = (Long) a;
            Boolean bool = (Boolean) b;
            return (l == 1 && bool) || (l == 0 && !bool);
        };
    }
    
    /**
     * Matcher for BOOLEAN to LONG conversion (reverse of longToBoolean).
     */
    public static BiPredicate<Object, Object> booleanToLong() {
        return (a, b) -> {
            Boolean bool = (Boolean) a;
            Long l = (Long) b;
            return (bool && l == 1) || (!bool && l == 0);
        };
    }
    
    /**
     * Matcher for LONG to DOUBLE conversion.
     * Compares the double value of the long with the double.
     */
    public static BiPredicate<Object, Object> longToDouble() {
        return (a, b) -> ((Long) a).doubleValue() == (Double) b;
    }
    
    /**
     * Matcher for DOUBLE to LONG conversion.
     * Compares the long value of the double with the long.
     */
    public static BiPredicate<Object, Object> doubleToLong() {
        return (a, b) -> ((Double) a).longValue() == (Long) b;
    }
    
    /**
     * Matcher for STRING to LONG conversion.
     * Parses the string as a long and compares.
     */
    public static BiPredicate<Object, Object> stringToLong() {
        return (a, b) -> {
            try {
                return Long.parseLong((String) a) == (Long) b;
            } catch (NumberFormatException e) {
                return false;
            }
        };
    }
    
    /**
     * Matcher for LONG to STRING conversion.
     * Compares the string representation of the long with the string.
     */
    public static BiPredicate<Object, Object> longToString() {
        return (a, b) -> ((Long) a).toString().equals(b);
    }
    
    /**
     * Matcher for STRING to DOUBLE conversion.
     * Parses the string as a double and compares.
     */
    public static BiPredicate<Object, Object> stringToDouble() {
        return (a, b) -> {
            try {
                return Double.parseDouble((String) a) == (Double) b;
            } catch (NumberFormatException e) {
                return false;
            }
        };
    }
    
    /**
     * Matcher for DOUBLE to STRING conversion.
     * Compares the string representation of the double with the string.
     */
    public static BiPredicate<Object, Object> doubleToString() {
        return (a, b) -> ((Double) a).toString().equals(b);
    }
    
    /**
     * Matcher for STRING to BOOLEAN conversion.
     * Parses common string representations of boolean.
     */
    public static BiPredicate<Object, Object> stringToBoolean() {
        return (a, b) -> {
            String s = ((String) a).trim().toLowerCase();
            Boolean bool = (Boolean) b;
            if (bool) {
                return "true".equals(s) || "1".equals(s) || "yes".equals(s);
            } else {
                return "false".equals(s) || "0".equals(s) || "no".equals(s);
            }
        };
    }
    
    /**
     * Matcher for BOOLEAN to STRING conversion.
     */
    public static BiPredicate<Object, Object> booleanToString() {
        return (a, b) -> {
            Boolean bool = (Boolean) a;
            String s = ((String) b).trim().toLowerCase();
            if (bool) {
                return "true".equals(s) || "1".equals(s);
            } else {
                return "false".equals(s) || "0".equals(s);
            }
        };
    }
}
