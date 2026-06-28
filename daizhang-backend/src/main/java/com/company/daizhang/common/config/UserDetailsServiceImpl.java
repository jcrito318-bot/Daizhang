package com.company.daizhang.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.module.system.entity.SysMenu;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysMenuMapper;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户详情服务实现
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

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
        // 超级管理员(id=1)拥有全部权限,避免admin因菜单未配置而被锁死
        if (user.getId() != null && user.getId() == 1L) {
            permissions.add("*");
        } else {
            List<SysMenu> menus = sysMenuMapper.selectMenusByUserId(user.getId());
            if (menus != null) {
                for (SysMenu menu : menus) {
                    if (menu.getPermission() != null && !menu.getPermission().isEmpty()) {
                        permissions.add(menu.getPermission());
                    }
                }
            }
        }

        return new SecurityUserDetails(user, permissions);
    }
}
