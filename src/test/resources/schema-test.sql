DROP TABLE IF EXISTS `occupancy_record`;
DROP TABLE IF EXISTS `ai_device`;
DROP TABLE IF EXISTS `booking`;
DROP TABLE IF EXISTS `classroom`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `department` VARCHAR(100) NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `classroom` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `room_no` VARCHAR(50) NOT NULL UNIQUE,
    `building` VARCHAR(100) NOT NULL,
    `capacity` INT NOT NULL,
    `seat_count` INT NOT NULL,
    `equipment` VARCHAR(255),
    `current_head_count` INT NOT NULL DEFAULT 0,
    `current_occupancy_rate` DECIMAL(7,2) NOT NULL DEFAULT 0,
    `occupancy_updated_at` TIMESTAMP,
    `status` VARCHAR(20) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `booking` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `classroom_id` BIGINT NOT NULL,
    `start_time` TIMESTAMP NOT NULL,
    `end_time` TIMESTAMP NOT NULL,
    `purpose` VARCHAR(255) NOT NULL,
    `expected_attendees` INT NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    `approver_id` BIGINT,
    `reviewed_at` TIMESTAMP,
    `reject_reason` VARCHAR(255),
    `cancelled_at` TIMESTAMP,
    `released_at` TIMESTAMP,
    `release_reason` VARCHAR(255),
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `ai_device` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `device_id` VARCHAR(100) NOT NULL UNIQUE,
    `classroom_id` BIGINT NOT NULL,
    `token_hash` VARCHAR(255) NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    `report_interval_seconds` INT NOT NULL DEFAULT 30,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `occupancy_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `classroom_id` BIGINT NOT NULL,
    `head_count` INT NOT NULL,
    `seat_count` INT NOT NULL,
    `occupancy_rate` DECIMAL(7,2) NOT NULL,
    `collected_at` TIMESTAMP NOT NULL,
    `source` VARCHAR(20) NOT NULL,
    `device_id` VARCHAR(100) NOT NULL,
    `sequence_no` BIGINT NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (`device_id`, `sequence_no`)
);

INSERT INTO `classroom`
    (`room_no`, `building`, `capacity`, `seat_count`, `equipment`, `status`)
VALUES
    ('A101', '一教', 100, 96, '投影仪,空调', 'ACTIVE');
