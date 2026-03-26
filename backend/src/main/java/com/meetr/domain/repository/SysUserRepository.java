package com.meetr.domain.repository;

import com.meetr.domain.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    Optional<SysUser> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
