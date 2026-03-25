# Meetr 后端 API 草案（V1）

## 1. 会议室查询
### GET /api/rooms
- 查询会议室列表
- 支持条件：buildingId、floor、capacity、status、keyword

### GET /api/rooms/available
- 查询指定时间段可用会议室
- 参数：startTime、endTime、buildingId、capacity

### GET /api/rooms/{id}
- 查询会议室详情

## 2. 会议室管理
### POST /api/admin/rooms
- 新增会议室

### PUT /api/admin/rooms/{id}
- 更新会议室

### PUT /api/admin/rooms/{id}/status
- 启用/停用会议室

## 3. 预约相关
### POST /api/bookings
- 创建预约

### PUT /api/bookings/{id}
- 修改预约

### POST /api/bookings/{id}/cancel
- 取消预约

### GET /api/bookings/{id}
- 查询预约详情

### GET /api/bookings/mine
- 查询我的预约

### GET /api/bookings/today
- 查询我的今日会议

## 4. 冲突校验
### POST /api/bookings/check-conflict
- 入参：roomId、startTime、endTime、excludeBookingId
- 返回：是否冲突、冲突预约信息

## 5. 楼栋与基础数据
### GET /api/buildings
- 查询楼栋列表

### POST /api/admin/buildings
- 新增楼栋

### PUT /api/admin/buildings/{id}
- 更新楼栋

## 6. 规则配置
### GET /api/admin/booking-rules
- 查询预约规则

### POST /api/admin/booking-rules
- 保存预约规则

## 返回规范建议
统一返回：
```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```
