package converter.mybatis;

import converter.schema.SqlTypeMapper;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Time;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Parameter proxy interceptor for parameter remapping and type conversion.
 * Intercepts ParameterHandler.setParameters() to proxy PreparedStatement.
 */
@Intercepts({
    @Signature(type = ParameterHandler.class, 
               method = "setParameters", 
               args = {PreparedStatement.class})
})
public class ParameterProxyInterceptor implements Interceptor {
    
    private final ConversionConfig config;
    
    // Methods that set parameters (methodName -> parameter index for the index argument)
    private static final Set<String> SET_METHODS = new HashSet<>(Arrays.asList(
        "setString", "setInt", "setLong", "setDouble", "setFloat", 
        "setShort", "setByte", "setBoolean", "setDate", "setTime",
        "setTimestamp", "setBigDecimal", "setBigInteger", "setBytes",
        "setObject", "setNull", "setBlob", "setClob", "setArray",
        "setRef", "setURL", "setSQLXML", "setNString", "setNClob"
    ));
    
    // Methods that have 1-based index as first parameter
    private static final Set<String> INDEXED_METHODS = new HashSet<>(Arrays.asList(
        "setString", "setInt", "setLong", "setDouble", "setFloat",
        "setShort", "setByte", "setBoolean", "setDate", "setTime",
        "setTimestamp", "setBigDecimal", "setBigInteger", "setBytes",
        "setObject", "setNull", "setBlob", "setClob", "setArray",
        "setRef", "setURL", "setSQLXML", "setNString", "setNClob",
        "clearParameters", "addBatch", "execute", "executeQuery", "executeUpdate"
    ));
    
