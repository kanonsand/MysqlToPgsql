package org.example.server.config;

import org.omono.converter.mybatis.ConversionConfig;
import org.omono.converter.mybatis.function.DefaultFunctionConverter;
import org.omono.converter.mybatis.interceptor.ParameterProxyInterceptor;
import org.omono.converter.mybatis.interceptor.SqlRewriteInterceptor;
import org.omono.converter.schema.SchemaRegistry;
import org.apache.ibatis.session.SqlSessionFactory;
import org.omono.converter.schema.SqlFileSchemaProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
public class MyBatisConfig {
    
    @Value("${conversion.enabled:true}")
    private boolean conversionEnabled;
    
    @Value("${conversion.strict-mode:false}")
    private boolean strictMode;
    
    @Value("classpath:mysql_schema.sql")
    private Resource mysqlSchemaResource;
    
    @Value("classpath:postgres_schema.sql")
    private Resource postgresSchemaResource;
    
    @Autowired
    public void addInterceptors(SqlSessionFactory sqlSessionFactory) {
        if (!conversionEnabled) {
            return;
        }
        
        SchemaRegistry schemaRegistry = createSchemaRegistry();
        ConversionConfig config = createConversionConfig();
        
        // Add interceptors (order matters!)
        sqlSessionFactory.getConfiguration().addInterceptor(
            new SqlRewriteInterceptor(schemaRegistry, config));
        sqlSessionFactory.getConfiguration().addInterceptor(
            new ParameterProxyInterceptor(config));
    }
    
    @Bean
    public SchemaRegistry schemaRegistry() {
        return createSchemaRegistry();
    }
    
    @Bean
    public ConversionConfig conversionConfig() {
        return createConversionConfig();
    }
    
    private SchemaRegistry createSchemaRegistry() {
        SchemaRegistry registry = new SchemaRegistry(strictMode);
        
        try {
            String mysqlSchema = new String(
                java.nio.file.Files.readAllBytes(mysqlSchemaResource.getFile().toPath()));
            String postgresSchema = new String(
                java.nio.file.Files.readAllBytes(postgresSchemaResource.getFile().toPath()));
            
            // Use fromContent to pass SQL content directly
            SqlFileSchemaProvider provider =
                SqlFileSchemaProvider.fromContent(mysqlSchema, postgresSchema);
            registry.load(provider);
            
            System.out.println("Schema registry loaded successfully with " + registry.size() + " table(s)");
        } catch (Exception e) {
            // Log warning but don't fail
            System.err.println("Warning: Failed to load schema files: " + e.getMessage());
        }
        
        return registry;
    }
    
    private ConversionConfig createConversionConfig() {
        ConversionConfig config = new ConversionConfig();
        config.setEnabled(conversionEnabled);
        config.setStrictMode(strictMode);
        config.addFunctionConverter(new DefaultFunctionConverter());
        return config;
    }
}
