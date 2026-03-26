package com.meetr.mapper;

import com.meetr.domain.entity.SysRolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRolePermissionMapper {

    @Select("select id, role_id, permission_id from sys_role_permission where role_id = #{roleId} order by id asc")
    List<SysRolePermission> findByRoleId(@Param("roleId") Long roleId);

    @Delete("delete from sys_role_permission where role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Select("select id, role_id, permission_id from sys_role_permission where role_id = #{roleId} and permission_id = #{permissionId} limit 1")
    SysRolePermission findByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    @Insert("insert into sys_role_permission (role_id, permission_id) values (#{roleId}, #{permissionId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysRolePermission relation);
}
