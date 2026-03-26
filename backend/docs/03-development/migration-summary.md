# JPA → MyBatis 迁移说明

## 本次改动
- 移除 `spring-boot-starter-data-jpa`，新增 `mybatis-spring-boot-starter` 与 `pagehelper-spring-boot-starter`
- `application.yml` 改为 MyBatis 配置：mapper XML 扫描、驼峰映射、PageHelper 分页
- 所有 `domain/entity` 保留为结果对象，去掉 JPA 注解
- 删除 `domain/repository`，新增 `com.meetr.mapper` Mapper 接口
- `BookingMapper.xml` 迁移 BookingRepository 原有复杂查询：
  - `findConflicting`
  - `findTodayBookings`
  - `countActiveBookingsOnDay`
  - `findByRoomIdAndDate`
  - `searchBookings`
- `MeetingRoomMapper.xml` 承载会议室检索与可用会议室查询
- application/service/controller/config 层全部改为注入 Mapper
- 分页查询从 Spring Data `Pageable` + `JpaRepository` 改为 `PageHelper + PageInfo + PageImpl`
- 原先依赖 JPA 生命周期的时间戳更新，改为 service 层显式调用：
  - 新增前 `initTimestampsForInsert()`
  - 更新前 `touchForUpdate()`

## 迁移后的目录
- Java Mapper：`src/main/java/com/meetr/mapper/`
- XML Mapper：`src/main/resources/mapper/`
- 实体：`src/main/java/com/meetr/domain/entity/`

## 注意事项
- 当前代码已按 MyBatis 方式完成主流程改造，但数据库表结构需与代码字段保持一致，尤其是：
  - `created_at_ms / updated_at_ms`
  - `booking.recurrence_type / recurrence_end_date / parent_id / series_index`
  - `room_config.admin_user_ids`
- 如果本地数据库仍是旧 JPA 自动建表产物，需要补齐缺失字段或执行对应 DDL 迁移
- `Booking.version` 现在在应用层手动递增，不再依赖 JPA `@Version`

## 建议验证项
1. 启动应用，确认 Mapper 扫描正常
2. 验证登录、用户/角色/权限初始化
3. 验证会议室、楼栋、配置增删改查
4. 验证预约创建、修改、取消、冲突检测、日历视图、我的预约、搜索分页
