package org.example.server;
import org.example.server.config.TestSchemaInitializer;
import org.example.server.entity.Product;
import org.example.server.mapper.ProductMapper;

import org.example.server.entity.User;
import org.example.server.mapper.UserMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration test for MyBatis interceptor with H2 in PostgreSQL mode.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestSchemaInitializer.class)
public class MapperIntegrationTest {
    
    @Autowired
    private UserMapper userMapper;
    
    @Test
    public void testInsert() {
        User user = new User();
        user.setUserName("test_user_" + System.currentTimeMillis());
        user.setAge(25);
        user.setEmail("test@example.com");
        user.setCreateTime(new Date());
        
        int rows = userMapper.insert(user);
        System.out.println("Insert result: " + rows + " row(s) affected, generated id: " + user.getId());
        
        assertTrue(rows > 0);
        assertNotNull(user.getId());
    }
    
    @Test
    public void testInsertWithoutColumns() {
        // Test INSERT without column names
        // MySQL: INSERT INTO user VALUES (?, ?, ?, ?, ?)
        // Should be converted to: INSERT INTO "user" ("id", "user_name", "age", "email", "create_time") VALUES (?, ?, ?, ?, ?)
        User user = new User();
        user.setId(System.currentTimeMillis()); // Set ID manually for this test
        user.setUserName("no_cols_test_" + System.currentTimeMillis());
        user.setAge(28);
        user.setEmail("nocols@example.com");
        user.setCreateTime(new Date());
        
        int rows = userMapper.insertWithoutColumns(user);
        System.out.println("Insert without columns result: " + rows + " row(s) affected, id: " + user.getId());
        
        assertTrue(rows > 0);
        
        // Verify data was inserted correctly
        User found = userMapper.findById(user.getId());
        assertNotNull(found);
        assertEquals(user.getUserName(), found.getUserName());
        assertEquals(user.getAge(), found.getAge());
        System.out.println("Verified inserted user: " + found);
    }
    
    @Test
    public void testSelectById() {
        // First insert a user
        User user = new User();
        user.setUserName("select_test_" + System.currentTimeMillis());
        user.setAge(30);
        user.setEmail("select@example.com");
        user.setCreateTime(new Date());
        userMapper.insert(user);
        
        // Then select
        User found = userMapper.findById(user.getId());
        System.out.println("Found user: " + found);
        
        assertNotNull(found);
        assertEquals(user.getUserName(), found.getUserName());
        assertEquals(user.getAge(), found.getAge());
    }
    
    @Test
    public void testSelectAll() {
        List<User> users = userMapper.findAll();
        System.out.println("Total users: " + users.size());
        
        assertNotNull(users);
    }
    
    @Test
    public void testUpdate() {
        // First insert a user
        User user = new User();
        user.setUserName("update_test_" + System.currentTimeMillis());
        user.setAge(20);
        user.setEmail("update@example.com");
        user.setCreateTime(new Date());
        userMapper.insert(user);
        
        // Then update
        user.setUserName("updated_name");
        user.setAge(21);
        int rows = userMapper.update(user);
        System.out.println("Update result: " + rows + " row(s) affected");
        
        assertEquals(1, rows);
        
        // Verify
        User updated = userMapper.findById(user.getId());
        assertEquals("updated_name", updated.getUserName());
        assertEquals(Integer.valueOf(21), updated.getAge());
    }
    
    @Test
    public void testDelete() {
        // First insert a user
        User user = new User();
        user.setUserName("delete_test_" + System.currentTimeMillis());
        user.setAge(40);
        user.setEmail("delete@example.com");
        user.setCreateTime(new Date());
        userMapper.insert(user);
        
        Long id = user.getId();
        
        // Then delete
        int rows = userMapper.deleteById(id);
        System.out.println("Delete result: " + rows + " row(s) affected");
        
        assertEquals(1, rows);
        
        // Verify deleted
        User deleted = userMapper.findById(id);
        assertNull(deleted);
    }
    
    @Test
    public void testLimitOffset() {
        // Insert some test data
        for (int i = 0; i < 5; i++) {
            User user = new User();
            user.setUserName("limit_test_" + i + "_" + System.currentTimeMillis());
            user.setAge(20 + i);
            user.setEmail("limit" + i + "@example.com");
            user.setCreateTime(new Date());
            userMapper.insert(user);
        }
        
        // Test LIMIT with offset (MySQL style: LIMIT offset, count)
        // Should be converted to PostgreSQL: LIMIT count OFFSET offset
        List<User> users = userMapper.findByPage(2, 3);
        System.out.println("Page query result: " + users.size() + " user(s)");
        
        assertNotNull(users);
        assertTrue(users.size() <= 3);
}

    // ========== Product Tests (Type Conversion) ==========
    // MySQL: price VARCHAR, stock INT
    // PostgreSQL: price INT, stock VARCHAR
    // Tests type conversion: VARCHAR->INT and INT->VARCHAR
    
    @Autowired
    private ProductMapper productMapper;
    
    @Test
    public void testProductInsertWithTypeConversion() {
        Product product = new Product();
        product.setProductName("Test Product " + System.currentTimeMillis());
        product.setPrice("199");      // VARCHAR in MySQL -> INT in PostgreSQL
        product.setStock(100);        // INT in MySQL -> VARCHAR in PostgreSQL
        product.setDescription("Test product for type conversion");
        
        int rows = productMapper.insert(product);
        System.out.println("Product insert result: " + rows + " row(s) affected, id: " + product.getId());
        
        assertTrue(rows > 0);
        assertNotNull(product.getId());
        
        // Verify data was inserted correctly
        Product found = productMapper.findById(product.getId());
        assertNotNull(found);
        assertEquals(product.getProductName(), found.getProductName());
        System.out.println("Found product: " + found);
    }
    
    @Test
    public void testProductSelectAll() {
        List<Product> products = productMapper.findAll();
        System.out.println("Total products: " + products.size());
        assertNotNull(products);
    }
}
