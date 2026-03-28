-- =============================================
-- Meetr 系统表结构
-- =============================================

-- -------------------------------------------
-- 1. sys_role（角色）
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL
);

-- -------------------------------------------
-- 2. sys_permission（权限）
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL
);

-- -------------------------------------------
-- 3. sys_role_permission（角色-权限关联）
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    CONSTRAINT uk_role_perm UNIQUE (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_perm FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE CASCADE
);

-- -------------------------------------------
-- 4. sys_user（用户）
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NULL,
    password VARCHAR(255) NULL COMMENT 'BCrypt 加密后的密码',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at_ms BIGINT NOT NULL, updated_at_ms BIGINT NOT NULL
);

-- -------------------------------------------
-- 5. sys_user_role（用户-角色关联）
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
);

-- -------------------------------------------
-- 6. sys_equipment（设备）
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS sys_equipment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

-- -------------------------------------------
-- 7. building（楼栋）
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS building (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    campus VARCHAR(100) NULL,
    address VARCHAR(255) NULL,
    sort_no INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at_ms BIGINT NOT NULL,
    updated_at_ms BIGINT NOT NULL
);

-- -------------------------------------------
-- 8. meeting_room（会议室）
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS meeting_room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    building_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    floor VARCHAR(50) NULL,
    capacity INT NOT NULL DEFAULT 0,
    equipment JSON NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    remark VARCHAR(500) NULL,
    created_at_ms BIGINT NOT NULL,
    updated_at_ms BIGINT NOT NULL,
    CONSTRAINT uk_building_room UNIQUE (building_id, name),
    CONSTRAINT fk_room_building FOREIGN KEY (building_id) REFERENCES building(id)
);

-- -------------------------------------------
-- 9. room_config（预约规则配置）
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS room_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NULL,
    resolution INT NOT NULL DEFAULT 1800,
    default_duration INT NOT NULL DEFAULT 60,
    morning_starts VARCHAR(5) NOT NULL DEFAULT '08:00',
    evening_ends VARCHAR(5) NOT NULL DEFAULT '22:00',
    min_book_ahead_minutes INT NOT NULL DEFAULT 0,
    max_book_ahead_days INT NOT NULL DEFAULT 30,
    min_duration_minutes INT NOT NULL DEFAULT 15,
    max_duration_minutes INT NOT NULL DEFAULT 480,
    max_per_day INT NOT NULL DEFAULT 3,
    max_per_week INT NOT NULL DEFAULT 10,
    approval_required BIT(1) NOT NULL DEFAULT b'0',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    admin_user_ids VARCHAR(500) NULL,
    created_at_ms BIGINT NOT NULL,
    updated_at_ms BIGINT NOT NULL,
    INDEX idx_room_config_room (room_id),
    CONSTRAINT fk_room_config_room FOREIGN KEY (room_id) REFERENCES meeting_room(id) ON DELETE SET NULL
);

-- -------------------------------------------
-- 10. booking（预约）
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS booking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    subject VARCHAR(200) NOT NULL,
    booker_id VARCHAR(64) NOT NULL,
    booker_name VARCHAR(100) NULL,
    start_time_ms BIGINT NOT NULL COMMENT 'UTC milliseconds since epoch',
    end_time_ms BIGINT NOT NULL COMMENT 'UTC milliseconds since epoch',
    attendee_count INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
    approval_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    remark VARCHAR(500) NULL,
    recurrence_type VARCHAR(20) NULL,
    recurrence_end_date DATE NULL,
    parent_id BIGINT NULL,
    series_index INT NULL DEFAULT 1,
    created_at_ms BIGINT NOT NULL,
    updated_at_ms BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_booking_room FOREIGN KEY (room_id) REFERENCES meeting_room(id),
    INDEX idx_room_time (room_id, start_time_ms, end_time_ms),
    INDEX idx_booker_time (booker_id, start_time_ms)
);

-- -------------------------------------------
-- 11. booking_attendee（参会人）
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS booking_attendee (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    user_name VARCHAR(100) NULL,
    created_at_ms BIGINT NOT NULL,
    updated_at_ms BIGINT NOT NULL,
    CONSTRAINT uk_booking_user UNIQUE (booking_id, user_id),
    CONSTRAINT fk_attendee_booking FOREIGN KEY (booking_id) REFERENCES booking(id) ON DELETE CASCADE
);

-- -------------------------------------------
-- 12. booking_operation_log（操作日志）
-- -------------------------------------------
CREATE TABLE IF NOT EXISTS booking_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    operator_id VARCHAR(64) NULL,
    operator_name VARCHAR(100) NULL,
    content VARCHAR(1000) NOT NULL,
    created_at_ms BIGINT NOT NULL,
    updated_at_ms BIGINT NOT NULL,
    CONSTRAINT fk_log_booking FOREIGN KEY (booking_id) REFERENCES booking(id) ON DELETE CASCADE
);

-- =============================================
-- 初始化数据（幂等，IF NOT EXISTS / WHERE NOT EXISTS）
-- =============================================

