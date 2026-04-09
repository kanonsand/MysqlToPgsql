package org.omono.converter;

import org.junit.Test;
import org.omono.converter.oldhandler.TableNameHandler;

import static org.junit.Assert.*;

/**
 * Tests for TableNameHandler
 */
public class TableNameHandlerTest {

    @Test
    public void testConvertWithBackticks() {
        String result = TableNameHandler.convert("`user`");
        assertEquals("\"user\"", result);
    }

    @Test
    public void testConvertWithoutQuotes() {
        String result = TableNameHandler.convert("user");
        assertEquals("\"user\"", result);
    }

    @Test
    public void testConvertReservedWord() {
        String result = TableNameHandler.convert("`order`");
        assertEquals("\"order\"", result);
    }

    @Test
    public void testConvertGroup() {
        String result = TableNameHandler.convert("`group`");
        assertEquals("\"group\"", result);
    }

    @Test
    public void testConvertNull() {
        String result = TableNameHandler.convert(null);
        assertNull(result);
    }

    @Test
    public void testConvertEmpty() {
        String result = TableNameHandler.convert("");
        assertEquals("", result);
    }

    @Test
    public void testConvertAlreadyDoubleQuoted() {
        String result = TableNameHandler.convert("\"user\"");
        assertEquals("\"user\"", result);
    }
}
