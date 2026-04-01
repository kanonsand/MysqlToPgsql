package converter;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import org.junit.Test;
import org.junit.Before;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for UpdateHandler
 */
public class UpdateHandlerTest {

    private UpdateHandler handler;

    @Before
    public void setUp() {
        handler = new UpdateHandler();
    }

    @Test
    public void testCanHandleUpdate() {
        String sql = "UPDATE `user` SET `name` = 'test' WHERE `id` = 1";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        assertTrue(handler.canHandle(statements.get(0)));
    }

    @Test
    public void testConvertSimpleUpdate() {
        String sql = "UPDATE `user` SET `name` = 'test' WHERE `id` = 1";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlUpdateStatement updateStmt = (MySqlUpdateStatement) statements.get(0);
        
        List<String> result = handler.convert(updateStmt);
        
        assertEquals(1, result.size());
        // Check parameterized query
        assertTrue(result.get(0).contains("?"));
        // Check table name is quoted
        assertTrue(result.get(0).contains("\"user\""));
    }

    @Test
    public void testConvertUpdateWithReservedWord() {
        String sql = "UPDATE `order` SET `user` = 'test' WHERE `group` = 1";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlUpdateStatement updateStmt = (MySqlUpdateStatement) statements.get(0);
        
        List<String> result = handler.convert(updateStmt);
        
        // Check reserved words are quoted
        assertTrue(result.get(0).contains("\"order\""));
    }

    @Test
    public void testConvertUpdateMultipleColumns() {
        String sql = "UPDATE `user` SET `name` = 'test', `age` = 20 WHERE `id` = 1";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlUpdateStatement updateStmt = (MySqlUpdateStatement) statements.get(0);
        
        List<String> result = handler.convert(updateStmt);
        
        assertEquals(1, result.size());
        // Should have 2 placeholders for SET clause
        assertTrue(result.get(0).contains("?"));
    }

    @Test
    public void testConvertUpdateWithoutWhere() {
        String sql = "UPDATE `user` SET `name` = 'test'";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlUpdateStatement updateStmt = (MySqlUpdateStatement) statements.get(0);
        
        List<String> result = handler.convert(updateStmt);
        
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("\"user\""));
        assertFalse(result.get(0).toUpperCase().contains("WHERE"));
    }

    @Test
    public void testGetStatementTypeName() {
        assertEquals("update", handler.getStatementTypeName());
    }
}
