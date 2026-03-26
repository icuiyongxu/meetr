# RBAC 权限系统

## 数据模型

```
sys_user ── sys_user_role ── sys_role
                              │
                        sys_role_permission
                              │
                        sys_permission
```

## 数据库表

| 表 | 字段 | 说明 |
|---|---|---|
| `sys_user` | id, user_id, name, status, created_at_ms | 用户主表 |
| `sys_role` | id, code, name, description | 角色（ADMIN/USER） |
| `sys_permission` | id, code, name, description | 权限定义 |
| `sys_user_role` | id, user_id, role_id | 用户↔角色关联 |
| `sys_role_permission` | id, role_id, permission_id | 角色↔权限关联 |

## 预置权限码

| code | 名称 | 所属模块 |
|---|---|---|
| `room:view` | 查看会议室 | 会议室 |
| `room:manage` | 管理会议室 | 会议室 |
| `building:view` | 查看楼栋 | 楼栋 |
| `building:manage` | 管理楼栋 | 楼栋 |
| `booking:view` | 查看预约 | 预约 |
| `booking:manage` | 管理预约 | 预约 |
| `booking:approve` | 审批预约 | 预约 |
| `user:view` | 查看用户 | 用户 |
| `user:manage` | 管理用户 | 用户 |
| `role:manage` | 管理角色 | 角色 |
| `config:view` | 查看配置 | 配置 |
| `config:manage` | 管理配置 | 配置 |

## 预置角色权限

| 角色 | 拥有权限 |
|---|---|
| ADMIN | 全部 12 个权限 |
| USER | room:view, building:view, booking:view, booking:manage |

## API 权限矩阵

| 方法 | 路径 | 所需权限 |
|---|---|---|
| POST | /api/auth/login | 公开 |
| GET | /api/users | 公开（用于参会人选择） |
| GET | /api/users/{userId} | 公开 |
| GET | /api/admin/users | user:view |
| POST | /api/admin/users | user:manage |
| PUT | /api/admin/users/{id}/roles | user:manage |
| GET | /api/admin/roles | role:manage |
| POST | /api/admin/roles | role:manage |
| GET | /api/admin/roles/permissions | role:manage |
| GET | /api/admin/roles/{roleId}/permissions | role:manage |
| PUT | /api/admin/roles/{roleId}/permissions | role:manage |
| GET | /api/rooms | 公开 |
| GET | /api/rooms/available | 公开 |
| GET | /api/rooms/{id} | 公开 |
| GET | /api/rooms/schedule | booking:view |
| POST | /api/admin/rooms | room:manage |
| PUT | /api/admin/rooms/{id} | room:manage |
| PUT | /api/admin/rooms/{id}/status | room:manage |
| GET | /api/buildings | 公开 |
| POST | /api/admin/buildings | building:manage |
| PUT | /api/admin/buildings/{id} | building:manage |
| GET | /api/admin/booking-rules | config:view |
| POST | /api/admin/booking-rules | config:manage |
| POST | /api/bookings | booking:manage |
| PUT | /api/bookings/{id} | booking:manage |
| POST | /api/bookings/{id}/cancel | booking:manage |
| GET | /api/bookings/{id} | booking:view |
| GET | /api/bookings/mine | 公开（按 bookerId 过滤） |
| GET | /api/bookings/today | 公开（按 bookerId 过滤） |
| GET | /api/bookings/search | booking:view |
| POST | /api/bookings/check-conflict | 公开（预检） |

## 关键实现

- **AuthDataInitializer**：启动时初始化权限数据 + 角色-权限绑定
- **AdminInterceptor**：解析 `X-Meetr-User-Id` 请求头，填充 `UserContext`
- **PermissionInterceptor**：读取方法上的 `@RequirePermission` 注解，调用 `AuthService.hasPermission` 校验
- **@RequirePermission**：标记 Controller 方法所需权限码

## 前端页面

| 页面 | 路由 | 说明 |
|---|---|---|
| 用户管理 | /admin/users | 用户列表 + 角色分配 |
| 角色管理 | /admin/roles | 角色列表 + 权限配置 |
