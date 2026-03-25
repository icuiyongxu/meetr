# Meetr 领域模型设计（V1）

> 基于 MRBS 业务模型，DDD 风格，Spring Boot + Vue3 可直接落地
> 参考：MRBS 源码分析报告（与本文档并行）

---

## 一、战略建模：限界上下文（Bounded Contexts）

```
┌─────────────────────────────────────────────────────────┐
│                    MeetingRoom Context                    │
│  (会议室主数据管理：楼栋、会议室、会议室配置)              │
│                                                          │
│  Building · MeetingRoom · RoomConfig                      │
│  Repository: BuildingRepository, MeetingRoomRepository     │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                     Booking Context                       │
│  (预约核心：预约创建、冲突检测、变更、取消、审批)           │
│                                                          │
│  Booking(AR) · BookingSlot · BookingRule · ConflictCheck  │
│  Repository: BookingRepository                           │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                   UserProfile Context                     │
│  (用户、角色、权限)                                        │
│                                                          │
│  User · UserRole · BookingRole                           │
└─────────────────────────────────────────────────────────┘
```

**一期设计说明**：三个上下文暂时共用同一个 MySQL 数据库，RoomConfig 归属 MeetingRoom Context，但 Booking Context 依赖它做规则校验。

---

## 二、聚合根设计（Aggregates）

### 2.1 MeetingRoom 聚合

```
Building（聚合根）
  └─ MeetingRoom（实体）
        └─ RoomConfig（实体，值对象合集）
```

#### Building（楼栋）
| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| name | String(100) | 楼栋名称 |
| campus | String(100) | 所属园区 |
| address | String(255) | 详细地址 |
| sortNo | Integer | 排序号 |
| status | Enum(ACTIVE, INACTIVE) | 状态 |

#### MeetingRoom（会议室）
| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| buildingId | Long | 所属楼栋 |
| name | String(100) | 会议室名称 |
| floor | String(50) | 楼层 |
| capacity | Integer | 容量 |
| equipment | List<Equipment> | 设备（投影仪/白板/电话...） |
| status | Enum(ENABLED, DISABLED) | 启用/停用 |
| remark | String(500) | 备注 |

**设备用 JSON 存储**：`["projector", "whiteboard", "video_conference", "phone"]`

#### RoomConfig（会议室规则配置）
| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| roomId | Long | 所属会议室（null=继承全局） |
| resolution | Integer | 时间粒度（秒），默认 1800（30分钟） |
| defaultDuration | Integer | 默认时长（分钟） |
| morningStarts | String | 开放开始时间 "08:00" |
| eveningEnds | String | 开放结束时间 "22:00" |
| minBookAheadMinutes | Integer | 最少提前预约分钟数（防滥用） |
| maxBookAheadDays | Integer | 最多提前几天预约 |
| minBookDays | Integer | 至少提前多少分钟 |
| maxDurationMinutes | Integer | 单次最长分钟数 |
| maxPerDay | Integer | 同一天最多预约次数 |
| maxPerWeek | Integer | 同一周最多预约次数 |
| approvalRequired | Boolean | 是否需要审批 |
| status | Enum(ENABLED, DISABLED) | 状态 |

> **设计原则**：RoomConfig 独立成实体，允许全局配置（roomId=null），会议室继承全局配置并可覆盖。

---

### 2.2 Booking 聚合（核心）

```
Booking（聚合根）
  ├─ BookingId（值对象）
  ├─ BookingStatus（值对象/状态机）
  ├─ TimeSlot（值对象）
  └─ BookingAttendee（实体）
```

#### Booking（预约 - 聚合根）
| 属性 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| roomId | Long | 会议室 ID |
| subject | String(200) | 会议主题 |
| bookerId | String(64) | 预约人用户 ID |
| bookerName | String(100) | 预约人姓名 |
| startTime | LocalDateTime | 开始时间 |
| endTime | LocalDateTime | 结束时间 |
| attendeeCount | Integer | 参会人数 |
| status | BookingStatus | **预约状态** |
| approvalStatus | ApprovalStatus | **审批状态** |
| remark | String(500) | 备注 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |
| version | Long | 乐观锁版本号 |

