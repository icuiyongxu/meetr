package com.meetr.config;

import com.meetr.domain.entity.SysUser;
import com.meetr.domain.entity.SysUserRole;
import com.meetr.mapper.SysUserMapper;
import com.meetr.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthDataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    /**
     * 启动后修正：
     * 给所有未分配角色的用户补上 USER 角色
     */
    @Override
    public void run(String... args) {
        Long userRoleId = 2L; // USER 角色的 id

        List<SysUser> allUsers = sysUserMapper.findAll();
        for (SysUser user : allUsers) {
            if (sysUserRoleMapper.findByUserId(user.getId()).isEmpty()) {
                SysUserRole relation = new SysUserRole();
                relation.setUserId(user.getId());
                relation.setRoleId(userRoleId);
                sysUserRoleMapper.insert(relation);
                log.info("为未分配角色用户 {} 补发 USER 角色", user.getUserId());
            }
        }
        log.info("AuthDataInitializer 完成，共处理 {} 个用户", allUsers.size());
    }
}
