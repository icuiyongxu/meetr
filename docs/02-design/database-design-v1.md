# Meetr 数据库表设计（V1）

## 1. building
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| name | varchar(100) | 楼栋名称 |
| campus | varchar(100) | 园区/地点 |
| address | varchar(255) | 地址 |
| sort_no | int | 排序 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 2. meeting_room
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| building_id | bigint | 所属楼栋 |
| name | varchar(100) | 会议室名称 |
| floor | varchar(50) | 楼层 |
| capacity | int | 容量 |
| equipment_json | json | 设备信息 |
| status | varchar(20) | ENABLED / DISABLED |
| remark | varchar(500) | 备注 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 3. booking
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| room_id | bigint | 会议室ID |
| subject | varchar(200) | 会议主题 |
| booker_id | varchar(64) | 预约人ID |
| booker_name | varchar(100) | 预约人姓名 |
| start_time | datetime | 开始时间 |
| end_time | datetime | 结束时间 |
| attendee_count | int | 参会人数 |
| status | varchar(20) | BOOKED / CANCELED / FINISHED |
| approval_status | varchar(20) | NONE / PENDING / APPROVED / REJECTED |
| remark | varchar(500) | 备注 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 4. booking_attendee
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| booking_id | bigint | 预约ID |
| user_id | varchar(64) | 用户ID |
| user_name | varchar(100) | 用户名 |

## 5. booking_rule
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| room_id | bigint | 会议室ID，可为空表示全局 |
| max_advance_days | int | 最多提前预约天数 |
| min_duration_minutes | int | 最短时长 |
| max_duration_minutes | int | 最长时长 |
| require_approval | tinyint | 是否审批 |
| allow_repeat | tinyint | 是否允许周期预约 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

## 6. booking_operation_log
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| booking_id | bigint | 预约ID |
| operation_type | varchar(50) | CREATE / UPDATE / CANCEL / APPROVE |
| operator_id | varchar(64) | 操作人 |
| operator_name | varchar(100) | 操作人姓名 |
| content | varchar(1000) | 操作内容 |
| created_at | datetime | 创建时间 |

## 索引建议
- booking(room_id, start_time, end_time)
- booking(booker_id, start_time)
- meeting_room(building_id, status)
- booking_attendee(booking_id)
