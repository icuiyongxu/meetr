# 重复预约（系列预约）管理补全技术设计文档

## 1. 目标范围

本设计文档用于描述“系列预约管理补全”的后端与前端改造方案，涵盖：
- 系列详情接口
- 系列修改/取消的范围语义
- 冲突校验策略
- 日志记录与审批联动

---

## 2. 当前系统现状

### 2.1 已有能力
- 创建重复预约（CreateBookingCommand + recurrenceType + recurrenceEndDate）
- 查询系列：`GET /api/bookings/{id}/series`
- 修改后续预约：`PUT /api/bookings/{id}/future`
- 取消预约：`POST /api/bookings/{id}/cancel`（支持 cancelSeries）
- 日志记录：`booking_operation_log`

### 2.2 现有不足
- 修改时无法选择“本次 / 后续 / 全部”
- 取消只支持“本次 / 全部”，缺少“后续”
- 无冲突校验策略扩展
- 系列详情展示不完整
- 日志缺乏系列操作类型

---

## 3. 数据模型与字段

### 3.1 booking 表
已有字段：
- parent_id
- series_index
- recurrence_type
- recurrence_end_date

建议保持不变。

### 3.2 booking_operation_log
扩展 operationType 枚举约定：
- CREATE_SERIES
- UPDATE_ONCE
- UPDATE_FUTURE
- UPDATE_ALL
- CANCEL_ONCE
- CANCEL_FUTURE
- CANCEL_ALL

无需新增字段，但在 content 里记录清晰范围。

---

## 4. 接口设计

### 4.1 系列详情
```
GET /api/bookings/{id}/series
```
返回：
- master
- instances
- totalCount

已有接口，建议前端直接展示。

---

### 4.2 系列修改
新增统一接口：
```
PUT /api/bookings/{id}/series
```
参数：
```json
{
  "operatorId": "user01",
  "scope": "ONCE|FUTURE|ALL",
  "subject": "xxx",
  "startTime": 1742983200000,
  "endTime": 1742986800000,
  "attendeeCount": 6,
  "remark": "..."
}
```

逻辑：
- scope = ONCE：只修改当前 booking
- scope = FUTURE：修改当前及后续实例
- scope = ALL：修改整个系列

冲突校验：
- FUTURE / ALL 需要对每条实例重新校验
- 默认严格模式：有冲突即整体失败

---

### 4.3 系列取消
新增统一接口：
```
POST /api/bookings/{id}/series-cancel
```
参数：
```json
{
  "operatorId": "user01",
  "scope": "ONCE|FUTURE|ALL"
}
```

逻辑：
- ONCE：仅取消当前预约
- FUTURE：取消当前及后续
- ALL：取消整个系列

---

### 4.4 冲突返回
若遇冲突，返回结构：
```json
{
  "success": false,
  "conflicts": [
    { "id": 1, "subject": "...", "startTime": 123, "endTime": 456 }
  ]
}
```

---

## 5. 后端核心逻辑设计

### 5.1 Series 更新流程
1. 查出 master + all instances
2. 根据 scope 计算需影响的实例
3. 对每条实例执行：
   - 校验规则
   - 校验冲突
4. 如果全部通过，批量更新
5. 写入操作日志
6. 返回更新后的系列详情

### 5.2 冲突校验
使用现有 conflictCheckService
- 为每条实例计算 slot
- 校验是否与其他 booking 冲突（排除自身 id）

### 5.3 审批联动
如果规则开启审批：
- 普通用户修改后 -> affected 实例重新进入 PENDING
- 管理员修改 -> 直接 APPROVED

---

## 6. 前端改造设计

### 6.1 系列详情 UI
- 展示 master + instances
- 标明 seriesIndex
- 显示审批状态

### 6.2 修改交互
- 非系列：走现有修改弹窗
- 系列：弹出 scope 选择

### 6.3 取消交互
- 非系列：走现有 cancel 逻辑
- 系列：弹出 scope 选择（本次 / 后续 / 全部）

---

## 7. 日志与通知

### 7.1 日志
所有系列操作必须写入 booking_operation_log，内容包括：
- scope
- 时间范围变化
- 操作人

### 7.2 通知
建议后续扩展：
- 系列被修改/取消的通知
- 通知对象：预约人 + 参会人

---

## 8. 风险与边界

1. 系列数据量较大时，批量修改需注意性能
2. 严格模式可能导致部分用户感知“修改失败”，需要明确提示冲突原因
3. 审批逻辑重新进入待审批可能带来大量审批任务

---

## 9. 验收指标

- 系列修改可正确选择范围
- 系列取消可正确选择范围
- 冲突校验结果准确
- 日志记录完整

---

## 10. 后续拆分建议

后续可把该需求拆成以下子任务：
1. 系列详情页面优化
2. 系列修改能力（含冲突校验）
3. 系列取消能力（含后续）
4. 系列日志 + 通知
