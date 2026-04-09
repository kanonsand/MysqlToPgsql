package org.omono.converter.schema;

import org.junit.Test;
import org.junit.Before;
import org.omono.converter.schema.exception.SchemaValidationException;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Tests for SchemaRegistry
 */
public class SchemaRegistryTest {
    
    private SchemaRegistry registry;
    
    @Before
    public void setUp() {
        registry = new SchemaRegistry();
    }
    
    @Test
    public void testRegisterAndGet() throws SchemaValidationException {
        TableMapping mapping = new TableMapping("user");
        mapping.setMysqlColumns(Arrays.asList(
            new ColumnDefinition("id", "INT", 0)
        ));
        mapping.setPgColumns(Arrays.asList(
            new ColumnDefinition("id", "INTEGER", 0)
        ));
        
        registry.register(mapping);
        
        assertNotNull(registry.getTableMapping("user"));
        assertNotNull(registry.getTableMapping("USER")); // case insensitive
        assertNull(registry.getTableMapping("unknown"));
    }
    
    @Test
    public void testStrictModeValidation() {
        registry.setStrictMode(true);
        
        TableMapping badMapping = new TableMapping("test");
        badMapping.setMysqlColumns(Arrays.asList(new ColumnDefinition("id", "INT", 0)));
        badMapping.setPgColumns(Arrays.asList()); // Empty - mismatch
        
        try {
            registry.register(badMapping);
            fail("Should throw SchemaValidationException");
        } catch (SchemaValidationException e) {
            // Expected
        }
    }
    
    @Test
    public void testHasTableMapping() throws SchemaValidationException {
        TableMapping mapping = new TableMapping("user");
        mapping.setMysqlColumns(Arrays.asList(new ColumnDefinition("id", "INT", 0)));
        mapping.setPgColumns(Arrays.asList(new ColumnDefinition("id", "INTEGER", 0)));
        
        registry.register(mapping);
        
        assertTrue(registry.hasTableMapping("user"));
        assertTrue(registry.hasTableMapping("USER"));
        assertFalse(registry.hasTableMapping("unknown"));
    }
    
    @Test
    public void testSize() throws SchemaValidationException {
        assertEquals(0, registry.size());
        
        TableMapping mapping = new TableMapping("user");
        mapping.setMysqlColumns(Arrays.asList(new ColumnDefinition("id", "INT", 0)));
        mapping.setPgColumns(Arrays.asList(new ColumnDefinition("id", "INTEGER", 0)));
        
        registry.register(mapping);
        assertEquals(1, registry.size());
    }
    
    @Test
    public void testClear() throws SchemaValidationException {
        TableMapping mapping = new TableMapping("user");
        mapping.setMysqlColumns(Arrays.asList(new ColumnDefinition("id", "INT", 0)));
        mapping.setPgColumns(Arrays.asList(new ColumnDefinition("id", "INTEGER", 0)));
        
        registry.register(mapping);
        assertEquals(1, registry.size());
        
        registry.clear();
        assertEquals(0, registry.size());
    }
}
