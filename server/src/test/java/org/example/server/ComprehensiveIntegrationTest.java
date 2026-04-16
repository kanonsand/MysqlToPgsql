package org.example.server;

import org.example.server.config.TestSchemaInitializer;
import org.example.server.entity.TestUser;
import org.example.server.entity.TestOrder;
import org.example.server.mapper.TestMapper;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive integration test for MySQL to PostgreSQL SQL rewrite.
 * Tests all RewriteHandlers and ClauseHandlers with H2 in PostgreSQL mode.
 * 
 * Column name mapping (MySQL -> PostgreSQL):
 * - user.user_name -> user.name
 * - user.is_active -> user.active (TINYINT -> BOOLEAN)
 * - order.order_no -> order.order_number
 * 
 * Tests coverage:
 * - SelectRewriteHandler: SELECT with various clauses
 * - InsertRewriteHandler: INSERT with/without column names
 * - UpdateRewriteHandler: UPDATE with SET clause
 * - DeleteRewriteHandler: DELETE with WHERE clause
 * - ClauseHandlers: SELECT, FROM, WHERE, GROUP BY, HAVING, ORDER BY, LIMIT
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestSchemaInitializer.class)
@Transactional  // Rollback after each test
public class ComprehensiveIntegrationTest {
    
    @Autowired
    private TestMapper testMapper;
    
    private TestUser testUser1;
    private TestUser testUser2;
    private TestOrder testOrder1;
    
    @Before
    public void setUp() {
        // Create test users
        testUser1 = new TestUser("test_user_1", "user1@test.com", 1, true);
        testUser2 = new TestUser("test_user_2", "user2@test.com", 2, false);
        
        testMapper.insertUser(testUser1);
        testMapper.insertUser(testUser2);
        
        // Create test order
        testOrder1 = new TestOrder(testUser1.getId(), "ORD-" + System.currentTimeMillis(), 
                new BigDecimal("100.50"), "pending");
        testMapper.insertOrder(testOrder1);
    }
    
    // ==================== SELECT Tests ====================
    
    @Test
    public void testSelectById() {
        System.out.println("\n=== Test: SELECT by ID ===");
        
        TestUser found = testMapper.findUserById(testUser1.getId());
        
        assertNotNull("User should be found", found);
        assertEquals("User name should match", testUser1.getUserName(), found.getUserName());
        assertEquals("Email should match", testUser1.getEmail(), found.getEmail());
        assertEquals("Active status should match", testUser1.getIsActive(), found.getIsActive());
        
        System.out.println("Found user: " + found);
    }
    
    @Test
    public void testSelectWithAlias() {
        System.out.println("\n=== Test: SELECT with table alias ===");
        
        TestUser found = testMapper.findUserByIdWithAlias(testUser1.getId());
        
        assertNotNull("User should be found", found);
        assertEquals("User name should match", testUser1.getUserName(), found.getUserName());
        
        System.out.println("Found user with alias: " + found);
    }
    
    @Test
    public void testSelectWithWhereConditions() {
        System.out.println("\n=== Test: SELECT with WHERE conditions ===");
        
        List<TestUser> users = testMapper.findUserByStatusAndActive(1, true);
        
        assertNotNull("Users should be found", users);
        assertTrue("Should have at least one user", users.size() >= 1);
        
        System.out.println("Found " + users.size() + " active user(s) with status 1");
    }
    
    @Test
    public void testSelectWithLike() {
        System.out.println("\n=== Test: SELECT with LIKE ===");
        
        List<TestUser> users = testMapper.findUserByNameLike("test_user");
        
        assertNotNull("Users should be found", users);
        assertTrue("Should have at least one user matching", users.size() >= 2);
        
        System.out.println("Found " + users.size() + " user(s) matching 'test_user'");
    }
    
    @Test
    public void testSelectWithInClause() {
        System.out.println("\n=== Test: SELECT with IN clause ===");
        
        List<TestUser> users = testMapper.findUserByStatusIn();
        
        assertNotNull("Users should be found", users);
        
        System.out.println("Found " + users.size() + " user(s) with status IN (1,2,3)");
    }
    
