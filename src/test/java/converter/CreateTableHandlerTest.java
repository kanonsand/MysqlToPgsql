package converter;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import org.junit.Test;
import org.junit.Before;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for CreateTableHandler
 */
public class CreateTableHandlerTest {

    private CreateTableHandler handler;

    @Before
    public void setUp() {
        handler = new CreateTableHandler();
    }

    @Test
    public void testCanHandleCreateTable() {
        String sql = "CREATE TABLE `user` (`id` INT PRIMARY KEY)";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        assertTrue(handler.canHandle(statements.get(0)));
    }

    @Test
    public void testConvertSimpleCreateTable() {
        String sql = "CREATE TABLE `user` (" +
                "`id` INT NOT NULL AUTO_INCREMENT, " +
                "`name` VARCHAR(255), " +
                "PRIMARY KEY (`id`)" +
                ")";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlCreateTableStatement createStmt = (MySqlCreateTableStatement) statements.get(0);
        
        List<String> result = handler.convert(createStmt);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        // Check that table name is quoted
        assertTrue(result.get(0).contains("\"user\""));
    }

    @Test
    public void testConvertAutoIncrement() {
        String sql = "CREATE TABLE `test` (`id` INT NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id`))";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlCreateTableStatement createStmt = (MySqlCreateTableStatement) statements.get(0);
        
        List<String> result = handler.convert(createStmt);
        
        // AUTO_INCREMENT should be converted to SERIAL
        assertTrue(result.get(0).toUpperCase().contains("SERIAL"));
    }

    @Test
    public void testConvertBigIntAutoIncrement() {
        String sql = "CREATE TABLE `test` (`id` BIGINT NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id`))";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlCreateTableStatement createStmt = (MySqlCreateTableStatement) statements.get(0);
        
        List<String> result = handler.convert(createStmt);
        
        // BIGINT AUTO_INCREMENT should be converted to BIGSERIAL
        assertTrue(result.get(0).toUpperCase().contains("BIGSERIAL"));
    }

    @Test
    public void testConvertRemoveCharset() {
        String sql = "CREATE TABLE `test` (`id` INT, `name` VARCHAR(255) CHARACTER SET utf8 COLLATE utf8_general_ci)";
        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlCreateTableStatement createStmt = (MySqlCreateTableStatement) statements.get(0);
        
        List<String> result = handler.convert(createStmt);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testGetStatementTypeName() {
        assertEquals("create", handler.getStatementTypeName());
    }
}
