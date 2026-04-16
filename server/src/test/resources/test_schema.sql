-- Test Schema for H2 in PostgreSQL mode
-- Includes tables for both TestMapper (user, order) and UserMapper/ProductMapper (user, product)

-- Drop tables if exist
DROP TABLE IF EXISTS "order";
DROP TABLE IF EXISTS "product";
DROP TABLE IF EXISTS "user";

-- User table (for TestMapper - comprehensive tests)
-- Column mapping: user_name -> name, is_active -> active
CREATE TABLE "user" (
    "id" BIGSERIAL PRIMARY KEY,
    "name" VARCHAR(100),                    -- user_name -> name
    "email" VARCHAR(200),
    "status" INTEGER DEFAULT 1,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "active" BOOLEAN DEFAULT TRUE,          -- is_active -> active (TINYINT -> BOOLEAN)
    
    -- Additional columns for UserMapper tests
    "age" INTEGER,
    "create_time" TIMESTAMP
);

-- Order table (for TestMapper - comprehensive tests)  
-- Column mapping: order_no -> order_number
CREATE TABLE "order" (
    "id" BIGSERIAL PRIMARY KEY,
    "user_id" BIGINT NOT NULL,
    "order_number" VARCHAR(50),             -- order_no -> order_number
    "amount" DECIMAL(10,2),
    "status" VARCHAR(20) DEFAULT 'pending',
    "created_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY ("user_id") REFERENCES "user"("id")
);

-- Product table (for ProductMapper tests)
CREATE TABLE "product" (
    "id" BIGSERIAL PRIMARY KEY,
    "product_name" VARCHAR(200),
    "price" DECIMAL(10,2),
    "stock" INTEGER DEFAULT 0,
    "description" TEXT,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_order_user_id ON "order"("user_id");
CREATE INDEX idx_user_name ON "user"("name");
