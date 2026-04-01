package converter;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import org.junit.Test;
import org.junit.Before;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for DeleteHandler
 */
public class DeleteHandlerTest {

    private DeleteHandler handler;

    @Before
    public void setUp() {
        handler = new DeleteHandler();
    }

    @Test
    public void testCanHandleDelete() {
        String sql = "DELETE FROM `user` WHERE `id` = 1";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        assertTrue(handler.canHandle(statements.get(0)));
    }

    @Test
    public void testConvertSimpleDelete() {
        String sql = "DELETE FROM `user` WHERE `id` = 1";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlDeleteStatement deleteStmt = (MySqlDeleteStatement) statements.get(0);
        
        List<String> result = handler.convert(deleteStmt);
        
        assertEquals(1, result.size());
        // Check table name is quoted
        assertTrue(result.get(0).contains("\"user\""));
        assertTrue(result.get(0).toUpperCase().contains("DELETE FROM"));
        assertTrue(result.get(0).toUpperCase().contains("WHERE"));
    }

    @Test
    public void testConvertDeleteWithReservedWord() {
        String sql = "DELETE FROM `order` WHERE `user` = 1";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlDeleteStatement deleteStmt = (MySqlDeleteStatement) statements.get(0);
        
        List<String> result = handler.convert(deleteStmt);
        
        // Check reserved words are quoted
        assertTrue(result.get(0).contains("\"order\""));
    }

    @Test
    public void testConvertDeleteWithoutWhere() {
        String sql = "DELETE FROM `user`";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlDeleteStatement deleteStmt = (MySqlDeleteStatement) statements.get(0);
        
        List<String> result = handler.convert(deleteStmt);
        
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("\"user\""));
        assertFalse(result.get(0).toUpperCase().contains("WHERE"));
    }

    @Test
    public void testConvertDeleteWithMultipleConditions() {
        String sql = "DELETE FROM `user` WHERE `id` = 1 AND `status` = 0";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlDeleteStatement deleteStmt = (MySqlDeleteStatement) statements.get(0);
        
        List<String> result = handler.convert(deleteStmt);
        
        assertEquals(1, result.size());
        assertTrue(result.get(0).toUpperCase().contains("AND"));
    }

    @Test
    public void testGetStatementTypeName() {
        assertEquals("delete", handler.getStatementTypeName());
    }
}
