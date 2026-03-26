package com.meetr.mapper;

import com.meetr.domain.entity.SysRole;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper {

    @Select("select id, code, name, description from sys_role where code = #{code} limit 1")
    SysRole findByCode(@Param("code") String code);

    @Select("select id, code, name, description from sys_role where id = #{id} limit 1")
    SysRole findById(@Param("id") Long id);

    @Select("select id, code, name, description from sys_role order by id asc")
    List<SysRole> findAll();

    @Insert("insert into sys_role (code, name, description) values (#{code}, #{name}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysRole role);
}
