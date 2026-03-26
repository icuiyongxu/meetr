package com.meetr.domain.repository;

import com.meetr.domain.entity.SysRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysRolePermissionRepository extends JpaRepository<SysRolePermission, Long> {
    List<SysRolePermission> findByRoleId(Long roleId);
    void deleteByRoleId(Long roleId);
    Optional<SysRolePermission> findByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
