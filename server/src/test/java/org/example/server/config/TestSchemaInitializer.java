package org.example.server.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * Test configuration to initialize H2 database schema.
 * Loads PostgreSQL schema for tests since we're using H2 in PostgreSQL mode.
 */
@TestConfiguration
public class TestSchemaInitializer {
    
    @Bean
    public DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setEnabled(true);
        
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("test_schema.sql"));
        populator.setContinueOnError(false);
        
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}
