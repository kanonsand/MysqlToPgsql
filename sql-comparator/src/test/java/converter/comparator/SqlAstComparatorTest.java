package converter.comparator;

import com.alibaba.druid.DbType;
import org.omono.converter.comparator.SqlAstComparator;
import org.omono.converter.comparator.context.CompareContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SqlAstComparatorTest {
    
    // ========== 基础 SELECT 测试 ==========
    
    @Test
    void testSimpleSelect() {
        String mysql = "SELECT id, name FROM user";
        String pgsql = "SELECT \"id\", \"name\" FROM \"user\"";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    @Test
    void testSelectWithWhere() {
        String mysql = "SELECT id, name FROM user WHERE id = 1";
        String pgsql = "SELECT \"id\", \"name\" FROM \"user\" WHERE \"id\" = 1";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    @Test
    void testSelectWithMultipleConditions() {
        String mysql = "SELECT * FROM user WHERE id = 1 AND status = 1";
        String pgsql = "SELECT * FROM \"user\" WHERE \"id\" = 1 AND \"status\" = 1";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    @Test
    void testSelectWithColumnMapping() {
        String mysql = "SELECT user_name, is_active FROM user";
        String pgsql = "SELECT \"name\", \"active\" FROM \"user\"";
        
        CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .targetQuotes(CompareContext.POSTGRES_QUOTES)
            .columnMapping("user_name", "name")
            .columnMapping("is_active", "active")
            .build();
        
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    @Test
    void testSelectWithBooleanConversion() {
        // MySQL: is_active = 1  →  PG: active = true
        String mysql = "SELECT id FROM user WHERE is_active = 1";
        String pgsql = "SELECT \"id\" FROM \"user\" WHERE \"active\" = true";
        
        CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .targetQuotes(CompareContext.POSTGRES_QUOTES)
            .withDefaultTypeMatchers()
            .columnMapping("is_active", "active")
            .build();
        
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    @Test
    void testSelectSameValueNoConversion() {
        // 同值不同类型：双方都用 1，不应受 matcher 影响
        String mysql = "SELECT id FROM user WHERE status = 1";
        String pgsql = "SELECT \"id\" FROM \"user\" WHERE \"status\" = 1";
        
        CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .targetQuotes(CompareContext.POSTGRES_QUOTES)
            .withDefaultTypeMatchers()
            .build();
        
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    // ========== JOIN 测试 ==========
    
    @Test
    void testSimpleJoin() {
        String mysql = "SELECT u.id, o.order_no FROM user u JOIN `order` o ON u.id = o.user_id";
        String pgsql = "SELECT \"u\".\"id\", \"o\".\"order_number\" FROM \"user\" \"u\" JOIN \"order\" \"o\" ON \"u\".\"id\" = \"o\".\"user_id\"";
        
        CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .sourceQuotes(CompareContext.MYSQL_QUOTES)
            .targetQuotes(CompareContext.POSTGRES_QUOTES)
            .columnMapping("order_no", "order_number")
            .build();
        
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    // ========== INSERT 测试 ==========
    
    @Test
    void testSimpleInsert() {
        String mysql = "INSERT INTO user (id, name) VALUES (1, 'Alice')";
        String pgsql = "INSERT INTO \"user\" (\"id\", \"name\") VALUES (1, 'Alice')";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    @Test
    void testInsertWithBooleanConversion() {
        String mysql = "INSERT INTO user (user_name, is_active) VALUES ('Alice', 1)";
        String pgsql = "INSERT INTO \"user\" (\"name\", \"active\") VALUES ('Alice', true)";
        
        CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .targetQuotes(CompareContext.POSTGRES_QUOTES)
            .withDefaultTypeMatchers()
            .columnMapping("user_name", "name")
            .columnMapping("is_active", "active")
            .build();
        
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    // ========== UPDATE 测试 ==========
    
    @Test
    void testSimpleUpdate() {
        String mysql = "UPDATE user SET name = 'Bob' WHERE id = 1";
        String pgsql = "UPDATE \"user\" SET \"name\" = 'Bob' WHERE \"id\" = 1";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    @Test
    void testUpdateWithBooleanConversion() {
        String mysql = "UPDATE user SET is_active = 0 WHERE id = 1";
        String pgsql = "UPDATE \"user\" SET \"active\" = false WHERE \"id\" = 1";
        
        CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .targetQuotes(CompareContext.POSTGRES_QUOTES)
            .withDefaultTypeMatchers()
            .columnMapping("is_active", "active")
            .build();
        
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    // ========== DELETE 测试 ==========
    
    @Test
    void testSimpleDelete() {
        String mysql = "DELETE FROM user WHERE id = 1";
        String pgsql = "DELETE FROM \"user\" WHERE \"id\" = 1";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    @Test
    void testDeleteWithMultipleConditions() {
        String mysql = "DELETE FROM user WHERE id = 1 AND status = 0";
        String pgsql = "DELETE FROM \"user\" WHERE \"id\" = 1 AND \"status\" = 0";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    // ========== ORDER BY / LIMIT 测试 ==========
    
    @Test
    void testSelectWithOrderBy() {
        String mysql = "SELECT id, name FROM user ORDER BY id DESC";
        String pgsql = "SELECT \"id\", \"name\" FROM \"user\" ORDER BY \"id\" DESC";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    @Test
    void testSelectWithLimit() {
        String mysql = "SELECT id FROM user LIMIT 10";
        String pgsql = "SELECT \"id\" FROM \"user\" LIMIT 10";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    // ========== IN / BETWEEN 测试 ==========
    
    @Test
    void testSelectWithIn() {
        String mysql = "SELECT id FROM user WHERE id IN (1, 2, 3)";
        String pgsql = "SELECT \"id\" FROM \"user\" WHERE \"id\" IN (1, 2, 3)";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    @Test
    void testSelectWithBetween() {
        String mysql = "SELECT id FROM user WHERE id BETWEEN 1 AND 10";
        String pgsql = "SELECT \"id\" FROM \"user\" WHERE \"id\" BETWEEN 1 AND 10";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    // ========== 失败场景测试 ==========
    
    @Test
    void testMismatchTable() {
        String mysql = "SELECT id FROM user";
        String pgsql = "SELECT \"id\" FROM \"order\"";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertThrows(AssertionError.class, () -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    @Test
    void testMismatchColumn() {
        String mysql = "SELECT id, name FROM user";
        String pgsql = "SELECT \"id\", \"other\" FROM \"user\"";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertThrows(AssertionError.class, () -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    @Test
    void testMismatchOperator() {
        String mysql = "SELECT id FROM user WHERE id = 1";
        String pgsql = "SELECT \"id\" FROM \"user\" WHERE \"id\" > 1";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertThrows(AssertionError.class, () -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    @Test
    void testMismatchValue() {
        String mysql = "SELECT id FROM user WHERE id = 1";
        String pgsql = "SELECT \"id\" FROM \"user\" WHERE \"id\" = 2";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertThrows(AssertionError.class, () -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    @Test
    void testMismatchStatementType() {
        String mysql = "SELECT id FROM user";
        String pgsql = "DELETE FROM \"user\"";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertThrows(AssertionError.class, () -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    // ========== Alias 测试 ==========
    
    @Test
    void testSelectBothHaveAlias() {
        String mysql = "SELECT user_name AS name FROM user";
        String pgsql = "SELECT \"user_name\" AS \"name\" FROM \"user\"";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    @Test
    void testSelectWithAliasAndColumnMapping() {
        String mysql = "SELECT user_name AS name FROM user";
        String pgsql = "SELECT \"name\" AS \"name\" FROM \"user\"";
        
        CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .targetQuotes(CompareContext.POSTGRES_QUOTES)
            .columnMapping("user_name", "name")
            .build();
        
        assertDoesNotThrow(() -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
    
    @Test
    void testSelectAliasWithWrongColumn() {
        String mysql = "SELECT user_name AS name FROM user";
        String pgsql = "SELECT \"other_column\" AS \"name\" FROM \"user\"";
        
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.postgresql);
        assertThrows(AssertionError.class, () -> SqlAstComparator.assertEquivalent(mysql, pgsql, ctx));
    }
}
