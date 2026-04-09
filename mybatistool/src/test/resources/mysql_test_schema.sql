-- MySQL Schema for Testing
-- Two tables for JOIN testing

DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(200),
    `status` INT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `is_active` TINYINT(1) DEFAULT 1
);

CREATE TABLE `order` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `order_no` VARCHAR(50) NOT NULL,
    `amount` DECIMAL(10,2),
    `status` VARCHAR(20) DEFAULT 'pending',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

-- Sample data
INSERT INTO `user` (`id`, `user_name`, `email`, `status`, `is_active`) VALUES
(1, 'Alice', 'alice@example.com', 1, 1),
(2, 'Bob', 'bob@example.com', 2, 1),
(3, 'Charlie', 'charlie@example.com', 1, 0);

INSERT INTO `order` (`id`, `user_id`, `order_no`, `amount`, `status`) VALUES
(1, 1, 'ORD001', 100.50, 'completed'),
(2, 1, 'ORD002', 200.00, 'pending'),
(3, 2, 'ORD003', 150.75, 'completed'),
(4, 3, 'ORD004', 50.00, 'cancelled');
