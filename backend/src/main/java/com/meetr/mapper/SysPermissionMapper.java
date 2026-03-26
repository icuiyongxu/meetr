package com.meetr.mapper;

import com.meetr.domain.entity.SysPermission;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper {

    @Select("select id, code, name, description from sys_permission where code = #{code} limit 1")
    SysPermission findByCode(@Param("code") String code);

    @Select("select id, code, name, description from sys_permission where id = #{id} limit 1")
    SysPermission findById(@Param("id") Long id);

    @Select("select id, code, name, description from sys_permission order by id asc")
    List<SysPermission> findAll();

    @Insert("insert into sys_permission (code, name, description) values (#{code}, #{name}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysPermission permission);
}
