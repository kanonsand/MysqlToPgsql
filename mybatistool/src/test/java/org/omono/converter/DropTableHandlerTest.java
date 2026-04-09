package org.omono.converter;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDropTableStatement;
import org.junit.Test;
import org.junit.Before;
import org.omono.converter.oldhandler.DropTableHandler;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for DropTableHandler
 */
public class DropTableHandlerTest {

    private DropTableHandler handler;

    @Before
    public void setUp() {
        handler = new DropTableHandler();
    }

    @Test
    public void testCanHandleDropTable() {
        String sql = "DROP TABLE `user`";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        assertTrue(handler.canHandle(statements.get(0)));
    }

    @Test
    public void testConvertDropTable() {
        String sql = "DROP TABLE `user`";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        SQLDropTableStatement dropStmt = (SQLDropTableStatement) statements.get(0);
        
        List<String> result = handler.convert(dropStmt);
        
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("\"user\""));
    }

    @Test
    public void testConvertDropTableWithReservedWord() {
        String sql = "DROP TABLE `order`";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        SQLDropTableStatement dropStmt = (SQLDropTableStatement) statements.get(0);
        
        List<String> result = handler.convert(dropStmt);
        
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("\"order\""));
    }

    @Test
    public void testConvertDropMultipleTables() {
        String sql = "DROP TABLE `user`, `group`";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        SQLDropTableStatement dropStmt = (SQLDropTableStatement) statements.get(0);
        
        List<String> result = handler.convert(dropStmt);
        
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("\"user\""));
        assertTrue(result.get(0).contains("\"group\""));
    }

    @Test
    public void testGetStatementTypeName() {
        assertEquals("drop", handler.getStatementTypeName());
    }
}
