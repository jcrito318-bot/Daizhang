package com.company.daizhang.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.module.system.entity.SysMenu;
import com.company.daizhang.module.system.entity.SysRole;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.entity.SysUserRole;
import com.company.daizhang.module.system.mapper.SysMenuMapper;
import com.company.daizhang.module.system.mapper.SysRoleMapper;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import com.company.daizhang.module.system.mapper.SysUserRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户详情服务实现
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getDeleted, 0)
        );

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 获取用户权限:从数据库查询用户关联角色下的菜单权限标识
        Set<String> permissions = new HashSet<>();
        // 加载用户角色编码作为 Spring Security 角色权限(ROLE_ 前缀),
        // 用于支持 @PreAuthorize("hasRole('XXX')") 方法级权限校验(高危接口治理)
        // 超级管理员通过 sys_user_role + sys_role 表中实际的 ADMIN 角色记录获得 ROLE_ADMIN 权限,
        // 不再硬编码 id=1 赋予 "*" 通配权限(Spring Security 的 hasRole 是精确匹配,"*" 无法命中
        // @PreAuthorize("hasRole('ADMIN')")),避免数据库重置后首位非admin用户(id=1)被误判为超管。
        loadRoleAuthorities(user.getId(), permissions);
        List<SysMenu> menus = sysMenuMapper.selectMenusByUserId(user.getId());
        if (menus != null) {
            for (SysMenu menu : menus) {
                if (menu.getPermission() != null && !menu.getPermission().isEmpty()) {
                    permissions.add(menu.getPermission());
                }
            }
        }

        return new SecurityUserDetails(user, permissions);
    }

    /**
     * 加载用户已分配的角色编码,转换为 Spring Security 角色权限(ROLE_ 前缀)。
     * 角色 role_code 为 "ADMIN" 时对应权限 "ROLE_ADMIN",可被 hasRole('ADMIN') 命中。
     * 仅加载启用状态(status=1)且未删除的角色。
     */
    private void loadRoleAuthorities(Long userId, Set<String> permissions) {
        if (userId == null) {
            return;
        }
        LambdaQueryWrapper<SysUserRole> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(urWrapper);
        if (userRoles.isEmpty()) {
            return;
        }
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, 1)
                .eq(SysRole::getDeleted, 0);
        List<SysRole> roles = sysRoleMapper.selectList(roleWrapper);
        for (SysRole role : roles) {
            if (role.getRoleCode() != null && !role.getRoleCode().isEmpty()) {
                permissions.add("ROLE_" + role.getRoleCode());
            }
        }
    }
}
