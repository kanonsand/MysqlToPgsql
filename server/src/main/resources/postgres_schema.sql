-- PostgreSQL Schema (target)
-- Column names are different from MySQL to test mapping:
-- user.user_name -> user.name
-- user.is_active -> user.active (TINYINT -> BOOLEAN)
-- order.order_no -> order.order_number

DROP TABLE IF EXISTS "order";
DROP TABLE IF EXISTS "user";

CREATE TABLE "user" (
    "id" BIGSERIAL PRIMARY KEY,
    "name" VARCHAR(100) NOT NULL,           -- user_name -> name
    "email" VARCHAR(200),
    "status" INTEGER DEFAULT 1,
    "created_at" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "active" BOOLEAN DEFAULT TRUE           -- is_active -> active (TINYINT -> BOOLEAN)
);

CREATE TABLE "order" (
    "id" BIGSERIAL PRIMARY KEY,
    "user_id" BIGINT NOT NULL,
    "order_number" VARCHAR(50) NOT NULL,    -- order_no -> order_number
    "amount" DECIMAL(10,2),
    "status" VARCHAR(20) DEFAULT 'pending',
    "created_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY ("user_id") REFERENCES "user"("id")
);

-- Create indexes
CREATE INDEX idx_order_user_id ON "order"("user_id");