#### BookingStatus（枚举）
```java
public enum BookingStatus {
    BOOKED,      // 已预约（正常状态）
    CANCELED,    // 已取消
    FINISHED     // 已结束（历史状态）
}
```

#### ApprovalStatus（枚举）
```java
public enum ApprovalStatus {
    NONE,        // 不需要审批
    PENDING,     // 待审批
    APPROVED,    // 已批准
    REJECTED     // 已拒绝
}
```

#### TimeSlot（值对象 - 不可变）
```java
public record TimeSlot(LocalDateTime start, LocalDateTime end) {
    // 校验：start < end
    // 校验：不在过去
    // 方法：toSeconds(), overlaps(TimeSlot other), durationMinutes()
}
```

---

## 三、状态机：Booking.approvalStatus

```
                    ┌─────────────┐
                    │   NONE      │ (不需要审批)
                    └──────┬──────┘
                           │
         ┌─────────────────┼─────────────────┐
         ▼                 ▼                 ▼
  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
  │   NONE      │   │   PENDING   │   │   REJECTED  │
  │ (已确认)    │   │  (待审批)   │   │  (已拒绝)   │
  └──────┬──────┘   └──────┬──────┘   └──────┬──────┘
         │                 │                 │
         │          ┌──────┴──────┐          │
         │          ▼             ▼          │
         │    ┌──────────┐  ┌──────────┐     │
         │    │ APPROVED │  │ REJECTED │     │
         │    └──────────┘  └──────────┘     │
         │          │             │          │
         └──────────┴─────────────┴──────────┘
                           │
                           ▼
                    ┌─────────────┐
                    │  CANCELED   │
                    └─────────────┘
```

**一期说明**：一期默认 `approvalRequired=false`，走 NONE 直接确认路径。审批流程二期再做。

---

## 四、核心领域服务：ConflictService（冲突检测）

### 4.1 冲突检测算法

```java
/**
 * 判断 [newStart, newEnd) 与现有预约是否时间重叠
 *
 * 重叠条件（参考 MRBS mrbsCheckFree）：
 *   existing.startTime < newEnd  &&  existing.endTime > newStart
 *
 * 排除条件：
 *   - 排除自身（编辑时）
 *   - 排除已取消的预约（status = CANCELED）
 *   - 排除审批被拒绝的预约（approvalStatus = REJECTED）
 */
public class ConflictCheckService {

    public record ConflictResult(
        boolean conflict,
        List<Booking> conflictingBookings  // 冲突预约列表
    ) {}

    public ConflictResult hasConflict(
        Long roomId,
        TimeSlot newSlot,
        Long excludeBookingId
    ) {
        // SQL: SELECT * FROM booking
        //      WHERE room_id = :roomId
        //        AND status != 'CANCELED'
        //        AND approval_status != 'REJECTED'
        //        AND start_time < :newEnd
        //        AND end_time > :newStart
        //        AND id != :excludeBookingId
    }
}
```

### 4.2 预约时间对齐（Slot 对齐）

会议室有 `resolution`（时间粒度），例如 30 分钟。
预约开始/结束时间需要**对齐到 slot 边界**：

```
resolution = 30分钟
用户选择: 14:20 - 15:50
对齐后:   14:30 - 16:00
```

```java
public TimeSlot alignToSlot(TimeSlot raw) {
    long resolutionSecs = roomConfig.getResolution();
    long startSec = raw.start().toEpochSecond(ZoneOffset.UTC);
    long endSec   = raw.end().toEpochSecond(ZoneOffset.UTC);

    long alignedStart = (startSec / resolutionSecs) * resolutionSecs;
    long alignedEnd   = ((endSec + resolutionSecs - 1) / resolutionSecs) * resolutionSecs;

    return new TimeSlot(
        LocalDateTime.ofEpochSecond(alignedStart, 0, ZoneOffset.UTC),
        LocalDateTime.ofEpochSecond(alignedEnd,   0, ZoneOffset.UTC)
    );
}
```

---

