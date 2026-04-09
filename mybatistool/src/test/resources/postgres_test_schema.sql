-- PostgreSQL Schema for Testing
-- Column names are different from MySQL to test mapping

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

-- Sample data (with boolean true/false instead of 1/0)
INSERT INTO "user" ("id", "name", "email", "status", "active") VALUES
(1, 'Alice', 'alice@example.com', 1, TRUE),
(2, 'Bob', 'bob@example.com', 2, TRUE),
(3, 'Charlie', 'charlie@example.com', 1, FALSE);

INSERT INTO "order" ("id", "user_id", "order_number", "amount", "status") VALUES
(1, 1, 'ORD001', 100.50, 'completed'),
(2, 1, 'ORD002', 200.00, 'pending'),
(3, 2, 'ORD003', 150.75, 'completed'),
(4, 3, 'ORD004', 50.00, 'cancelled');
