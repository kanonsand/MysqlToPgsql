package converter.mybatis;

import converter.schema.SchemaProvider;
import converter.schema.SchemaRegistry;
import converter.schema.SchemaValidationException;
import converter.schema.SqlFileSchemaProvider;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * MyBatis SQL interceptor for MySQL to PostgreSQL conversion
 */
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class MyBatisSqlInterceptor implements Interceptor {
    
    private final ConversionConfig config;
    private final SchemaRegistry schemaRegistry;
    private final RuntimeSqlConverter sqlConverter;
    
    public MyBatisSqlInterceptor(ConversionConfig config, SchemaRegistry schemaRegistry) {
        this.config = config;
        this.schemaRegistry = schemaRegistry;
        this.sqlConverter = new RuntimeSqlConverter(config, schemaRegistry);
    }
    
    public MyBatisSqlInterceptor(ConversionConfig config, String mysqlDdlPath, String postgresDdlPath) 
            throws SchemaValidationException {
        this.config = config;
        this.schemaRegistry = new SchemaRegistry(config.isStrictMode());
        
        SchemaProvider provider = new SqlFileSchemaProvider(mysqlDdlPath, postgresDdlPath);
        try {
            schemaRegistry.load(provider);
        } catch (Exception e) {
            throw new SchemaValidationException("Failed to load schema from SQL files", e);
        }
        
        this.sqlConverter = new RuntimeSqlConverter(config, schemaRegistry);
    }
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!config.isEnabled()) {
            return invocation.proceed();
        }
        
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        
        if (config.isSqlIdExcluded(mappedStatement.getId())) {
            return invocation.proceed();
        }
        
        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        String originalSql = boundSql.getSql();
        String convertedSql = sqlConverter.convert(originalSql, mappedStatement.getSqlCommandType().name());
        
        if (originalSql.equals(convertedSql)) {
            return invocation.proceed();
        }
        
        setFieldValue(boundSql, "sql", convertedSql);
        return invocation.proceed();
    }
    
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    
    @Override
    public void setProperties(Properties properties) {
        // Configuration from properties if needed
    }
    
    private void setFieldValue(Object object, String fieldName, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
    
    public SchemaRegistry getSchemaRegistry() {
        return schemaRegistry;
    }
    
    public ConversionConfig getConfig() {
        return config;
    }
    
    public RuntimeSqlConverter getSqlConverter() {
        return sqlConverter;
    }
}