    @Test
    public void testSelectWithBetween() {
        System.out.println("\n=== Test: SELECT with BETWEEN ===");
        
        Date start = new Date(System.currentTimeMillis() - 3600000);  // 1 hour ago
        Date end = new Date(System.currentTimeMillis() + 3600000);    // 1 hour later
        
        List<TestUser> users = testMapper.findUserByDateRange(start, end);
        
        assertNotNull("Users should be found", users);
        
        System.out.println("Found " + users.size() + " user(s) in date range");
    }
    
    @Test
    public void testSelectWithIsNotNull() {
        System.out.println("\n=== Test: SELECT with IS NOT NULL ===");
        
        List<TestUser> users = testMapper.findUserWithEmail();
        
        assertNotNull("Users should be found", users);
        assertTrue("Should have users with email", users.size() >= 2);
        
        System.out.println("Found " + users.size() + " user(s) with email");
    }
    
    @Test
    public void testSelectWithOrderBy() {
        System.out.println("\n=== Test: SELECT with ORDER BY ===");
        
        List<TestUser> users = testMapper.findAllUsersOrderByDate();
        
        assertNotNull("Users should be found", users);
        
        System.out.println("Found " + users.size() + " user(s) ordered by date DESC");
    }
    
    @Test
    public void testSelectWithOrderByMultiple() {
        System.out.println("\n=== Test: SELECT with ORDER BY multiple columns ===");
        
        List<TestUser> users = testMapper.findAllUsersOrderByMultiple();
        
        assertNotNull("Users should be found", users);
        
        System.out.println("Found " + users.size() + " user(s) ordered by status ASC, created_at DESC");
    }
    
    @Test
    public void testSelectWithLimitOffset() {
        System.out.println("\n=== Test: SELECT with LIMIT offset, count (MySQL -> PostgreSQL) ===");
        
        // MySQL: LIMIT offset, count -> PostgreSQL: LIMIT count OFFSET offset
        List<TestUser> users = testMapper.findUserByPage(0, 10);
        
        assertNotNull("Users should be found", users);
        assertTrue("Should have at most 10 users", users.size() <= 10);
        
        System.out.println("Found " + users.size() + " user(s) with LIMIT 0, 10");
    }
    
    @Test
    public void testSelectWithLimitOnly() {
        System.out.println("\n=== Test: SELECT with LIMIT only ===");
        
        List<TestUser> users = testMapper.findUserWithLimit(5);
        
        assertNotNull("Users should be found", users);
        assertTrue("Should have at most 5 users", users.size() <= 5);
        
        System.out.println("Found " + users.size() + " user(s) with LIMIT 5");
    }
    
    @Test
    public void testSelectWithGroupBy() {
        System.out.println("\n=== Test: SELECT with GROUP BY ===");
        
        List<Map<String, Object>> results = testMapper.countUsersByStatus();
        
        assertNotNull("Results should be found", results);
        
        System.out.println("Group by status results: " + results);
    }
    
    @Test
    public void testSelectWithGroupByHaving() {
        System.out.println("\n=== Test: SELECT with GROUP BY and HAVING ===");
        
        List<Map<String, Object>> results = testMapper.countUsersByStatusHaving();
        
        assertNotNull("Results should be found", results);
        
        System.out.println("Group by status with HAVING results: " + results);
    }
    
    @Test
    public void testSelectWithDistinct() {
        System.out.println("\n=== Test: SELECT with DISTINCT ===");
        
        List<Integer> statuses = testMapper.findDistinctStatuses();
        
        assertNotNull("Statuses should be found", statuses);
        
        System.out.println("Distinct statuses: " + statuses);
    }
    
    @Test
    public void testSelectWithCount() {
        System.out.println("\n=== Test: SELECT with COUNT ===");
        
        long count = testMapper.countUsersByActive(true);
        
        assertTrue("Count should be >= 0", count >= 0);
        
        System.out.println("Active users count: " + count);
    }
    
