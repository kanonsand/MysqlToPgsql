package org.omono.converter.mybatis.interceptor;

import org.omono.converter.common.TypeCategory;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;
import org.omono.converter.mybatis.ConversionConfig;
import org.omono.converter.mybatis.ConversionContext;
import org.omono.converter.mybatis.ConversionControl;
import org.omono.converter.mybatis.type.handler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * Parameter proxy interceptor for type conversion.
 * Intercepts ParameterHandler.setParameters() to proxy PreparedStatement.
 * 
 * Uses TypeConvertHandler strategy pattern for clean, extensible type conversion.
 */
@Intercepts({
    @Signature(type = ParameterHandler.class, 
               method = "setParameters", 
               args = {java.sql.PreparedStatement.class})
})
public class ParameterProxyInterceptor implements Interceptor {
    
    private static final Logger log = LoggerFactory.getLogger(ParameterProxyInterceptor.class);
    
    private final ConversionConfig config;
    
    // Handler list (ordered by priority)
    private final List<TypeConvertHandler> handlers;
    
    // Quick lookup: TypeCategory -> Handler
    private final Map<TypeCategory, TypeConvertHandler> handlerMap;
    
    public ParameterProxyInterceptor(ConversionConfig config) {
        this.config = config;
        
        // Initialize handlers
        this.handlers = Arrays.asList(
            new LongTypeHandler(),
            new DoubleTypeHandler(),
            new DecimalTypeHandler(),
            new StringTypeHandler(),
            new BooleanTypeHandler(),
            new DateTypeHandler(),
            new BinaryTypeHandler()
        );
        
        // Build quick lookup map
        this.handlerMap = new EnumMap<>(TypeCategory.class);
        for (TypeConvertHandler handler : handlers) {
            handlerMap.put(handler.getTypeCategory(), handler);
        }
    }
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // Check if conversion is disabled globally
        if (!config.isEnabled()) {
            return invocation.proceed();
        }
        
        // Check if conversion is skipped via ThreadLocal
        if (ConversionControl.shouldSkip()) {
            if (log.isDebugEnabled()) {
                log.debug("[ParameterProxyInterceptor] Skipping conversion (ThreadLocal flag set)");
            }
            return invocation.proceed();
        }
        
        ParameterHandler parameterHandler = (ParameterHandler) invocation.getTarget();
        java.sql.PreparedStatement originalPs = (java.sql.PreparedStatement) invocation.getArgs()[0];
        
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
        
        if (log.isDebugEnabled()) {
            log.debug("[ParameterProxyInterceptor] Context found, creating proxy");
            log.debug("[ParameterProxyInterceptor] Context: {}", context);
        }
        
        // Create proxy PreparedStatement
        java.sql.PreparedStatement proxyPs = createProxy(originalPs, context);
        
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
    private java.sql.PreparedStatement createProxy(java.sql.PreparedStatement original, ConversionContext context) {
        return (java.sql.PreparedStatement) Proxy.newProxyInstance(
            java.sql.PreparedStatement.class.getClassLoader(),
            new Class<?>[]{java.sql.PreparedStatement.class},
            new PreparedStatementInvocationHandler(original, context)
        );
    }
    
    /**
     * Check if a method name is a setXxx method that any handler supports
     */
    private boolean isSetMethod(String methodName) {
        for (TypeConvertHandler handler : handlers) {
            if (handler.supportsMethod(methodName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Invocation handler for PreparedStatement proxy
     */
    private class PreparedStatementInvocationHandler implements InvocationHandler {
        
        private final java.sql.PreparedStatement original;
        private final ConversionContext context;
        
        public PreparedStatementInvocationHandler(java.sql.PreparedStatement original, ConversionContext context) {
            this.original = original;
            this.context = context;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            
            // Only intercept setXxx methods that handlers support
            if (args != null && args.length >= 2 && isSetMethod(methodName)) {
                return handleSetMethod(methodName, args);
            }
            
            // Pass through other methods
            return method.invoke(original, args);
        }
        
        /**
         * Handle setXxx method calls with type conversion
         */
        private Object handleSetMethod(String methodName, Object[] args) throws Throwable {
            // args[0] = parameter index (1-based)
            // args[1] = value
            // args[2...] = optional additional arguments
            
            int paramIndex = (Integer) args[0];
            Object value = args[1];
            Object originalValue = value;
            
            // Get target type category
            TypeCategory targetCategory = context.getParameterCategory(paramIndex);
            
            // Get handler for target type
            TypeConvertHandler handler = handlerMap.get(targetCategory);
            
            // No handler or method already compatible - no conversion needed
            if (handler == null || handler.supportsMethod(methodName)) {
                return invokeMethod(methodName, args);
            }
            
            // Perform type conversion
            try {
                Object convertedValue = handler.convert(value);
                args[1] = handler.adaptValue(convertedValue);
                String targetMethod = targetCategory.getTargetJdbcMethod();
                
                // Log conversion
                if (log.isDebugEnabled()) {
                    log.debug("[ParameterProxyInterceptor] {} index {}: {} -> {} ({} -> {}, method: {} -> {})",
                        methodName, paramIndex, originalValue, args[1],
                        originalValue != null ? originalValue.getClass().getSimpleName() : "null",
                        args[1] != null ? args[1].getClass().getSimpleName() : "null",
                        methodName, targetMethod);
                }
                
                return invokeMethod(targetMethod, args);
                
            } catch (Exception e) {
                // Conversion failed, try original method
                if (log.isDebugEnabled()) {
                    log.debug("[ParameterProxyInterceptor] Conversion failed, fallback to original: {}", e.getMessage());
                }
                args[1] = originalValue;
                return invokeMethod(methodName, args);
            }
        }
        
        /**
         * Invoke PreparedStatement method with proper parameter type handling
         */
        private Object invokeMethod(String methodName, Object[] args) throws Throwable {
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }
            
            // First parameter is always int (index)
            paramTypes[0] = int.class;
            
            try {
                Method method = findMethod(original.getClass(), methodName, paramTypes);
                return method.invoke(original, args);
            } catch (NoSuchMethodException e) {
                // Try with Object parameter type for value
                for (int i = 1; i < paramTypes.length; i++) {
                    paramTypes[i] = Object.class;
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
                // Try to find matching method with primitive type handling
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