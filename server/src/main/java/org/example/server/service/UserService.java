package org.example.server.service;

import org.example.server.entity.User;
import org.example.server.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public User findById(Long id) {
        return userMapper.findById(id);
    }
    
    public List<User> findAll() {
        return userMapper.findAll();
    }
    
    @Transactional
    public int create(User user) {
        return userMapper.insert(user);
    }
    
    @Transactional
    public int update(User user) {
        return userMapper.update(user);
    }
    
    @Transactional
    public int delete(Long id) {
        return userMapper.deleteById(id);
    }
    
    public List<User> findByPage(int offset, int limit) {
        return userMapper.findByPage(offset, limit);
    }
}