    public ParameterProxyInterceptor(ConversionConfig config) {
        this.config = config;
    }
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!config.isEnabled()) {
            return invocation.proceed();
        }
        
        ParameterHandler parameterHandler = (ParameterHandler) invocation.getTarget();
        PreparedStatement originalPs = (PreparedStatement) invocation.getArgs()[0];
        
        // Get BoundSql from ParameterHandler
        BoundSql boundSql = getFieldValue(parameterHandler, "boundSql");
        if (boundSql == null) {
            return invocation.proceed();
        }
        
        // Get conversion context
        ConversionContext context = ConversionContext.from(boundSql);
        
        // If no conversion needed, proceed directly
        if (context == null || !context.needsConversion()) {
            return invocation.proceed();
        }
        
        // Create proxy PreparedStatement
        PreparedStatement proxyPs = createProxy(originalPs, context);
        
        // Replace argument with proxy
        invocation.getArgs()[0] = proxyPs;
        
        return invocation.proceed();
    }
    
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    
    @Override
    public void setProperties(Properties properties) {
        // No properties needed
    }
    
    /**
     * Create a proxy for PreparedStatement
     */
    private PreparedStatement createProxy(PreparedStatement original, ConversionContext context) {
        return (PreparedStatement) Proxy.newProxyInstance(
            PreparedStatement.class.getClassLoader(),
            new Class<?>[]{PreparedStatement.class},
            new PreparedStatementInvocationHandler(original, context)
        );
    }
    
    /**
     * Invocation handler for PreparedStatement proxy
     */
    private class PreparedStatementInvocationHandler implements InvocationHandler {
        
        private final PreparedStatement original;
        private final ConversionContext context;
        
        public PreparedStatementInvocationHandler(PreparedStatement original, ConversionContext context) {
            this.original = original;
            this.context = context;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            
            // Intercept setXxx methods
            if (SET_METHODS.contains(methodName) && args != null && args.length >= 2) {
                return handleSetMethod(methodName, args);
            }
            
            // Pass through other methods
            return method.invoke(original, args);
        }
        
        /**
         * Handle setXxx method calls
         */
        private Object handleSetMethod(String methodName, Object[] args) throws Throwable {
            // args[0] = parameter index (1-based)
            // args[1] = value
            // args[2...] = optional additional arguments
            
            int originalIndex = (Integer) args[0];
            
            // 1. Remap parameter index
            int newIndex = context.remapParameterIndex(originalIndex);
            args[0] = newIndex;
            
            // 2. Type conversion for value
            Object value = args[1];
            if (value != null) {
                SqlTypeMapper.TypeCategory category = context.getParameterCategory(originalIndex);
                args[1] = convertValue(value, category, methodName);
            }
            
            // Call original method
            try {
                return invokeOriginalMethod(methodName, args);
            } catch (Exception e) {
                // If conversion failed, try with original value
                if (args[1] != value) {
                    args[1] = value;
                    args[0] = originalIndex;
                    return invokeOriginalMethod(methodName, args);
                }
                throw e;
            }
        }
        
        /**
         * Invoke original PreparedStatement method
         */
        private Object invokeOriginalMethod(String methodName, Object[] args) throws Throwable {
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }
            
            // Handle primitive types
            if (args[0] instanceof Integer) {
                paramTypes[0] = int.class;
            }
            
            try {
                Method method = findMethod(original.getClass(), methodName, paramTypes);
                return method.invoke(original, args);
            } catch (NoSuchMethodException e) {
                // Try with Object parameter type
                for (int i = 1; i < paramTypes.length; i++) {
                    if (paramTypes[i] != int.class) {
                        paramTypes[i] = Object.class;
                    }
                }
                Method method = findMethod(original.getClass(), methodName, paramTypes);
                return method.invoke(original, args);
            }
        }
        
        /**
         * Find method with parameter types, handling primitive type boxing
         */
        private Method findMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes) throws NoSuchMethodException {
            try {
                return clazz.getMethod(methodName, paramTypes);
            } catch (NoSuchMethodException e) {
                // Try with alternative primitive types
                for (Method m : clazz.getMethods()) {
                    if (m.getName().equals(methodName) && m.getParameterCount() == paramTypes.length) {
                        Class<?>[] methodParamTypes = m.getParameterTypes();
                        boolean match = true;
                        for (int i = 0; i < paramTypes.length; i++) {
                            if (!isAssignable(methodParamTypes[i], paramTypes[i])) {
                                match = false;
                                break;
                            }
                        }
                        if (match) {
                            return m;
                        }
                    }
                }
                throw e;
            }
        }
        
        /**
         * Check if a type is assignable, handling primitive types
         */
        private boolean isAssignable(Class<?> target, Class<?> source) {
            if (target.isPrimitive()) {
                if (target == int.class) return source == Integer.class || source == int.class;
                if (target == long.class) return source == Long.class || source == long.class;
                if (target == double.class) return source == Double.class || source == double.class;
                if (target == float.class) return source == Float.class || source == float.class;
                if (target == boolean.class) return source == Boolean.class || source == boolean.class;
                if (target == short.class) return source == Short.class || source == short.class;
                if (target == byte.class) return source == Byte.class || source == byte.class;
            }
            return target.isAssignableFrom(source);
        }
        
        /**
         * Convert value based on target type category
         */
        private Object convertValue(Object value, SqlTypeMapper.TypeCategory category, String methodName) {
            if (value == null) {
                return null;
            }
            
            switch (category) {
                case NUMERIC:
                    return convertToNumeric(value);
                case BOOLEAN:
                    return convertToBoolean(value);
                case DATE:
                    return convertToDate(value, methodName);
                case STRING:
                    return convertToString(value);
                default:
                    return value;
            }
        }
        
        /**
         * Convert value to numeric type
         */
        private Object convertToNumeric(Object value) {
            if (value instanceof Number) {
                return value;
            }
            
            String str = value.toString().trim();
            
            // Handle empty string
            if (str.isEmpty()) {
                return null;
            }
            
            // Try to parse as number
            try {
                if (str.contains(".") || str.toLowerCase().contains("e")) {
                    return new BigDecimal(str);
                }
                return new BigInteger(str);
            } catch (NumberFormatException e) {
                return value; // Return original if conversion fails
            }
        }
        
        /**
         * Convert value to boolean
         */
        private Object convertToBoolean(Object value) {
            if (value instanceof Boolean) {
                return value;
            }
            
            if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
            
            String str = value.toString().trim().toLowerCase();
            if ("true".equals(str) || "1".equals(str) || "yes".equals(str) || "on".equals(str)) {
                return true;
            }
            if ("false".equals(str) || "0".equals(str) || "no".equals(str) || "off".equals(str)) {
                return false;
            }
            
            return value; // Return original if conversion fails
        }
        
        /**
         * Convert value to date/timestamp
         */
        private Object convertToDate(Object value, String methodName) {
            if (value instanceof Timestamp || value instanceof Date || value instanceof Time) {
                return value;
            }
            
            if (value instanceof java.util.Date) {
                return new Timestamp(((java.util.Date) value).getTime());
            }
            
            if (value instanceof Long) {
                return new Timestamp((Long) value);
            }
            
            String str = value.toString().trim();
            
            // Try common date formats
            try {
                // ISO format: yyyy-MM-dd HH:mm:ss
                if (str.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                    return Timestamp.valueOf(str.replace("T", " "));
                }
                
                // Unix timestamp (seconds)
                if (str.matches("\\d+")) {
                    long ts = Long.parseLong(str);
                    if (ts < 10000000000L) {
                        // Probably seconds, convert to milliseconds
                        ts *= 1000;
                    }
                    return new Timestamp(ts);
                }
            } catch (Exception e) {
                // Ignore, return original
            }
            
            return value; // Return original if conversion fails
        }
        
        /**
         * Convert value to string
         */
        private Object convertToString(Object value) {
            if (value instanceof String) {
                return value;
            }
            return value.toString();
        }
    }
    
    /**
     * Get field value via reflection
     */
    @SuppressWarnings("unchecked")
    private <T> T getFieldValue(Object object, String fieldName) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(object);
        } catch (Exception e) {
            // Try superclass
            try {
                Field field = object.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return (T) field.get(object);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
