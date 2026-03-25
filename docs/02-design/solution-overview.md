# 会议室预定系统方案总览

## 技术栈
### 后端
- Java 21 / Spring Boot 3
- Spring Web
- Spring Validation
- MyBatis-Plus 或 JPA
- MySQL 8
- Redis（可选）

### 前端
- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Vue Router
- Day.js

## 前后端目录规划
- `backend/`：Spring Boot 后端
- `frontend/`：Vue3 + Element Plus 前端
- `docs/`：项目文档

## 核心领域对象
- Building
- MeetingRoom
- Booking
- BookingAttendee
- BookingRule
- BookingOperationLog

## 核心规则
1. 同会议室同时间段不可重叠
2. 不能预约过去时间
3. 开始时间必须早于结束时间
4. 取消预约后释放占用
5. 房间停用后不可新建预约
6. 一期默认：审批中预约也占坑
