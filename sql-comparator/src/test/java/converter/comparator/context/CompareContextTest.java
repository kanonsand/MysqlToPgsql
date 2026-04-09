package converter.comparator.context;

import com.alibaba.druid.DbType;
import org.omono.converter.comparator.context.CompareContext;
import org.omono.converter.common.TypeCategory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.function.BiPredicate;

class CompareContextTest {
    
    @Test
    void testBuilder() {
        CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql).build();
        assertNotNull(ctx);
        assertEquals(DbType.mysql, ctx.getSourceDbType());
        assertEquals(DbType.postgresql, ctx.getTargetDbType());
    }
    
    @Test
    void testMySqlToPostgres() {
        CompareContext ctx = CompareContext.mySqlToPostgres();
        assertNotNull(ctx);
        assertEquals(DbType.mysql, ctx.getSourceDbType());
        assertEquals(DbType.postgresql, ctx.getTargetDbType());
    }
    
    @Test
    void testForDbTypes() {
        CompareContext ctx = CompareContext.forDbTypes(DbType.mysql, DbType.oracle);
        assertNotNull(ctx);
        assertEquals(DbType.mysql, ctx.getSourceDbType());
        assertEquals(DbType.oracle, ctx.getTargetDbType());
        
        // MySQL 引号处理
        assertEquals("name", ctx.stripSourceQuotes("`name`"));
        
        // Oracle 引号处理
        assertEquals("name", ctx.stripTargetQuotes("\"name\""));
    }
    
    // ========== 引号处理测试 ==========
    
    @Test
    void testStripSourceQuotes_MySql() {
        CompareContext ctx = CompareContext.mySqlToPostgres();
        
        assertEquals("name", ctx.stripSourceQuotes("`name`"));
        assertEquals("name", ctx.stripSourceQuotes("\"name\""));
        assertEquals("name", ctx.stripSourceQuotes("name"));
    }
    
    @Test
    void testStripTargetQuotes_Postgres() {
        CompareContext ctx = CompareContext.mySqlToPostgres();
        
        assertEquals("name", ctx.stripTargetQuotes("\"name\""));
        assertEquals("`name`", ctx.stripTargetQuotes("`name`")); // Postgres doesn't use backticks
        assertEquals("name", ctx.stripTargetQuotes("name"));
    }
    
    // ========== 列名映射测试 ==========
    
    @Test
    void testColumnMapping() {
        CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .columnMapping("user_name", "name")
            .columnMapping("is_active", "active")
            .build();
        
        assertEquals("name", ctx.getMappedColumnName("user_name"));
        assertEquals("active", ctx.getMappedColumnName("is_active"));
        assertNull(ctx.getMappedColumnName("other"));
    }
    
    @Test
    void testNormalizeColumnName() {
        CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .sourceQuotes(CompareContext.MYSQL_QUOTES)
            .targetQuotes(CompareContext.POSTGRES_QUOTES)
            .columnMapping("user_name", "name")
            .build();
        
        // Source (MySQL)
        assertEquals("name", ctx.normalizeColumnName("user_name", true));
        assertEquals("name", ctx.normalizeColumnName("`user_name`", true));
        assertEquals("other", ctx.normalizeColumnName("other", true));
        
        // Target (PostgreSQL)
        assertEquals("name", ctx.normalizeColumnName("name", false));
        assertEquals("name", ctx.normalizeColumnName("\"name\"", false));
    }
    
    // ========== 表名映射测试 ==========
    
    @Test
    void testTableMapping() {
        CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .tableMapping("tb_user", "user")
            .build();
        
        assertEquals("user", ctx.getMappedTableName("tb_user"));
        assertNull(ctx.getMappedTableName("other"));
    }
    
    // ========== 标识符比较测试 ==========
    
    @Test
    void testIsEquivalentIdentifier_Column() {
        CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .sourceQuotes(CompareContext.MYSQL_QUOTES)
            .targetQuotes(CompareContext.POSTGRES_QUOTES)
            .columnMapping("user_name", "name")
            .build();
        
        // 映射后的名称应该匹配
        assertTrue(ctx.isEquivalentIdentifier("user_name", "name", true));
        assertTrue(ctx.isEquivalentIdentifier("`user_name`", "\"name\"", true));
        
        // 未映射的名称
        assertTrue(ctx.isEquivalentIdentifier("id", "id", true));
        assertTrue(ctx.isEquivalentIdentifier("`id`", "\"id\"", true));
        assertFalse(ctx.isEquivalentIdentifier("id", "other", true));
    }
    
    @Test
    void testIsEquivalentIdentifier_Table() {
        CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .sourceQuotes(CompareContext.MYSQL_QUOTES)
            .targetQuotes(CompareContext.POSTGRES_QUOTES)
            .tableMapping("tb_user", "user")
            .build();
        
        assertTrue(ctx.isEquivalentIdentifier("tb_user", "user", false));
        assertTrue(ctx.isEquivalentIdentifier("`tb_user`", "\"user\"", false));
        assertTrue(ctx.isEquivalentIdentifier("order", "order", false));
    }
    
    // ========== Type Matcher 测试 ==========
    
    @Test
    void testDefaultTypeMatchers() {
        CompareContext ctx = CompareContext.mySqlToPostgres();
        
        // LONG → BOOLEAN (1 → true, 0 → false)
        BiPredicate<Object, Object> longToBool = ctx.getTypeMatcher(TypeCategory.LONG, TypeCategory.BOOLEAN);
        assertNotNull(longToBool);
        assertTrue(longToBool.test(1L, true));
        assertTrue(longToBool.test(0L, false));
        assertFalse(longToBool.test(1L, false));
        assertFalse(longToBool.test(0L, true));
        
        // BOOLEAN → LONG
        BiPredicate<Object, Object> boolToLong = ctx.getTypeMatcher(TypeCategory.BOOLEAN, TypeCategory.LONG);
        assertNotNull(boolToLong);
        assertTrue(boolToLong.test(true, 1L));
        assertTrue(boolToLong.test(false, 0L));
    }
    
    @Test
    void testCustomTypeMatcher() {
        BiPredicate<Object, Object> customMatcher = (a, b) -> {
            Long l = (Long) a;
            Boolean bool = (Boolean) b;
            return l == 2 && bool;  // Custom: 2 → true
        };
        
        CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .typeMatcher(TypeCategory.LONG, TypeCategory.BOOLEAN, customMatcher)
            .build();
        
        BiPredicate<Object, Object> matcher = ctx.getTypeMatcher(TypeCategory.LONG, TypeCategory.BOOLEAN);
        assertTrue(matcher.test(2L, true));
        assertFalse(matcher.test(1L, true));  // Custom matcher: 1 ≠ true
    }
    
    @Test
    void testStringToLongMatcher() {
        CompareContext ctx = CompareContext.mySqlToPostgres();
        
        BiPredicate<Object, Object> matcher = ctx.getTypeMatcher(TypeCategory.STRING, TypeCategory.LONG);
        assertNotNull(matcher);
        assertTrue(matcher.test("123", 123L));
        assertTrue(matcher.test("-456", -456L));
        assertFalse(matcher.test("abc", 123L));
    }
    
    // ========== 引号配置测试 ==========
    
    @Test
    void testCustomQuotePairs() {
        CompareContext ctx = CompareContext.builder(DbType.sqlserver, DbType.postgresql)
            .sourceQuotes(CompareContext.SQLSERVER_QUOTES)
            .targetQuotes(CompareContext.POSTGRES_QUOTES)
            .build();
        
        // SQL Server uses []
        assertEquals("name", ctx.stripSourceQuotes("[name]"));
        assertEquals("name", ctx.stripSourceQuotes("\"name\""));
        
        // PostgreSQL uses ""
        assertEquals("name", ctx.stripTargetQuotes("\"name\""));
    }
    
    // ========== Builder 组合测试 ==========
    
    @Test
    void testBuilderChaining() {
        CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql)
            .sourceQuotes(CompareContext.MYSQL_QUOTES)
            .targetQuotes(CompareContext.POSTGRES_QUOTES)
            .withDefaultTypeMatchers()
            .columnMapping("user_name", "name")
            .columnMapping("is_active", "active")
            .tableMapping("tb_user", "users")
            .build();
        
        assertNotNull(ctx);
        assertEquals("name", ctx.getMappedColumnName("user_name"));
        assertEquals("users", ctx.getMappedTableName("tb_user"));
        
        // Type matchers should be configured
        assertNotNull(ctx.getTypeMatcher(TypeCategory.LONG, TypeCategory.BOOLEAN));
    }
    
    // ========== 默认引号测试 ==========
    
    @Test
    void testNoQuotesConfigured() {
        // 不手动设置引号时，不做任何处理，直接返回原字符串
        CompareContext ctx = CompareContext.builder(DbType.mysql, DbType.postgresql).build();
        
        // 没有配置引号，不处理任何引号
        assertEquals("\"name\"", ctx.stripSourceQuotes("\"name\""));
        assertEquals("`name`", ctx.stripSourceQuotes("`name`"));
        assertEquals("[name]", ctx.stripSourceQuotes("[name]"));
        assertEquals("name", ctx.stripSourceQuotes("name")); // 无引号的保持不变
    }
    
    // ========== TypePair 测试 ==========
    
    @Test
    void testTypePairEquality() {
        CompareContext.TypePair p1 = new CompareContext.TypePair(TypeCategory.LONG, TypeCategory.BOOLEAN);
        CompareContext.TypePair p2 = new CompareContext.TypePair(TypeCategory.LONG, TypeCategory.BOOLEAN);
        CompareContext.TypePair p3 = new CompareContext.TypePair(TypeCategory.BOOLEAN, TypeCategory.LONG);
        
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1, p3);
    }
}