    // ==================== JOIN Tests ====================
    
    @Test
    public void testSelectWithJoin() {
        System.out.println("\n=== Test: SELECT with JOIN ===");
        
        List<Map<String, Object>> results = testMapper.findUserOrdersByStatus(1);
        
        assertNotNull("Results should be found", results);
        
        System.out.println("JOIN results: " + results);
    }
    
    @Test
    public void testSelectWithLeftJoin() {
        System.out.println("\n=== Test: SELECT with LEFT JOIN ===");
        
        List<Map<String, Object>> results = testMapper.findAllUsersWithOrders();
        
        assertNotNull("Results should be found", results);
        
        System.out.println("LEFT JOIN results count: " + results.size());
    }
    
    @Test
    public void testSelectWithSubquery() {
        System.out.println("\n=== Test: SELECT with subquery ===");
        
        List<TestUser> users = testMapper.findUsersWithOrdersAboveAmount(new BigDecimal("50"));
        
        assertNotNull("Users should be found", users);
        
        System.out.println("Users with orders > 50: " + users.size());
    }
    
    // ==================== INSERT Tests ====================
    
    @Test
    public void testInsert() {
        System.out.println("\n=== Test: INSERT with column names ===");
        
        TestUser user = new TestUser("insert_test", "insert@test.com", 3, true);
        user.setCreatedAt(new Date());
        
        int rows = testMapper.insertUser(user);
        
        assertEquals("Should insert 1 row", 1, rows);
        assertNotNull("ID should be generated", user.getId());
        
        // Verify
        TestUser found = testMapper.findUserById(user.getId());
        assertNotNull("User should be found after insert", found);
        assertEquals("User name should match", "insert_test", found.getUserName());
        
        System.out.println("Inserted user with ID: " + user.getId());
    }
    
    @Test
    public void testInsertWithBoolean() {
        System.out.println("\n=== Test: INSERT with boolean value (TINYINT -> BOOLEAN) ===");
        
        TestUser user = new TestUser("bool_test", "bool@test.com", 1, null);
        user.setCreatedAt(new Date());
        
        int rows = testMapper.insertUserActive(user);
        
        assertEquals("Should insert 1 row", 1, rows);
        
        // Verify boolean was converted correctly
        TestUser found = testMapper.findUserById(user.getId());
        assertTrue("isActive should be true", found.getIsActive());
        
        System.out.println("Inserted user with isActive=true, found: " + found.getIsActive());
    }
    
    // ==================== UPDATE Tests ====================
    
    @Test
    public void testUpdate() {
        System.out.println("\n=== Test: UPDATE ===");
        
        testUser1.setUserName("updated_name");
        testUser1.setEmail("updated@test.com");
        testUser1.setStatus(5);
        
        int rows = testMapper.updateUser(testUser1);
        
        assertEquals("Should update 1 row", 1, rows);
        
        // Verify
        TestUser found = testMapper.findUserById(testUser1.getId());
        assertEquals("User name should be updated", "updated_name", found.getUserName());
        assertEquals("Email should be updated", "updated@test.com", found.getEmail());
        
        System.out.println("Updated user: " + found);
    }
    
    @Test
    public void testUpdateBoolean() {
        System.out.println("\n=== Test: UPDATE boolean column ===");
        
        int rows = testMapper.updateUserActive(testUser1.getId(), false);
        
        assertEquals("Should update 1 row", 1, rows);
        
        // Verify
        TestUser found = testMapper.findUserById(testUser1.getId());
        assertFalse("isActive should be false", found.getIsActive());
        
        System.out.println("Updated isActive to false: " + found.getIsActive());
    }
    
