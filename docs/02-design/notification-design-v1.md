# 通知系统设计文档 v1

## 1. 需求背景

预约系统需要在关键事件发生时主动通知相关人员，包括：
- 预约创建 / 修改 / 取消
- 预约审批通过 / 拒绝
- 预约开始前提醒

通知通道分两条：
- **站内通知**：实时推送 + 历史消息列表
- **邮件通知**：SMTP 发送，需用户授权

## 2. 系统架构

```
后端事件 → NotificationService
              ├── 存储 Notification 记录（3个月TTL）
              ├── WebSocket → 前端实时弹窗 / 未读数
              ├── 前端轮询兜底（每30s拉一次）
              └── JavaMailSender → 邮件（用户开启时）
```

## 3. 消息事件类型

| 事件 | 触发时机 | 通知对象 |
|------|---------|---------|
| BOOKING_CREATED | 预约创建成功 | 预约人 + 参会人 + 会议室管理员 |
| BOOKING_UPDATED | 预约信息变更 | 预约人 + 参会人 + 会议室管理员 |
| BOOKING_CANCELED | 预约取消 | 预约人 + 参会人 + 会议室管理员 |
| BOOKING_APPROVED | 审批通过 | 预约人 |
| BOOKING_REJECTED | 审批拒绝 | 预约人 |
| BOOKING_REMINDER | 会议开始前 N 分钟 | 预约人 + 参会人 |

## 4. 数据库设计

### 表：notification

```sql
CREATE TABLE notification (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id         VARCHAR(64)  NOT NULL COMMENT '通知目标用户ID',
  event_type      VARCHAR(32)   NOT NULL COMMENT '事件类型：BOOKING_CREATED等',
  title           VARCHAR(256) NOT NULL COMMENT '通知标题',
  content         TEXT         NOT NULL COMMENT '通知正文',
  booking_id      BIGINT       NULL COMMENT '关联预约ID',
  room_id         BIGINT       NULL COMMENT '关联会议室ID',
  is_read         TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否已读：0未读，1已读',
  read_at         BIGINT       NULL COMMENT '已读时间戳（ms）',
  created_at      BIGINT       NOT NULL COMMENT '创建时间戳（ms）',
  INDEX idx_user_created (user_id, created_at DESC),
  INDEX idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

> 注意：所有时间字段均使用 BIGINT 毫秒时间戳，不存储字符串。

## 5. 站内通知

### 5.1 实时推送（WebSocket + STOMP）

- 协议：STOMP over WebSocket
- 端点：`/ws`
- 订阅主题：`/user/queue/notifications`（每个用户独立队列）
- 认证：JWT Token 在握手时通过 header 传递
- 重连：前端 ws 断开后自动重连，轮询兜底立即启用

### 5.2 轮询兜底

- 前端每 30s 主动调用 `GET /api/notifications/unread-count` 和 `GET /api/notifications`
- 用于 WebSocket 断开时的保底

### 5.3 前端组件

- 顶栏右侧 🔔 铃铛图标 + 未读数小红点
- 点击展开通知列表面板（最近 20 条）
- 支持标记单条已读 / 全部已读
- 通知项点击跳转至预约详情页

## 6. 邮件通知

### 6.1 用户邮件配置

用户表新增字段：
```sql
ALTER TABLE user ADD COLUMN email       VARCHAR(256) NULL COMMENT '邮箱地址';
ALTER TABLE user ADD COLUMN email_enabled TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '是否开启邮件通知';
```

### 6.2 邮件内容模板

- **预约创建**：`[Meetr] 您的预约已创建：{会议室} {时间}`
- **预约取消**：`[Meetr] 预约已取消：{会议室} {时间}`
- **预约变更**：`[Meetr] 预约已变更：{会议室} {时间}`
- **会议提醒**：`[Meetr] 会议即将开始：{会议室} {时间}`

邮件内容包含：
- 预约主题 / 会议室 / 时间
- 参会人列表
- 操作链接（点击跳转详情页）

## 7. 定时清理

- 使用 Spring `@Scheduled` 定时任务
- 每天凌晨 3:00 执行一次
- SQL：`DELETE FROM notification WHERE created_at < (NOW_MS - 90 天毫秒数)`
- 不使用软删除，物理删除

## 8. SMTP 配置

在 `application.yml` 中配置（支持后续后台可配置化）：

```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.example.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

## 9. 接口设计

### 9.1 获取未读数
```
GET /api/notifications/unread-count
Response: { "count": 5 }
```

### 9.2 分页获取通知列表
```
GET /api/notifications?page=0&size=20
Response: { "content": [...], "totalElements": 50 }
```

### 9.3 标记单条已读
```
PUT /api/notifications/{id}/read
```

### 9.4 标记全部已读
```
PUT /api/notifications/read-all
```

### 9.5 删除过期通知（后台定时任务，无需接口）

## 10. 通知事件发送流程

```
1. BookingApplicationService.create/update/cancel 成功后
2. 抛出领域事件 BookingEvent（BOOKING_CREATED 等）
3. NotificationEventListener 监听并处理：
   a. 收集所有需要通知的用户列表
   b. 写入 notification 表
   c. 通过 WebSocket 推送到对应用户
   d. 如果用户开启了邮件通知，调用 EmailService.send()
```

## 11. 待扩展点（暂不纳入一期）

- 通知模板管理（后台配置通知内容模板）
- 通知渠道开关精细化（创建/取消/提醒各自独立开关）
- 邮件富文本 / HTML 模板
- 微信 / Slack 等其他通知通道
