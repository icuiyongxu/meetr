package com.meetr.mapper;

import com.meetr.domain.entity.SysUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysUserMapper {

    @Select("select id, user_id, name, password, status, created_at_ms, updated_at_ms, email, email_enabled, calendar_token from sys_user where user_id = #{userId} limit 1")
    SysUser findByUserId(@Param("userId") String userId);

    @Select("select id, user_id, name, password, status, created_at_ms, updated_at_ms, email, email_enabled, calendar_token from sys_user where user_id = #{userId} limit 1")
    SysUser findByUserIdWithEmail(@Param("userId") String userId);

    @Select("select count(1) > 0 from sys_user where user_id = #{userId}")
    boolean existsByUserId(@Param("userId") String userId);

    @Select("select id, user_id, name, password, status, created_at_ms, updated_at_ms, email, email_enabled, calendar_token from sys_user where id = #{id} limit 1")
    SysUser findById(@Param("id") Long id);

    @Select("select id, user_id, name, password, status, created_at_ms, updated_at_ms, email, email_enabled, calendar_token from sys_user order by id asc")
    List<SysUser> findAll();

    @Insert("insert into sys_user (user_id, name, password, status, created_at_ms, updated_at_ms, email, email_enabled, calendar_token) values (#{userId}, #{name}, #{password}, #{status}, #{createdAtMs}, #{updatedAtMs}, #{email}, #{emailEnabled}, #{calendarToken})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);

    @Update("update sys_user set name=#{name}, password=#{password}, status=#{status}, updated_at_ms=#{updatedAtMs}, email=#{email}, email_enabled=#{emailEnabled}, calendar_token=#{calendarToken} where id=#{id}")
    int update(SysUser user);

    @Delete("delete from sys_user where id = #{id}")
    int deleteById(@Param("id") Long id);
}
