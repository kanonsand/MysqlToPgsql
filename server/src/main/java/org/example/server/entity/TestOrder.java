package org.example.server.entity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Order entity with MySQL column names.
 * Column name mapping (MySQL -> PostgreSQL):
 * - order_no -> order_number
 */
public class TestOrder {
    private Long id;
    private Long userId;
    private String orderNo;       // MySQL: order_no -> PostgreSQL: order_number
    private BigDecimal amount;
    private String status;
    private Date createdTime;
    
    public TestOrder() {}
    
    public TestOrder(Long userId, String orderNo, BigDecimal amount, String status) {
        this.userId = userId;
        this.orderNo = orderNo;
        this.amount = amount;
        this.status = status;
        this.createdTime = new Date();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getOrderNo() {
        return orderNo;
    }
    
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Date getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }
    
    @Override
    public String toString() {
        return "TestOrder{" +
                "id=" + id +
                ", userId=" + userId +
                ", orderNo='" + orderNo + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", createdTime=" + createdTime +
                '}';
    }
}
