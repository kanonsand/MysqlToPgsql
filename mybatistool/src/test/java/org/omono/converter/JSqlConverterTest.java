package org.omono.converter;

import net.sf.jsqlparser.JSQLParserException;
import org.junit.Test;
import org.omono.converter.oldhandler.JSqlConverter;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for JSqlConverter
 */
public class JSqlConverterTest {

    @Test
    public void testConvertCreateSimple() throws JSQLParserException {
        String sql = "CREATE TABLE `user` (`id` INT NOT NULL, `name` VARCHAR(255))";
        
        List<String> result = JSqlConverter.convertCreate(sql);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        // Check quoted identifiers
        assertTrue(result.get(0).contains("\"user\""));
        assertTrue(result.get(0).contains("\"id\""));
        assertTrue(result.get(0).contains("\"name\""));
    }

    @Test
    public void testConvertCreateWithAutoIncrement() throws JSQLParserException {
        String sql = "CREATE TABLE `test` (`id` INT NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id`))";
        
        List<String> result = JSqlConverter.convertCreate(sql);
        
        // INT AUTO_INCREMENT should become SERIAL
        assertTrue(result.get(0).toUpperCase().contains("SERIAL"));
    }

    @Test
    public void testConvertCreateWithBigIntAutoIncrement() throws JSQLParserException {
        String sql = "CREATE TABLE `test` (`id` BIGINT NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id`))";
        
        List<String> result = JSqlConverter.convertCreate(sql);
        
        // BIGINT AUTO_INCREMENT should become BIGSERIAL
        assertTrue(result.get(0).toUpperCase().contains("BIGSERIAL"));
    }

    @Test
    public void testConvertCreateWithReservedWordTableName() throws JSQLParserException {
        String sql = "CREATE TABLE `order` (`user` INT, `group` VARCHAR(255))";
        
        List<String> result = JSqlConverter.convertCreate(sql);
        
        // Reserved words should be quoted
        assertTrue(result.get(0).contains("\"order\""));
        assertTrue(result.get(0).contains("\"user\""));
        assertTrue(result.get(0).contains("\"group\""));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertCreateInvalidSql() throws JSQLParserException {
        String sql = "SELECT * FROM user";
        JSqlConverter.convertCreate(sql);
    }
}