## 五、Booking 聚合的业务规则校验

```java
public class BookingRuleService {

    /**
     * 校验预约是否符合会议室规则
     * 返回违规列表（空=合规）
     */
    public List<RuleViolation> validate(Booking booking, MeetingRoom room) {
        List<RuleViolation> violations = new ArrayList<>();
        RoomConfig config = room.getConfig(); // 继承全局或自身配置

        // R1: 预约时间不能在过去
        if (booking.timeSlot().start().isBefore(LocalDateTime.now())) {
            violations.add(new RuleViolation("R1", "不能预约过去的时间"));
        }

        // R2: 开始 < 结束
        if (!booking.timeSlot().start().isBefore(booking.timeSlot().end())) {
            violations.add(new RuleViolation("R2", "开始时间必须早于结束时间"));
        }

        // R3: 不在开放时间内
        LocalTime startTime = booking.timeSlot().start().toLocalTime();
        LocalTime endTime   = booking.timeSlot().end().toLocalTime();
        if (startTime.isBefore(config.getMorningStartsAsTime()) ||
            endTime.isAfter(config.getEveningEndsAsTime())) {
            violations.add(new RuleViolation("R3", "预约时间不在开放时间段内"));
        }

        // R4: 时长不能超过最大限制
        long durationMinutes = booking.timeSlot().durationMinutes();
        if (durationMinutes > config.getMaxDurationMinutes()) {
            violations.add(new RuleViolation("R4",
                "单次预约时长不能超过" + config.getMaxDurationMinutes() + "分钟"));
        }

        // R5: 不能短于最短时长
        if (durationMinutes < config.getMinDurationMinutes()) {
            violations.add(new RuleViolation("R5",
                "单次预约时长不能少于" + config.getMinDurationMinutes() + "分钟"));
        }

        // R6: 不能早于最小提前量
        long minutesAhead = ChronoUnit.MINUTES.between(
            LocalDateTime.now(), booking.timeSlot().start());
        if (minutesAhead < config.getMinBookAheadMinutes()) {
            violations.add(new RuleViolation("R6",
                "至少需要提前" + config.getMinBookAheadMinutes() + "分钟预约"));
        }

        // R7: 不能超过最大提前天数
        long daysAhead = ChronoUnit.DAYS.between(LocalDate.now(), booking.timeSlot().start().toLocalDate());
        if (daysAhead > config.getMaxBookAheadDays()) {
            violations.add(new RuleViolation("R7",
                "最多只能提前" + config.getMaxBookAheadDays() + "天预约"));
        }

        // R8: 同一天次数限制
        long dayBookings = bookingRepository.countByBookerAndDate(
            booking.bookerId(), booking.timeSlot().start().toLocalDate());
        if (dayBookings >= config.getMaxPerDay()) {
            violations.add(new RuleViolation("R8",
                "同一天最多预约" + config.getMaxPerDay() + "次"));
        }

        return violations;
    }
}
```

---

## 六、Booking 聚合的应用服务（Application Service）

