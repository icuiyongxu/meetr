package com.meetr.controller;

import com.meetr.config.RequirePermission;
import com.meetr.domain.entity.Equipment;
import com.meetr.mapper.EquipmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentMapper equipmentMapper;

    /** 设备列表（公开，供会议室表单使用） */
    @GetMapping("/equipments")
    public ApiResponse<List<Equipment>> list() {
        return ApiResponse.ok(equipmentMapper.findAll());
    }

    /** 创建设备 */
    @RequirePermission("config:manage")
    @PostMapping("/admin/equipments")
    public ApiResponse<Equipment> create(@RequestBody CreateRequest req) {
        if (equipmentMapper.findByCode(req.code()) != null) {
            throw new com.meetr.exception.BusinessException(40001, "设备编码已存在");
        }
        Equipment e = new Equipment();
        e.setCode(req.code());
        e.setName(req.name());
        e.setStatus("ACTIVE");
        equipmentMapper.insert(e);
        return ApiResponse.ok(e);
    }

    /** 更新设备 */
    @RequirePermission("config:manage")
    @PutMapping("/admin/equipments/{id}")
    public ApiResponse<Equipment> update(@PathVariable Long id, @RequestBody UpdateRequest req) {
        Equipment e = equipmentMapper.findById(id);
        if (e == null) {
            throw new com.meetr.exception.BusinessException(40401, "设备不存在");
        }
        if (req.name() != null) e.setName(req.name());
        if (req.status() != null) e.setStatus(req.status());
        equipmentMapper.update(e);
        return ApiResponse.ok(e);
    }

    /** 删除设备 */
    @RequirePermission("config:manage")
    @DeleteMapping("/admin/equipments/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        equipmentMapper.delete(id);
        return ApiResponse.ok(null);
    }

    public record CreateRequest(String code, String name) {}
    public record UpdateRequest(String name, String status) {}
}
