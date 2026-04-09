package org.example.server.entity;

import java.util.Date;

/**
 * User entity with MySQL column names.
 * Column name mapping (MySQL -> PostgreSQL):
 * - user_name -> name
 * - is_active -> active
 */
public class TestUser {
    private Long id;
    private String userName;      // MySQL: user_name -> PostgreSQL: name
    private String email;
    private Integer status;
    private Date createdAt;       // created_at
    private Boolean isActive;     // MySQL: is_active -> PostgreSQL: active (TINYINT -> BOOLEAN)
    
    public TestUser() {}
    
    public TestUser(String userName, String email, Integer status, Boolean isActive) {
        this.userName = userName;
        this.email = email;
        this.status = status;
        this.isActive = isActive;
        this.createdAt = new Date();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    @Override
    public String toString() {
        return "TestUser{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", isActive=" + isActive +
                '}';
    }
}