```java
@Service
@Transactional
public class BookingApplicationService {

    // ========== 创建预约 ==========
    public BookingResult create(CreateBookingCommand cmd) {
        // 1. 获取会议室
        MeetingRoom room = meetingRoomRepository.findById(cmd.roomId())
            .orElseThrow(() -> new BusinessException("会议室不存在"));

        // 2. 对齐时间 slot
        TimeSlot alignedSlot = conflictCheckService.alignToSlot(cmd.timeSlot(), room);

        // 3. 构建预约
        Booking booking = Booking.create(
            roomId: cmd.roomId(),
            subject: cmd.subject(),
            bookerId: cmd.bookerId(),
            bookerName: cmd.bookerName(),
            timeSlot: alignedSlot,
            attendeeCount: cmd.attendeeCount(),
            remark: cmd.remark()
        );

        // 4. 规则校验
        List<RuleViolation> violations = ruleService.validate(booking, room);
        if (!violations.isEmpty()) {
            return BookingResult.rejected(violations);
        }

        // 5. 冲突检测（最核心！）
        ConflictResult conflict = conflictCheckService.hasConflict(
            cmd.roomId(), alignedSlot, null);
        if (conflict.conflict()) {
            return BookingResult.conflict(conflict.conflictingBookings());
        }

        // 6. 保存
        bookingRepository.save(booking);
        bookingEventPublisher.publish(BookingCreatedEvent.of(booking));

        return BookingResult.success(booking);
    }

    // ========== 修改预约 ==========
    public BookingResult update(UpdateBookingCommand cmd) {
        Booking booking = bookingRepository.findById(cmd.bookingId())
            .orElseThrow(() -> new BusinessException("预约不存在"));

        // 权限校验：只有创建人或管理员可修改
        if (!booking.canBeModifiedBy(cmd.operatorId(), isAdmin)) {
            throw new AccessDeniedException("无权修改此预约");
        }

        // 时间变更时重新检测
        TimeSlot newSlot = conflictCheckService.alignToSlot(cmd.newTimeSlot(), room);
        ConflictResult conflict = conflictCheckService.hasConflict(
            booking.roomId(), newSlot, booking.id());
        if (conflict.conflict()) {
            return BookingResult.conflict(conflict.conflictingBookings());
        }

        booking.update(newSlot, cmd.subject(), cmd.attendeeCount(), cmd.remark());
        bookingRepository.save(booking);

        return BookingResult.success(booking);
    }

    // ========== 取消预约 ==========
    public void cancel(Long bookingId, String operatorId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new BusinessException("预约不存在"));

        if (!booking.canBeModifiedBy(operatorId, isAdmin)) {
            throw new AccessDeniedException("无权取消此预约");
        }

        booking.cancel();
        bookingRepository.save(booking);
        bookingEventPublisher.publish(BookingCanceledEvent.of(booking));
    }
}
```

---

## 七、仓储接口设计（Repository）

```java
// BookingRepository
public interface BookingRepository {
    Optional<Booking> findById(Long id);

    Page<Booking> findByBookerId(String bookerId, Pageable pageable);

    Page<Booking> findByBookerIdAndDateRange(
        String bookerId, LocalDate start, LocalDate end, Pageable pageable);

    /** 冲突检测查询 */
    List<Booking> findConflicting(
        Long roomId,
        LocalDateTime newStart,
        LocalDateTime newEnd,
        Long excludeBookingId
    );

    /** 某会议室某日所有有效预约（用于日历视图） */
    List<Booking> findByRoomIdAndDate(Long roomId, LocalDate date);

    /** 统计某人某天预约次数 */
    long countByBookerAndDate(String bookerId, LocalDate date);

    void save(Booking booking);
}
```

**冲突检测 SQL（MyBatis-Plus）**：
```sql
SELECT * FROM booking
WHERE room_id = #{roomId}
  AND status != 'CANCELED'
  AND approval_status != 'REJECTED'
  AND start_time < #{newEnd}
  AND end_time > #{newStart}
  AND (@excludeBookingId IS NULL OR id != @excludeBookingId)
ORDER BY start_time
```

---

## 八、Domain Events（领域事件）

```java
// 事件发布
public sealed interface BookingDomainEvent permits BookingCreatedEvent,
                                                    BookingUpdatedEvent,
                                                    BookingCanceledEvent {}

public record BookingCreatedEvent(Long bookingId, Long roomId,
    LocalDateTime startTime, LocalDateTime endTime,
    String bookerId, String bookerName) implements BookingDomainEvent {}

public record BookingCanceledEvent(Long bookingId, Long roomId,
    LocalDateTime startTime, LocalDateTime endTime,
    String bookerId, String operatorId) implements BookingDomainEvent {}
```

**用途**：邮件/企微通知、日志记录、审计追踪。

---

## 九、MEETR 领域模型完整类图

