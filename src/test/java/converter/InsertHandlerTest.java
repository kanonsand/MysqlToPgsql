package converter;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import org.junit.Test;
import org.junit.Before;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for InsertHandler
 */
public class InsertHandlerTest {

    private InsertHandler handler;

    @Before
    public void setUp() {
        handler = new InsertHandler();
    }

    @Test
    public void testCanHandleInsert() {
        String sql = "INSERT INTO `user` (`id`, `name`) VALUES (1, 'test')";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        assertTrue(handler.canHandle(statements.get(0)));
    }

    @Test
    public void testConvertSimpleInsert() {
        String sql = "INSERT INTO `user` (`id`, `name`) VALUES (1, 'test')";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlInsertStatement insertStmt = (MySqlInsertStatement) statements.get(0);
        
        List<String> result = handler.convert(insertStmt);
        
        assertEquals(1, result.size());
        // Check parameterized query
        assertTrue(result.get(0).contains("?"));
        // Check table name is quoted
        assertTrue(result.get(0).contains("\"user\""));
        // Check column names are quoted
        assertTrue(result.get(0).contains("\"id\""));
        assertTrue(result.get(0).contains("\"name\""));
    }

    @Test
    public void testConvertInsertWithReservedWord() {
        String sql = "INSERT INTO `order` (`user`, `group`) VALUES (1, 'test')";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlInsertStatement insertStmt = (MySqlInsertStatement) statements.get(0);
        
        List<String> result = handler.convert(insertStmt);
        
        // Check reserved words are quoted
        assertTrue(result.get(0).contains("\"order\""));
        assertTrue(result.get(0).contains("\"user\""));
        assertTrue(result.get(0).contains("\"group\""));
    }

    @Test
    public void testConvertInsertWithoutColumns() {
        String sql = "INSERT INTO `user` VALUES (1, 'test', 20)";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlInsertStatement insertStmt = (MySqlInsertStatement) statements.get(0);
        
        List<String> result = handler.convert(insertStmt);
        
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("\"user\""));
        // Should have 3 placeholders
        assertTrue(result.get(0).contains("(?, ?, ?)") || result.get(0).contains("(?,?,?)"));
    }

    @Test
    public void testConvertInsertMultipleValues() {
        String sql = "INSERT INTO `user` (`id`, `name`) VALUES (1, 'test'), (2, 'test2'), (3, 'test3')";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlInsertStatement insertStmt = (MySqlInsertStatement) statements.get(0);
        
        List<String> result = handler.convert(insertStmt);
        
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("\"user\""));
    }

    @Test
    public void testGetStatementTypeName() {
        assertEquals("insert", handler.getStatementTypeName());
    }
}