-- ---- 角色 ----
INSERT IGNORE INTO sys_role (id, code, name, description) VALUES
(1, 'ADMIN', '管理员', '系统管理员，拥有全部权限'),
(2, 'USER',  '普通用户', '普通用户，可预约会议室');

-- ---- 权限 ----
INSERT IGNORE INTO sys_permission (id, code, name, description) VALUES
(1,  'room:view',             '查看会议室', '查看会议室'),
(2,  'room:manage',           '管理会议室', '管理会议室'),
(3,  'building:view',         '查看楼栋', '查看楼栋'),
(4,  'building:manage',       '管理楼栋', '管理楼栋'),
(5,  'booking:view',          '查看预约', '查看预约'),
(6,  'booking:manage',        '管理预约', '管理预约'),
(7,  'booking:approve',       '审批预约', '审批预约'),
(8,  'user:view',             '查看用户', '查看用户'),
(9,  'user:manage',           '管理用户', '管理用户'),
(10, 'role:manage',           '管理角色', '管理角色与权限'),
(11, 'config:view',           '查看配置', '查看规则配置'),
(12, 'config:manage',         '管理配置', '管理规则配置'),
(13, 'notification:view',     '查看通知', '查看通知'),
(14, 'notification:manage',   '管理通知', '标记已读和处理通知');

-- ---- 角色-权限关联 ----
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
-- ADMIN 拥有全部权限
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7),(1,8),(1,9),(1,10),(1,11),(1,12),(1,13),(1,14),
-- USER 拥有基础权限
(2,1),(2,3),(2,5),(2,6),(2,13),(2,14);

-- ---- 用户（admin / admin123，BCrypt 预加密）----
INSERT IGNORE INTO sys_user (id, user_id, name, password, status, created_at_ms, updated_at_ms) VALUES
(1, 'admin', '管理员', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n0SQFh/ULwDwECIeGCZ3S', 'ACTIVE', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000);

-- ---- 用户-角色关联 ----
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- ---- 设备 ----
INSERT IGNORE INTO sys_equipment (id, code, name, status) VALUES
(1, 'projector',     '投影仪',    'ACTIVE'),
(2, 'whiteboard',    '白板',      'ACTIVE'),
(3, 'video',         '视频会议',  'ACTIVE'),
(4, 'phone',         '电话',      'ACTIVE'),
(5, 'tv',           '电视',      'ACTIVE'),
(6, 'air_conditioner','空调',     'ACTIVE'),
(7, 'wifi',         'Wi-Fi',     'ACTIVE');

-- =============================================
-- 通知系统相关表
-- =============================================

-- 13. notification（站内通知）
CREATE TABLE IF NOT EXISTS notification (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         VARCHAR(64)  NOT NULL COMMENT '通知目标用户ID',
    event_type      VARCHAR(32)  NOT NULL COMMENT 'BOOKING_CREATED / CANCELED / UPDATED 等',
    title           VARCHAR(256) NOT NULL COMMENT '通知标题',
    content         TEXT         NOT NULL COMMENT '通知正文',
    booking_id      BIGINT       NULL COMMENT '关联预约ID',
    room_id         BIGINT       NULL COMMENT '关联会议室ID',
    booking_start_time_ms BIGINT   NULL COMMENT '会议开始时间（ms），用于过滤已过期通知',
    booking_end_time_ms   BIGINT   NULL COMMENT '会议结束时间（ms）',
    is_read         TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '0=未读，1=已读',
    read_at         BIGINT       NULL COMMENT '已读时间戳（ms）',
    created_at_ms  BIGINT       NOT NULL COMMENT '创建时间戳（ms）',
    INDEX idx_user_created (user_id, created_at_ms DESC),
    INDEX idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE notification ADD COLUMN IF NOT EXISTS booking_start_time_ms BIGINT NULL COMMENT '会议开始时间（ms）' AFTER room_id;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS booking_end_time_ms BIGINT NULL COMMENT '会议结束时间（ms）' AFTER booking_start_time_ms;

-- 14. notification_event_log（通知发送日志，防止重复发送）
CREATE TABLE IF NOT EXISTS notification_event_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type      VARCHAR(32)  NOT NULL COMMENT '事件类型',
    booking_id      BIGINT       NOT NULL COMMENT '关联预约ID',
    target_user_id  VARCHAR(64)  NOT NULL COMMENT '通知目标用户ID',
    channel         VARCHAR(16)  NOT NULL COMMENT 'IN_APP / EMAIL',
    status          VARCHAR(16)  NOT NULL COMMENT 'SENT / FAILED',
    error_msg       VARCHAR(500) NULL,
    created_at_ms   BIGINT       NOT NULL,
    INDEX idx_booking_event (booking_id, event_type, target_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- sys_user 新增邮箱字段
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS email VARCHAR(256) NULL COMMENT '邮箱地址' AFTER name;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS email_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否开启邮件通知' AFTER email;