```
┌─────────────────────────────────────────────────────────┐
│                      Building AR                         │
│  - id, name, campus, address, sortNo, status              │
└───────────────────────┬─────────────────────────────────┘
                        │ 1:N
                        ▼
┌─────────────────────────────────────────────────────────┐
│                    MeetingRoom AR                        │
│  - id, buildingId, name, floor, capacity                 │
│  - equipment: List<Equipment>, status, remark            │
│                                                          │
│  RoomConfig (内嵌实体，不单独建聚合):                       │
│  - resolution, defaultDuration                          │
│  - morningStarts, eveningEnds                            │
│  - minBookAhead, maxBookAhead                            │
│  - maxDuration, minDuration                             │
│  - maxPerDay, maxPerWeek                                 │
│  - approvalRequired                                      │
└─────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────┐
│                      Booking AR ⭐核心                   │
│                                                          │
│  [聚合根]  Booking                                        │
│    - id: Long                                           │
│    - roomId: Long                                       │
│    - subject: String                                    │
│    - bookerId: String                                   │
│    - bookerName: String                                 │
│    - startTime: LocalDateTime                           │
│    - endTime: LocalDateTime                             │
│    - attendeeCount: Integer                             │
│    - status: BookingStatus (BOOKED/CANCELED/FINISHED)   │
│    - approvalStatus: ApprovalStatus                     │
│    - remark: String                                     │
│    - version: Long  (乐观锁)                            │
│                                                          │
│  [实体]     BookingAttendee                              │
│    - id, bookingId, userId, userName                     │
│                                                          │
│  [值对象]   TimeSlot(start, end)                         │
│    + overlaps(other): boolean                           │
│    + durationMinutes(): long                             │
│                                                          │
│  聚合根行为:                                             │
│    + create(): Booking                                  │
│    + update(slot, subject, ...): void                   │
│    + cancel(): void                                     │
│    + canBeModifiedBy(userId, isAdmin): boolean           │
└─────────────────────────────────────────────────────────┘
```

---

## 十、数据库设计（最终版，对应 MRBS 分析）

```sql
-- 楼栋
CREATE TABLE building (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL COMMENT '楼栋名称',
    campus       VARCHAR(100) COMMENT '所属园区',
    address      VARCHAR(255) COMMENT '地址',
    sort_no      INT DEFAULT 0,
    status       VARCHAR(20) DEFAULT 'ACTIVE',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 会议室
CREATE TABLE meeting_room (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    building_id  BIGINT NOT NULL,
    name         VARCHAR(100) NOT NULL,
    floor        VARCHAR(50),
    capacity     INT DEFAULT 0,
    equipment    JSON COMMENT '["projector","whiteboard"]',
    status       VARCHAR(20) DEFAULT 'ENABLED',
    remark       VARCHAR(500),
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (building_id) REFERENCES building(id),
    UNIQUE KEY uk_building_room (building_id, name)
);

-- 会议室规则配置（支持全局+会议室级）
CREATE TABLE room_config (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id               BIGINT DEFAULT NULL COMMENT 'NULL=全局配置',
    resolution            INT DEFAULT 1800 COMMENT '时间粒度(秒),默认30分钟',
    default_duration      INT DEFAULT 60 COMMENT '默认时长(分钟)',
    morning_starts        VARCHAR(10) DEFAULT '08:00',
    evening_ends          VARCHAR(10) DEFAULT '22:00',
    min_book_ahead_minutes INT DEFAULT 0 COMMENT '最少提前分钟',
    max_book_ahead_days   INT DEFAULT 30 COMMENT '最多提前天数',
    min_duration_minutes  INT DEFAULT 15,
    max_duration_minutes  INT DEFAULT 480 COMMENT '8小时',
    max_per_day           INT DEFAULT 3,
    max_per_week          INT DEFAULT 10,
    approval_required     TINYINT DEFAULT 0,
    status                VARCHAR(20) DEFAULT 'ENABLED',
    created_at            DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES meeting_room(id),
    UNIQUE KEY uk_room_config_room (room_id)  -- 全局只有一条(room_id=NULL)
);

-- 预约
CREATE TABLE booking (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id        BIGINT NOT NULL,
    subject        VARCHAR(200) NOT NULL,
    booker_id      VARCHAR(64) NOT NULL COMMENT '用户ID',
    booker_name    VARCHAR(100),
    start_time     DATETIME NOT NULL COMMENT '开始时间',
    end_time       DATETIME NOT NULL COMMENT '结束时间',
    attendee_count INT DEFAULT 1,
    status         VARCHAR(20) DEFAULT 'BOOKED' COMMENT 'BOOKED/CANCELED/FINISHED',
    approval_status VARCHAR(20) DEFAULT 'NONE' COMMENT 'NONE/PENDING/APPROVED/REJECTED',
    remark         VARCHAR(500),
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version        BIGINT DEFAULT 0 COMMENT '乐观锁',
    FOREIGN KEY (room_id) REFERENCES meeting_room(id),

    -- 冲突检测核心索引
    INDEX idx_room_time (room_id, start_time, end_time),
    -- 我的预约查询
    INDEX idx_booker_time (booker_id, start_time),
    -- 今日会议
    INDEX idx_room_date (room_id, start_time, status)
);

-- 预约参会人
CREATE TABLE booking_attendee (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id  BIGINT NOT NULL,
    user_id     VARCHAR(64),
    user_name   VARCHAR(100),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES booking(id) ON DELETE CASCADE,
    UNIQUE KEY uk_booking_user (booking_id, user_id)
);

-- 操作日志（参考 MRBS info_user / info_time / info_text）
CREATE TABLE booking_operation_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id      BIGINT NOT NULL,
    operation_type  VARCHAR(50) COMMENT 'CREATE/UPDATE/CANCEL/APPROVE',
    operator_id     VARCHAR(64),
    operator_name  VARCHAR(100),
    content         VARCHAR(1000),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES booking(id) ON DELETE CASCADE
);
```

