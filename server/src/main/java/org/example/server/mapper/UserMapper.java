package org.example.server.mapper;

import org.apache.ibatis.annotations.*;
import org.example.server.entity.User;

import java.util.List;

@Mapper
public interface UserMapper {
    
    @Select("SELECT id, user_name, age, email, create_time FROM user WHERE id = #{id}")
    User findById(@Param("id") Long id);
    
    @Select("SELECT id, user_name, age, email, create_time FROM user ORDER BY id")
    List<User> findAll();
    
    @Insert("INSERT INTO user (user_name, age, email, create_time) VALUES (#{userName}, #{age}, #{email}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
    
    // Test INSERT without column names - values must match PostgreSQL column order
    // MySQL: INSERT INTO user VALUES (id, user_name, age, email, create_time)
    // PostgreSQL: Same order in this case
    @Insert("INSERT INTO user VALUES (#{id}, #{userName}, #{age}, #{email}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertWithoutColumns(User user);
    
    @Update("UPDATE user SET user_name = #{userName}, age = #{age}, email = #{email} WHERE id = #{id}")
    int update(User user);
    
    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
    
    // Test query with LIMIT (MySQL style)
    @Select("SELECT id, user_name, age, email, create_time FROM user LIMIT #{offset}, #{limit}")
    List<User> findByPage(@Param("offset") int offset, @Param("limit") int limit);
}
