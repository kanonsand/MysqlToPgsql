package org.example.server;

import org.omono.converter.common.TypeCategory;
import org.omono.converter.mybatis.ConversionConfig;
import org.omono.converter.mybatis.ConversionContext;
import org.omono.converter.mybatis.function.DefaultFunctionConverter;
import org.omono.converter.schema.SchemaRegistry;
import org.omono.converter.schema.TableMapping;
import org.omono.converter.schema.ColumnDefinition;
import org.junit.Test;
import org.junit.Before;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test for MyBatis interceptor integration
 */
public class InterceptorIntegrationTest {
    
    private SchemaRegistry schemaRegistry;
    private ConversionConfig config;
    
    @Before
    public void setUp() throws Exception {
        schemaRegistry = new SchemaRegistry(false);
        config = ConversionConfig.builder()
            .enabled(true)
            .strictMode(false)
            .addFunctionConverter(new DefaultFunctionConverter())
            .build();
        
        // Create table mapping for testing
        TableMapping mapping = new TableMapping("user");
        
        // MySQL columns
        List<ColumnDefinition> mysqlCols = Arrays.asList(
            createColumn("id", "BIGINT", 0),
            createColumn("user_name", "VARCHAR", 1),
            createColumn("age", "INT", 2),
            createColumn("email", "VARCHAR", 3),
            createColumn("create_time", "DATETIME", 4)
        );
        mapping.setMysqlColumns(mysqlCols);
        
        // PostgreSQL columns (same order for simplicity)
        List<ColumnDefinition> pgCols = Arrays.asList(
            createColumn("id", "BIGSERIAL", 0),
            createColumn("user_name", "VARCHAR", 1),
            createColumn("age", "INTEGER", 2),
            createColumn("email", "VARCHAR", 3),
            createColumn("create_time", "TIMESTAMP", 4)
        );
        mapping.setPgColumns(pgCols);
        
        schemaRegistry.register(mapping);
    }
    
    private ColumnDefinition createColumn(String name, String type, int ordinal) {
        ColumnDefinition col = new ColumnDefinition();
        col.setName(name);
        col.setType(type);
        col.setOrdinal(ordinal);
        return col;
    }
    
    @Test
    public void testConversionContext() {
        TableMapping mapping = schemaRegistry.getTableMapping("user");
        assertNotNull(mapping);
        
        // Test parameter mapping
        List<String> columns = Arrays.asList("user_name", "age", "email");
        int[] paramMapping = mapping.getParameterMappingWithColumns(columns);
        assertEquals(3, paramMapping.length);
        
        // Test type categories
        TypeCategory[] categories = mapping.getParameterTypeCategories(columns);
        assertEquals(3, categories.length);
        assertEquals(TypeCategory.STRING, categories[0]);
        assertEquals(TypeCategory.LONG, categories[1]);
        assertEquals(TypeCategory.STRING, categories[2]);
    }
    
    @Test
    public void testConversionContextCreation() {
        ConversionContext context = new ConversionContext();
        context.setTableName("user");
        context.setStatementType(ConversionContext.StatementType.INSERT);
        context.setParameterCategories(new TypeCategory[]{
            TypeCategory.STRING,
            TypeCategory.LONG,
            TypeCategory.STRING
        });
        
        assertEquals("user", context.getTableName());
        assertEquals(ConversionContext.StatementType.INSERT, context.getStatementType());
        assertTrue(context.isNeedsTypeConversion());
        
        // Test type category
        assertEquals(TypeCategory.STRING, context.getParameterCategory(1));
        assertEquals(TypeCategory.LONG, context.getParameterCategory(2));
    }
}