---

## 十一、API 设计与现有 doc 对齐

### 核心接口不变，对返回值做增强

```json
// POST /api/bookings/check-conflict
// 请求
{ "roomId": 1, "startTime": "2026-03-26T14:00", "endTime": "2026-03-26T15:00" }

// 响应（有冲突）
{
  "code": 0,
  "message": "ok",
  "data": {
    "conflict": true,
    "conflictingBookings": [
      {
        "id": 15,
        "subject": "产品评审会",
        "startTime": "2026-03-26T13:30",
        "endTime": "2026-03-26T14:30",
        "bookerName": "张三"
      }
    ]
  }
}

// 响应（无冲突）
{
  "code": 0,
  "data": { "conflict": false, "conflictingBookings": [] }
}
```

---

## 十二、与 MRBS 的关键设计差异（避免踩坑）

| MRBS | Meetr 改进 |
|------|-----------|
| 重复预约拆成 `repeat`+`entry` 两表 | **一期不做重复**，只保留单次预约 |
| status 用位图（不便扩展） | 拆成 `status` + `approvalStatus` 两个独立字段，清晰可扩展 |
| Unix 时间戳存储 | **用 LocalDateTime**，Java 原生支持，无需手动转换 |
| Area 多层嵌套 | **一期简化**：扁平 Building → MeetingRoom 两级 |
| 多 room 批量预约 | **一期不做**，一个预约对应一个会议室 |
| PHP 无事务保障 | **Spring @Transactional 包裹所有聚合操作** |
| 权限靠 `level` 整数 | **RBAC：USER / ADMIN**，精细到会议室级 |
| 无乐观锁 | **JPA @Version / MyBatis-Plus version 字段** |

---

## 十三、一期待完成的工作

### 必须完成（MVP）
- [ ] 实体类：Building, MeetingRoom, RoomConfig, Booking, BookingAttendee, BookingOperationLog
- [ ] ConflictCheckService（含对齐 slot）
- [ ] BookingRuleService（含所有规则校验）
- [ ] BookingApplicationService（含创建/修改/取消）
- [ ] BookingRepository（含冲突检测 SQL）
- [ ] CRUD API（楼栋、会议室、预约）
- [ ] 冲突校验 API
- [ ] 我的预约查询

### 一期不做
- [ ] 重复预约
- [ ] 审批工作流
- [ ] 多会议室批量预约
- [ ] 全局 RoomConfig 初始化
- [ ] 通知（邮件/企微）
- [ ] 前端页面
