package org.example.server.mapper;

import org.apache.ibatis.annotations.*;
import org.example.server.entity.Product;

import java.util.List;

@Mapper
public interface ProductMapper {
    
    @Select("SELECT id, product_name, price, stock, description FROM product WHERE id = #{id}")
    Product findById(@Param("id") Long id);
    
    @Select("SELECT id, product_name, price, stock, description FROM product ORDER BY id")
    List<Product> findAll();
    
    // Test INSERT with type conversion: price (VARCHAR->INT), stock (INT->VARCHAR)
    @Insert("INSERT INTO product (product_name, price, stock, description) VALUES (#{productName}, #{price}, #{stock}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);
    
    @Update("UPDATE product SET product_name = #{productName}, price = #{price}, stock = #{stock} WHERE id = #{id}")
    int update(Product product);
    
    @Delete("DELETE FROM product WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
