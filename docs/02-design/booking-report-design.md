# 预约报表与使用率统计 — 技术设计文档

## 1. 后端改造

### 1.1 新增 Controller

```
GET /api/admin/reports/room-usage       会议室使用率
GET /api/admin/reports/booking-records  预约记录查询
GET /api/admin/reports/user-usage       用户维度统计
```

所有接口需要 `booking:view` 权限（管理员）。

### 1.2 会议室使用率接口

```
GET /api/admin/reports/room-usage
参数：
  - startDateMs   (必填)  开始时间戳
  - endDateMs     (必填)  结束时间戳
  - buildingId     (可选)  楼栋过滤
返回：
[
  {
    roomId, roomName, buildingId, buildingName,
    totalBookings,    // 有效预约次数
    totalMinutes,     // 总使用时长（分钟）
    usagePercent,     // 使用率 %
    canceledCount,    // 取消次数
    pendingCount      // 期初待审批数
  }
]
```

**计算逻辑**：
- 有效预约：`status = 'BOOKED' AND (approval_status = 'APPROVED' OR 'NONE')`
- 使用时长：`SUM(end_time_ms - start_time_ms) / 60000`（分钟）
- 可用总时长：时间段内会议室每天开放时长的总和（假设每天 08:00–22:00 = 14h）
- 使用率 = 使用时长 / 可用总时长

### 1.3 预约记录查询接口

```
GET /api/admin/reports/booking-records
参数：
  - buildingIds      (可选)  逗号分隔楼栋ID
  - roomIds          (可选)  逗号分隔会议室ID
  - bookerId        (可选)  预约人
  - keyword          (可选)  主题关键字
  - status          (可选)  BOOKED / CANCELED
  - approvalStatus   (可选)  APPROVED / REJECTED / PENDING / NONE
  - startFromMs      (可选)  开始时间起
  - startToMs        (可选)  开始时间止
  - page, size       分页

返回：PageResult<BookingDTO>
```

**注意**：
- 如果不传时间范围，默认查最近 30 天
- 导出模式下 `size = -1` 返回全部（上限 10000）

### 1.4 用户维度统计接口

```
GET /api/admin/reports/user-usage
参数：
  - startDateMs, endDateMs
返回：
[
  {
    bookerId, bookerName,
    totalBookings,
    validBookings,      // 有效次数
    canceledCount,
    rejectedCount,
    totalMinutes
  }
]
```

---

## 2. 前端改造

### 2.1 新增页面

```
src/views/admin/
  ReportRoomUsage.vue      会议室使用率
  ReportBookingRecords.vue 预约记录
  ReportUserUsage.vue      用户统计
```

### 2.2 路由

```
/admin/report/room-usage
/admin/report/records
/admin/report/user-usage
```

### 2.3 菜单入口

在管理菜单下新增「数据报表」子菜单：

```
管理
└── 数据报表
    ├── 使用率统计
    ├── 预约记录
    └── 用户统计
```

---

## 3. 导出 Excel

- 使用 `xlsx` 库（前端）或后端生成
- 推荐后端直接生成 Excel 文件流返回
- 建议新增接口：
  ```
  GET /api/admin/reports/booking-records/export
  参数同查询接口
  返回：application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
  ```

---

## 4. 数据模型

### 4.1 RoomUsageVO
```java
public record RoomUsageVO(
    Long roomId,
    String roomName,
    Long buildingId,
    String buildingName,
    long totalBookings,
    long totalMinutes,
    double usagePercent,
    long canceledCount,
    long pendingCount
) {}
```

### 4.2 UserUsageVO
```java
public record UserUsageVO(
    String bookerId,
    String bookerName,
    long totalBookings,
    long validBookings,
    long canceledCount,
    long rejectedCount,
    long totalMinutes
) {}
```

---

## 5. 风险与边界

1. 导出 Excel 大量数据时注意内存和响应超时，建议加 `size` 上限
2. 使用率计算中“可用总时长”按固定 14h/天估算，后续可从规则配置读取实际开放时间
3. 用户维度统计中如果 bookerId 不规范（空值），需要做兜底处理
