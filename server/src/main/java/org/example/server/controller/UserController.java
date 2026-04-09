package org.example.server.controller;

import org.example.server.entity.User;
import org.example.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userService.findById(id);
    }
    
    @GetMapping
    public List<User> getAll() {
        return userService.findAll();
    }
    
    @PostMapping
    public String create(@RequestBody User user) {
        userService.create(user);
        return "Created user with id: " + user.getId();
    }
    
    @PutMapping("/{id}")
    public String update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        int rows = userService.update(user);
        return "Updated " + rows + " row(s)";
    }
    
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        int rows = userService.delete(id);
        return "Deleted " + rows + " row(s)";
    }
    
    @GetMapping("/page")
    public List<User> getByPage(@RequestParam(defaultValue = "0") int offset,
                                @RequestParam(defaultValue = "10") int limit) {
        return userService.findByPage(offset, limit);
    }
}
