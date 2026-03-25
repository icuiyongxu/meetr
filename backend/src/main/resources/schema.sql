CREATE TABLE IF NOT EXISTS building (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    campus VARCHAR(100) NULL,
    address VARCHAR(255) NULL,
    sort_no INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS meeting_room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    building_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    floor VARCHAR(50) NULL,
    capacity INT NOT NULL DEFAULT 0,
    equipment JSON NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    remark VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_building_room UNIQUE (building_id, name),
    CONSTRAINT fk_room_building FOREIGN KEY (building_id) REFERENCES building(id)
);

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
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_room_config_room (room_id),
    CONSTRAINT fk_room_config_room FOREIGN KEY (room_id) REFERENCES meeting_room(id) ON DELETE SET NULL
);

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
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_booking_room FOREIGN KEY (room_id) REFERENCES meeting_room(id),
    INDEX idx_room_time (room_id, start_time_ms, end_time_ms),
    INDEX idx_booker_time (booker_id, start_time_ms)
);

CREATE TABLE IF NOT EXISTS booking_attendee (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    user_name VARCHAR(100) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_booking_user UNIQUE (booking_id, user_id),
    CONSTRAINT fk_attendee_booking FOREIGN KEY (booking_id) REFERENCES booking(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS booking_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    operator_id VARCHAR(64) NULL,
    operator_name VARCHAR(100) NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_log_booking FOREIGN KEY (booking_id) REFERENCES booking(id) ON DELETE CASCADE
);
