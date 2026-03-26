# 重复预约设计

## 数据模型

### Booking 新增字段
| 字段 | 类型 | 说明 |
|---|---|---|
| `recurrenceType` | ENUM(NONE, DAILY, WEEKLY, WORKDAY, MONTHLY) | 重复类型 |
| `recurrenceEndDate` | LocalDate | 重复结束日期（不含，只生成该日之前的实例） |
| `parentId` | Long | 指向主预约；主预约自身为 null；取消时以此判断是否系列 |
| `seriesIndex` | Integer | 该实例是系列中第几个（从1开始），主预约=1 |

- 主预约（`parentId=null`）本身也是一条有效预约，占用 `seriesIndex=1`
- 生成实例时，主预约+所有子实例全部持久化，一次性入库
- 每个实例独立做冲突检测，不通过的实例跳过（不生成）

## 重复规则

- **DAILY**：每天相同时间
- **WEEKLY**：每周相同星期几
- **WORKDAY**：工作日（周一到周五），跳过周末
- **MONTHLY**：每月相同天号（如 15号→每月15号）

结束日期默认 +30 天，最大 +180 天。

## 冲突处理

每个实例独立做冲突检测；与系列内其他实例不互斥（一个人可以连续约同一会议室多个时段）。

## 取消行为

- `cancel-series=false`：只取消当前预约
- `cancel-series=true`：取消主预约及所有未取消的子实例（逻辑删除，status=CANCELED）
- 审批中/已完成的实例也一并取消

## API

### 创建（POST /bookings）
请求体新增：
```json
{
  "roomId": 1,
  "subject": "周会",
  "startTime": 1742983200000,
  "endTime": 1742986800000,
  "recurrenceType": "WEEKLY",
  "recurrenceEndDate": "2026-04-25",
  "...": "..."
}
```

### 取消（DELETE /bookings/{id}）
新增参数 `?cancel-series=true`

## 前端

BookingForm 新增"重复"区块：
- 重复类型下拉（默认"不重复"）
- 结束日期选择（recurrenceType 非"不重复"时显示）
- 提交时显示"将生成 N 个预约实例"提示

## 数据库变更

```sql
ALTER TABLE booking ADD COLUMN recurrence_type VARCHAR(20) DEFAULT 'NONE';
ALTER TABLE booking ADD COLUMN recurrence_end_date DATE;
ALTER TABLE booking ADD COLUMN parent_id BIGINT;
ALTER TABLE booking ADD COLUMN series_index INT DEFAULT 0;
-- 外键
ALTER TABLE booking ADD FOREIGN KEY (parent_id) REFERENCES booking(id);
```