    @Test
    public void testUpdateWithAlias() {
        System.out.println("\n=== Test: UPDATE with table alias ===");
        
        int rows = testMapper.updateUserStatusWithAlias(testUser1.getId(), 99);
        
        assertEquals("Should update 1 row", 1, rows);
        
        // Verify
        TestUser found = testMapper.findUserById(testUser1.getId());
        assertEquals("Status should be updated", Integer.valueOf(99), found.getStatus());
        
        System.out.println("Updated status with alias: " + found.getStatus());
    }
    
    @Test
    public void testUpdateWithConditions() {
        System.out.println("\n=== Test: UPDATE with multiple conditions ===");
        
        int rows = testMapper.deactivateUsersByStatus(1);
        
        assertTrue("Should update at least 0 rows", rows >= 0);
        
        System.out.println("Deactivated " + rows + " user(s) with status 1");
    }
    
    // ==================== DELETE Tests ====================
    
    @Test
    public void testDeleteById() {
        System.out.println("\n=== Test: DELETE by ID ===");
        
        Long id = testUser1.getId();
        int rows = testMapper.deleteUserById(id);
        
        assertEquals("Should delete 1 row", 1, rows);
        
        // Verify
        TestUser found = testMapper.findUserById(id);
        assertNull("User should be deleted", found);
        
        System.out.println("Deleted user with ID: " + id);
    }
    
    @Test
    public void testDeleteWithConditions() {
        System.out.println("\n=== Test: DELETE with multiple conditions ===");
        
        // Create a user to delete
        TestUser user = new TestUser("delete_test", "delete@test.com", 999, false);
        user.setCreatedAt(new Date());
        testMapper.insertUser(user);
        
        int rows = testMapper.deleteUsersByStatusAndActive(999, false);
        
        assertTrue("Should delete at least 1 row", rows >= 1);
        
        System.out.println("Deleted " + rows + " user(s) with status 999 and active=false");
    }
    
    // ==================== Order Tests ====================
    
    @Test
    public void testSelectOrder() {
        System.out.println("\n=== Test: SELECT order with column name mapping ===");
        
        TestOrder found = testMapper.findOrderById(testOrder1.getId());
        
        assertNotNull("Order should be found", found);
        assertEquals("Order number should match", testOrder1.getOrderNo(), found.getOrderNo());
        
        System.out.println("Found order: " + found);
    }
    
    @Test
    public void testInsertOrder() {
        System.out.println("\n=== Test: INSERT order ===");
        
        TestOrder order = new TestOrder(testUser1.getId(), "ORD-NEW-" + System.currentTimeMillis(),
                new BigDecimal("200.00"), "completed");
        
        int rows = testMapper.insertOrder(order);
        
        assertEquals("Should insert 1 row", 1, rows);
        assertNotNull("ID should be generated", order.getId());
        
        System.out.println("Inserted order with ID: " + order.getId());
    }
    
    @Test
    public void testSelectOrdersByUserId() {
        System.out.println("\n=== Test: SELECT orders by user ID ===");
        
        List<TestOrder> orders = testMapper.findOrdersByUserId(testUser1.getId());
        
        assertNotNull("Orders should be found", orders);
        assertTrue("Should have at least 1 order", orders.size() >= 1);
        
        System.out.println("Found " + orders.size() + " order(s) for user " + testUser1.getId());
    }
    
    @Test
    public void testUpdateOrder() {
        System.out.println("\n=== Test: UPDATE order ===");
        
        testOrder1.setOrderNo("ORD-UPDATED-" + System.currentTimeMillis());
        testOrder1.setStatus("completed");
        
        int rows = testMapper.updateOrder(testOrder1);
        
        assertEquals("Should update 1 row", 1, rows);
        
        System.out.println("Updated order: " + testOrder1);
    }
    
    @Test
    public void testDeleteOrder() {
        System.out.println("\n=== Test: DELETE order ===");
        
        Long id = testOrder1.getId();
        int rows = testMapper.deleteOrderById(id);
        
        assertEquals("Should delete 1 row", 1, rows);
        
        // Verify
        TestOrder found = testMapper.findOrderById(id);
        assertNull("Order should be deleted", found);
        
        System.out.println("Deleted order with ID: " + id);
    }
}
