-- 校园教室预约管理系统数据库结构
-- 目标数据库：MySQL 8.0+

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    `department` VARCHAR(100) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_username` (`username`),
    CONSTRAINT `chk_user_role` CHECK (`role` IN ('STUDENT', 'ADMIN')),
    CONSTRAINT `chk_user_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `classroom` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `room_no` VARCHAR(50) NOT NULL,
    `building` VARCHAR(100) NOT NULL,
    `capacity` INT NOT NULL,
    `seat_count` INT NOT NULL,
    `equipment` VARCHAR(255) NULL,
    `current_head_count` INT NOT NULL DEFAULT 0,
    `current_occupancy_rate` DECIMAL(7,2) NOT NULL DEFAULT 0.00,
    `occupancy_updated_at` DATETIME NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_classroom_room_no` (`room_no`),
    KEY `idx_classroom_status` (`status`),
    CONSTRAINT `chk_classroom_capacity` CHECK (`capacity` > 0),
    CONSTRAINT `chk_classroom_seat_count` CHECK (`seat_count` > 0),
    CONSTRAINT `chk_classroom_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `booking` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `classroom_id` BIGINT NOT NULL,
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME NOT NULL,
    `purpose` VARCHAR(255) NOT NULL,
    `expected_attendees` INT NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `approver_id` BIGINT NULL,
    `reviewed_at` DATETIME NULL,
    `reject_reason` VARCHAR(255) NULL,
    `cancelled_at` DATETIME NULL,
    `released_at` DATETIME NULL,
    `release_reason` VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_booking_classroom_time_status` (`classroom_id`, `status`, `start_time`, `end_time`),
    KEY `idx_booking_user_start` (`user_id`, `start_time`),
    CONSTRAINT `fk_booking_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_booking_classroom` FOREIGN KEY (`classroom_id`) REFERENCES `classroom` (`id`),
    CONSTRAINT `fk_booking_approver` FOREIGN KEY (`approver_id`) REFERENCES `user` (`id`),
    CONSTRAINT `chk_booking_time` CHECK (`start_time` < `end_time`),
    CONSTRAINT `chk_booking_attendees` CHECK (`expected_attendees` > 0),
    CONSTRAINT `chk_booking_status` CHECK (`status` IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'RELEASED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `ai_device` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `device_id` VARCHAR(100) NOT NULL,
    `classroom_id` BIGINT NOT NULL,
    `token_hash` VARCHAR(255) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `report_interval_seconds` INT NOT NULL DEFAULT 30,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_ai_device_device_id` (`device_id`),
    CONSTRAINT `fk_ai_device_classroom` FOREIGN KEY (`classroom_id`) REFERENCES `classroom` (`id`),
    CONSTRAINT `chk_ai_device_status` CHECK (`status` IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT `chk_ai_device_interval` CHECK (`report_interval_seconds` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `occupancy_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `classroom_id` BIGINT NOT NULL,
    `head_count` INT NOT NULL,
    `seat_count` INT NOT NULL,
    `occupancy_rate` DECIMAL(7,2) NOT NULL,
    `collected_at` DATETIME NOT NULL,
    `source` VARCHAR(20) NOT NULL DEFAULT 'REAL',
    `device_id` VARCHAR(100) NOT NULL,
    `sequence_no` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_occupancy_classroom_collected` (`classroom_id`, `collected_at`),
    UNIQUE KEY `uk_occupancy_device_sequence` (`device_id`, `sequence_no`),
    CONSTRAINT `fk_occupancy_classroom` FOREIGN KEY (`classroom_id`) REFERENCES `classroom` (`id`),
    CONSTRAINT `chk_occupancy_head_count` CHECK (`head_count` >= 0),
    CONSTRAINT `chk_occupancy_seat_count` CHECK (`seat_count` > 0),
    CONSTRAINT `chk_occupancy_source` CHECK (`source` IN ('REAL', 'SIMULATED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
