package com.meetr.mapper;

import com.meetr.domain.entity.Building;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BuildingMapper {

    @Select("""
        select id, name, campus, address, sort_no, status, created_at_ms, updated_at_ms
        from building
        order by sort_no asc, id asc
        """)
    List<Building> findAllByOrderBySortNoAscIdAsc();

    @Select("""
        select id, name, campus, address, sort_no, status, created_at_ms, updated_at_ms
        from building
        where id = #{id}
        """)
    Building findById(@Param("id") Long id);

    @Select({"<script>",
        "select id, name, campus, address, sort_no, status, created_at_ms, updated_at_ms from building",
        "where id in",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"})
    List<Building> findAllByIds(@Param("ids") List<Long> ids);

    @Insert("""
        insert into building (name, campus, address, sort_no, status, created_at_ms, updated_at_ms)
        values (#{name}, #{campus}, #{address}, #{sortNo}, #{status}, #{createdAtMs}, #{updatedAtMs})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Building building);

    @Update("""
        update building
        set name = #{name},
            campus = #{campus},
            address = #{address},
            sort_no = #{sortNo},
            status = #{status},
            updated_at_ms = #{updatedAtMs}
        where id = #{id}
        """)
    int update(Building building);
}
