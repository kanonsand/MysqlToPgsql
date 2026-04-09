package org.omono.converter;

import org.junit.Test;
import org.omono.converter.oldhandler.ColumnNameHandler;

import static org.junit.Assert.*;

/**
 * Tests for ColumnNameHandler
 */
public class ColumnNameHandlerTest {

    @Test
    public void testConvertWithBackticks() {
        String result = ColumnNameHandler.convert("`name`");
        assertEquals("\"name\"", result);
    }

    @Test
    public void testConvertWithoutQuotes() {
        String result = ColumnNameHandler.convert("name");
        assertEquals("\"name\"", result);
    }

    @Test
    public void testConvertReservedWord() {
        String result = ColumnNameHandler.convert("`user`");
        assertEquals("\"user\"", result);
    }

    @Test
    public void testConvertOrder() {
        String result = ColumnNameHandler.convert("`order`");
        assertEquals("\"order\"", result);
    }

    @Test
    public void testConvertNull() {
        String result = ColumnNameHandler.convert(null);
        assertNull(result);
    }

    @Test
    public void testConvertEmpty() {
        String result = ColumnNameHandler.convert("");
        assertEquals("", result);
    }

    @Test
    public void testConvertAlreadyDoubleQuoted() {
        String result = ColumnNameHandler.convert("\"id\"");
        assertEquals("\"id\"", result);
    }
}
