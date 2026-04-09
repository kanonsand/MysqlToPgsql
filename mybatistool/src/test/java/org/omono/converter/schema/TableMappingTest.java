package org.omono.converter.schema;

import org.junit.Test;
import org.junit.Before;
import org.omono.converter.schema.exception.SchemaValidationException;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for TableMapping
 */
public class TableMappingTest {
    
    private TableMapping mapping;
    
    @Before
    public void setUp() {
        mapping = new TableMapping("user");
        
        // MySQL columns: id, name, age
        List<ColumnDefinition> mysqlColumns = Arrays.asList(
            createColumn("id", "INT", 0),
            createColumn("name", "VARCHAR", 1),
            createColumn("age", "INT", 2)
        );
        
        // PostgreSQL columns: id, age, name (different order)
        List<ColumnDefinition> pgColumns = Arrays.asList(
            createColumn("id", "INTEGER", 0),
            createColumn("age", "INTEGER", 1),
            createColumn("name", "VARCHAR", 2)
        );
        
        mapping.setMysqlColumns(mysqlColumns);
        mapping.setPgColumns(pgColumns);
    }
    
    private ColumnDefinition createColumn(String name, String type, int ordinal) {
        ColumnDefinition col = new ColumnDefinition(name, type, ordinal);
        col.setNullable(true);
        return col;
    }
    
    @Test
    public void testGetPostgresColumnName() {
        assertEquals("id", mapping.getPostgresColumnName("id"));
        assertEquals("name", mapping.getPostgresColumnName("name"));
    }
    
    @Test
    public void testGetPostgresColumnOrder() {
        // MySQL columns: id, name, age
        List<String> mysqlCols = Arrays.asList("id", "name", "age");
        List<String> pgCols = mapping.getPostgresColumnOrder(mysqlCols);
        
        // Should return same column names
        assertEquals(3, pgCols.size());
        assertEquals("id", pgCols.get(0));
        assertEquals("name", pgCols.get(1));
        assertEquals("age", pgCols.get(2));
    }
    
    @Test
    public void testGetPostgresColumnOrderEmpty() {
        // When no columns specified, return all PG columns
        List<String> pgCols = mapping.getPostgresColumnOrder(null);
        
        assertEquals(3, pgCols.size());
        // PG order: id, age, name
        assertEquals("id", pgCols.get(0));
        assertEquals("age", pgCols.get(1));
        assertEquals("name", pgCols.get(2));
    }
    
    @Test
    public void testGetPostgresColumnOrdinal() {
        assertEquals(0, mapping.getPostgresColumnOrdinal("id"));
        assertEquals(1, mapping.getPostgresColumnOrdinal("age"));
        assertEquals(2, mapping.getPostgresColumnOrdinal("name"));
        assertEquals(-1, mapping.getPostgresColumnOrdinal("unknown"));
    }
    
    @Test
    public void testValidateSuccess() throws SchemaValidationException {
        // Should not throw
        mapping.validate();
    }
    
    @Test(expected = SchemaValidationException.class)
    public void testValidateColumnCountMismatch() throws SchemaValidationException {
        TableMapping badMapping = new TableMapping("test");
        badMapping.setMysqlColumns(Arrays.asList(createColumn("id", "INT", 0)));
        badMapping.setPgColumns(Arrays.asList(
            createColumn("id", "INT", 0),
            createColumn("name", "VARCHAR", 1)
        ));
        
        badMapping.validate();
    }
    
    @Test
    public void testColumnNameMapping() {
        mapping.addColumnMapping("name", "full_name");
        
        assertEquals("full_name", mapping.getPostgresColumnName("name"));
        assertEquals("id", mapping.getPostgresColumnName("id"));
    }
}
