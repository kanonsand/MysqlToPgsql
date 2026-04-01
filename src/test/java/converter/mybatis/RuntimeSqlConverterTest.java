package converter.mybatis;

import converter.schema.ColumnDefinition;
import converter.schema.SchemaRegistry;
import converter.schema.TableMapping;
import org.junit.Test;
import org.junit.Before;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Tests for RuntimeSqlConverter
 */
public class RuntimeSqlConverterTest {
    
    private RuntimeSqlConverter converter;
    private SchemaRegistry registry;
    
    @Before
    public void setUp() {
        ConversionConfig config = new ConversionConfig();
        registry = new SchemaRegistry();
        
        // Setup table mapping with different column orders
        TableMapping mapping = new TableMapping("user");
        mapping.setMysqlColumns(Arrays.asList(
            new ColumnDefinition("id", "INT", 0),
            new ColumnDefinition("name", "VARCHAR", 1),
            new ColumnDefinition("age", "INT", 2)
        ));
        mapping.setPgColumns(Arrays.asList(
            new ColumnDefinition("id", "INTEGER", 0),
            new ColumnDefinition("age", "INTEGER", 1),
            new ColumnDefinition("name", "VARCHAR", 2)
        ));
        
        try {
            registry.register(mapping);
        } catch (Exception e) {
            fail("Setup failed: " + e.getMessage());
        }
        
        converter = new RuntimeSqlConverter(config, registry);
    }
    
    @Test
    public void testConvertInsertWithColumns() {
        String sql = "INSERT INTO `user` (`id`, `name`, `age`) VALUES (1, 'Tom', 20)";
        String result = converter.convert(sql, "INSERT");
        
        assertTrue(result.contains("\"user\""));
    }
    
    @Test
    public void testConvertUpdate() {
        String sql = "UPDATE `user` SET `name` = 'Tom' WHERE `id` = 1";
        String result = converter.convert(sql, "UPDATE");
        
        assertTrue(result.contains("\"user\""));
    }
    
    @Test
    public void testConvertDelete() {
        String sql = "DELETE FROM `user` WHERE `id` = 1";
        String result = converter.convert(sql, "DELETE");
        
        assertTrue(result.contains("\"user\""));
    }
    
    @Test
    public void testConvertSelect() {
        String sql = "SELECT * FROM `user` WHERE `id` = 1";
        String result = converter.convert(sql, "SELECT");
        
        assertTrue(result.contains("\"user\""));
    }
    
    @Test
    public void testCacheEnabled() {
        String sql = "SELECT * FROM `user`";
        
        // First conversion
        String result1 = converter.convert(sql, "SELECT");
        assertEquals(1, converter.getCacheSize());
        
        // Second conversion should use cache
        String result2 = converter.convert(sql, "SELECT");
        assertEquals(result1, result2);
        assertEquals(1, converter.getCacheSize());
    }
    
    @Test
    public void testClearCache() {
        String sql = "SELECT * FROM `user`";
        converter.convert(sql, "SELECT");
        
        assertTrue(converter.getCacheSize() > 0);
        
        converter.clearCache();
        assertEquals(0, converter.getCacheSize());
    }
    
    @Test
    public void testConvertNullSql() {
        String result = converter.convert(null, "SELECT");
        assertNull(result);
    }
    
    @Test
    public void testConvertEmptySql() {
        String result = converter.convert("", "SELECT");
        assertEquals("", result);
    }
}
