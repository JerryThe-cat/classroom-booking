-- 本地开发/演示教室数据；管理员和摄像头设备由应用启动时的环境变量创建。
SET NAMES utf8mb4;

INSERT INTO `classroom`
    (`room_no`, `building`, `capacity`, `seat_count`, `equipment`, `status`)
VALUES
    ('A101', '一教', 100, 96, '投影仪,空调,音响', 'ACTIVE'),
    ('A102', '一教', 80, 76, '投影仪,空调', 'ACTIVE'),
    ('B201', '二教', 60, 60, '多媒体讲台,空调', 'ACTIVE'),
    ('B202', '二教', 48, 48, '投影仪,白板', 'ACTIVE'),
    ('C305', '三教', 150, 144, '投影仪,空调,音响', 'ACTIVE'),
    ('D401', '四教', 120, 112, '智慧黑板,空调', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `building` = VALUES(`building`),
    `capacity` = VALUES(`capacity`),
    `seat_count` = VALUES(`seat_count`),
    `equipment` = VALUES(`equipment`);
