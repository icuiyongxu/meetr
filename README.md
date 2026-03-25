# Meetr 会议室预定系统

基于 MRBS 业务模型，Spring Boot 3 + Vue 3 + Element Plus 重构。

## 本地启动

### 1. 准备数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS meetr CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 创建用户并授权（如果还没建过）
mysql -u root -p -e "CREATE USER IF NOT EXISTS 'meetr'@'localhost' IDENTIFIED BY 'meetr123';"
mysql -u root -p -e "GRANT ALL PRIVILEGES ON meetr.* TO 'meetr'@'localhost';"
mysql -u root -p -e "FLUSH PRIVILEGES;"

# 初始化表结构
mysql -u meetr -pmeetr123 meetr < backend/src/main/resources/schema.sql
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端地址：http://localhost:8080

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端地址：http://localhost:5173（会自动代理 /api 到 8080）

## 技术栈

- **后端**：Java 21 / Spring Boot 3.3 / Spring Data JPA / MySQL 8 / Lombok
- **前端**：Vue 3 / TypeScript / Vite / Element Plus / Pinia / Vue Router / Axios / Day.js

## 目录结构

```
meetr/
├── backend/               # Spring Boot 后端
│   └── src/main/java/com/meetr/
│       ├── domain/        # 实体、枚举、值对象、仓储、领域服务
│       ├── application/  # 应用服务、DTO
│       ├── controller/   # REST API 控制器
│       └── exception/    # 统一异常处理
├── frontend/             # Vue3 前端
│   └── src/
│       ├── views/        # 页面组件
│       ├── components/   # 公共组件
│       ├── api/          # Axios API 调用
│       ├── types/        # TypeScript 类型定义
│       ├── stores/       # Pinia 状态管理
│       └── utils/        # 工具函数
└── docs/                 # 设计文档
    └── 02-design/
        ├── domain-model-v1.md   # 领域模型设计
        ├── database-design-v1.md # 数据库设计
        └── api-design-v1.md     # API 设计
```

## API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/rooms` | GET | 会议室列表 |
| `/api/rooms/available` | GET | 可用会议室查询 |
| `/api/rooms/{id}` | GET | 会议室详情 |
| `/api/admin/rooms` | POST | 新增会议室 |
| `/api/admin/rooms/{id}` | PUT | 更新会议室 |
| `/api/admin/rooms/{id}/status` | PUT | 启停会议室 |
| `/api/bookings` | POST | 创建预约 |
| `/api/bookings/{id}` | PUT | 修改预约 |
| `/api/bookings/{id}/cancel` | POST | 取消预约 |
| `/api/bookings/{id}` | GET | 预约详情 |
| `/api/bookings/mine` | GET | 我的预约 |
| `/api/bookings/today` | GET | 今日会议 |
| `/api/bookings/check-conflict` | POST | 冲突校验 |
| `/api/buildings` | GET | 楼栋列表 |
| `/api/admin/booking-rules` | GET/POST | 预约规则配置 |

## 统一响应格式

```json
{ "code": 0, "message": "ok", "data": {} }
```

## 默认账号

目前无登录系统，userId 通过 localStorage 自动生成（格式：`user_xxxxxxxx`）。

## 一期范围

- 会议室主数据管理（楼栋、会议室、规则配置）
- 预约创建 / 修改 / 取消
- 时间冲突校验（R1-R8 规则校验）
- 我的预约 / 今日会议
- 管理端基础配置

## 一期不做

- 重复预约
- 审批工作流
- 邮件 / 企微通知
- 小程序 / App
