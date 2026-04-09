package org.example.server.entity;

import java.util.Date;

/**
 * Product entity for testing type conversion.
 * MySQL: price VARCHAR, stock INT
 * PostgreSQL: price INT, stock VARCHAR
 */
public class Product {
    
    private Long id;
    private String productName;
    private String price;    // MySQL: VARCHAR, PostgreSQL: INT (will convert)
    private Integer stock;   // MySQL: INT, PostgreSQL: VARCHAR (will convert)
    private String description;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public String getPrice() {
        return price;
    }
    
    public void setPrice(String price) {
        this.price = price;
    }
    
    public Integer getStock() {
        return stock;
    }
    
    public void setStock(Integer stock) {
        this.stock = stock;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", productName='" + productName + '\'' +
                ", price='" + price + '\'' +
                ", stock=" + stock +
                ", description='" + description + '\'' +
                '}';
    }
}
