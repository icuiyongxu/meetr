package com.meetr.mapper;

import com.meetr.domain.entity.Equipment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EquipmentMapper {

    @Select("select id, code, name, status from sys_equipment where id = #{id} limit 1")
    Equipment findById(@Param("id") Long id);

    @Select("select id, code, name, status from sys_equipment where code = #{code} limit 1")
    Equipment findByCode(@Param("code") String code);

    @Select("select id, code, name, status from sys_equipment order by id asc")
    List<Equipment> findAll();

    @Insert("insert into sys_equipment (code, name, status) values (#{code}, #{name}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Equipment equipment);

    @Update("update sys_equipment set name = #{name}, status = #{status} where id = #{id}")
    int update(Equipment equipment);

    @Delete("delete from sys_equipment where id = #{id}")
    int delete(@Param("id") Long id);
}
