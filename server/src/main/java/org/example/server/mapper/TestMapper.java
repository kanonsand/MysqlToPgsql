package org.example.server.mapper;

import org.apache.ibatis.annotations.*;
import org.example.server.entity.TestUser;
import org.example.server.entity.TestOrder;

import java.util.List;
import java.util.Map;

/**
 * Comprehensive test mapper using MySQL syntax.
 * All queries will be rewritten for PostgreSQL.
 * 
 * Tests:
 * - Column name mapping: user_name->name, is_active->active, order_no->order_number
 * - Boolean conversion: TINYINT(1) -> BOOLEAN
 * - All ClauseHandlers: SELECT, FROM, WHERE, GROUP BY, HAVING, ORDER BY, LIMIT
 * - All RewriteHandlers: SELECT, INSERT, UPDATE, DELETE
 */
@Mapper
public interface TestMapper {
    
    // ==================== SELECT Tests ====================
    
    /**
     * Simple SELECT with column name mapping.
     * MySQL: SELECT id, user_name, is_active FROM user WHERE id = ?
     * PostgreSQL: SELECT "id", "name" AS "user_name", "active" AS "is_active" FROM "user" WHERE "id" = ?
     */
    @Select("SELECT id, user_name, email, status, created_at, is_active FROM user WHERE id = #{id}")
    TestUser findUserById(@Param("id") Long id);
    
    /**
     * SELECT with table alias.
     */
    @Select("SELECT u.id, u.user_name, u.email, u.status, u.is_active FROM user u WHERE u.id = #{id}")
    TestUser findUserByIdWithAlias(@Param("id") Long id);
    
    /**
     * SELECT with multiple WHERE conditions.
     */
    @Select("SELECT * FROM user WHERE status = #{status} AND is_active = #{isActive}")
    List<TestUser> findUserByStatusAndActive(@Param("status") Integer status, @Param("isActive") Boolean isActive);
    
    /**
     * SELECT with LIKE.
     */
    @Select("SELECT * FROM user WHERE user_name LIKE CONCAT('%', #{name}, '%')")
    List<TestUser> findUserByNameLike(@Param("name") String name);
    
    /**
     * SELECT with IN clause.
     */
    @Select("SELECT * FROM user WHERE status IN (1, 2, 3)")
    List<TestUser> findUserByStatusIn();
    
    /**
     * SELECT with BETWEEN.
     */
    @Select("SELECT * FROM user WHERE created_at BETWEEN #{start} AND #{end}")
    List<TestUser> findUserByDateRange(@Param("start") java.util.Date start, @Param("end") java.util.Date end);
    
    /**
     * SELECT with IS NULL / IS NOT NULL.
     */
    @Select("SELECT * FROM user WHERE email IS NOT NULL")
    List<TestUser> findUserWithEmail();
    
    /**
     * SELECT with ORDER BY.
     */
    @Select("SELECT * FROM user ORDER BY created_at DESC")
    List<TestUser> findAllUsersOrderByDate();
    
    /**
     * SELECT with ORDER BY multiple columns.
     */
    @Select("SELECT * FROM user ORDER BY status ASC, created_at DESC")
    List<TestUser> findAllUsersOrderByMultiple();
    
