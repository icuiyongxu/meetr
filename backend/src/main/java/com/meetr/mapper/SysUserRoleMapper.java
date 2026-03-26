package com.meetr.mapper;

import com.meetr.domain.entity.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserRoleMapper {

    @Select("select id, user_id, role_id from sys_user_role where user_id = #{userId} order by id asc")
    List<SysUserRole> findByUserId(@Param("userId") Long userId);

    @Delete("delete from sys_user_role where user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    @Delete("delete from sys_user_role where id = #{id}")
    void deleteById(@Param("id") Long id);

    @Insert("insert into sys_user_role (user_id, role_id) values (#{userId}, #{roleId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUserRole relation);
}