    /**
     * SELECT with LIMIT (MySQL syntax -> PostgreSQL).
     * MySQL: LIMIT offset, count
     * PostgreSQL: LIMIT count OFFSET offset
     */
    @Select("SELECT * FROM user LIMIT #{offset}, #{limit}")
    List<TestUser> findUserByPage(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * SELECT with LIMIT only.
     */
    @Select("SELECT * FROM user LIMIT #{limit}")
    List<TestUser> findUserWithLimit(@Param("limit") int limit);
    
    /**
     * SELECT with GROUP BY and COUNT.
     */
    @Select("SELECT status, COUNT(*) as cnt FROM user GROUP BY status")
    List<Map<String, Object>> countUsersByStatus();
    
    /**
     * SELECT with GROUP BY, HAVING.
     */
    @Select("SELECT status, COUNT(*) as cnt FROM user GROUP BY status HAVING COUNT(*) > 1")
    List<Map<String, Object>> countUsersByStatusHaving();
    
    /**
     * SELECT with DISTINCT.
     */
    @Select("SELECT DISTINCT status FROM user")
    List<Integer> findDistinctStatuses();
    
    /**
     * SELECT with aggregate functions.
     */
    @Select("SELECT COUNT(*) FROM user WHERE is_active = #{isActive}")
    long countUsersByActive(@Param("isActive") Boolean isActive);
    
    // ==================== JOIN Tests ====================
    
    /**
     * SELECT with JOIN - tests multi-table column mapping.
     */
    @Select("SELECT u.id, u.user_name, o.order_no, o.amount " +
            "FROM user u JOIN `order` o ON u.id = o.user_id " +
            "WHERE u.status = #{status}")
    List<Map<String, Object>> findUserOrdersByStatus(@Param("status") Integer status);
    
    /**
     * SELECT with LEFT JOIN.
     */
    @Select("SELECT u.id, u.user_name, o.order_no " +
            "FROM user u LEFT JOIN `order` o ON u.id = o.user_id")
    List<Map<String, Object>> findAllUsersWithOrders();
    
    /**
     * SELECT with subquery.
     */
    @Select("SELECT * FROM user WHERE id IN (SELECT user_id FROM `order` WHERE amount > #{amount})")
    List<TestUser> findUsersWithOrdersAboveAmount(@Param("amount") java.math.BigDecimal amount);
    
    // ==================== INSERT Tests ====================
    
    /**
     * INSERT with column names.
     * MySQL column names will be mapped to PostgreSQL names.
     */
    @Insert("INSERT INTO user (user_name, email, status, created_at, is_active) " +
            "VALUES (#{userName}, #{email}, #{status}, #{createdAt}, #{isActive})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(TestUser user);
    
    /**
     * INSERT without column names - values must match PostgreSQL column order.
     * PostgreSQL column order: id, name, email, status, created_at, active
     */
    @Insert("INSERT INTO user VALUES (#{id}, #{userName}, #{email}, #{status}, #{createdAt}, #{isActive})")
    int insertUserWithoutColumns(TestUser user);
    
    /**
     * INSERT with boolean value - tests TINYINT->BOOLEAN conversion.
     */
    @Insert("INSERT INTO user (user_name, email, status, created_at, is_active) " +
            "VALUES (#{userName}, #{email}, #{status}, #{createdAt}, true)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUserActive(TestUser user);
    
    // ==================== UPDATE Tests ====================
    
    /**
     * UPDATE with column name mapping.
     */
    @Update("UPDATE user SET user_name = #{userName}, email = #{email}, status = #{status} WHERE id = #{id}")
    int updateUser(TestUser user);
    
    /**
     * UPDATE with boolean column.
     */
    @Update("UPDATE user SET is_active = #{isActive} WHERE id = #{id}")
    int updateUserActive(@Param("id") Long id, @Param("isActive") Boolean isActive);
    
    /**
     * UPDATE with table alias.
     */
    @Update("UPDATE user u SET u.status = #{status} WHERE u.id = #{id}")
    int updateUserStatusWithAlias(@Param("id") Long id, @Param("status") Integer status);
    
    /**
     * UPDATE with WHERE conditions.
     */
    @Update("UPDATE user SET is_active = false WHERE status = #{status} AND is_active = true")
    int deactivateUsersByStatus(@Param("status") Integer status);
    
    // ==================== DELETE Tests ====================
    
    /**
     * DELETE by ID.
     */
    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteUserById(@Param("id") Long id);
    
    /**
     * DELETE with multiple conditions.
     */
    @Delete("DELETE FROM user WHERE status = #{status} AND is_active = #{isActive}")
    int deleteUsersByStatusAndActive(@Param("status") Integer status, @Param("isActive") Boolean isActive);
    
    // ==================== Order Tests ====================
    
    /**
     * INSERT order with column name mapping.
     */
    @Insert("INSERT INTO `order` (user_id, order_no, amount, status, created_time) " +
            "VALUES (#{userId}, #{orderNo}, #{amount}, #{status}, #{createdTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrder(TestOrder order);
    
    /**
     * SELECT order with column name mapping.
     */
    @Select("SELECT id, user_id, order_no, amount, status, created_time FROM `order` WHERE id = #{id}")
    TestOrder findOrderById(@Param("id") Long id);
    
    /**
     * SELECT orders by user ID.
     */
    @Select("SELECT * FROM `order` WHERE user_id = #{userId} ORDER BY created_time DESC")
    List<TestOrder> findOrdersByUserId(@Param("userId") Long userId);
    
    /**
     * UPDATE order with column name mapping.
     */
    @Update("UPDATE `order` SET order_no = #{orderNo}, status = #{status} WHERE id = #{id}")
    int updateOrder(TestOrder order);
    
    /**
     * DELETE order.
     */
    @Delete("DELETE FROM `order` WHERE id = #{id}")
    int deleteOrderById(@Param("id") Long id);
}
