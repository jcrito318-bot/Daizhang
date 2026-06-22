package com.company.daizhang.module.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.dto.RoleCreateRequest;
import com.company.daizhang.module.system.dto.RoleMenuAssignRequest;
import com.company.daizhang.module.system.dto.RoleQueryRequest;
import com.company.daizhang.module.system.dto.RoleUpdateRequest;
import com.company.daizhang.module.system.entity.SysMenu;
import com.company.daizhang.module.system.entity.SysRole;
import com.company.daizhang.module.system.entity.SysRoleMenu;
import com.company.daizhang.module.system.entity.SysUserRole;
import com.company.daizhang.module.system.mapper.SysMenuMapper;
import com.company.daizhang.module.system.mapper.SysRoleMapper;
import com.company.daizhang.module.system.mapper.SysRoleMenuMapper;
import com.company.daizhang.module.system.mapper.SysUserRoleMapper;
import com.company.daizhang.module.system.service.SysMenuService;
import com.company.daizhang.module.system.service.SysRoleService;
import com.company.daizhang.module.system.vo.MenuVO;
import com.company.daizhang.module.system.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {
    
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuService menuService;
    
    @Override
    public PageResult<RoleVO> pageRoles(RoleQueryRequest request) {
        Page<SysRole> page = new Page<>(request.getPageNum(), request.getPageSize());
        
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(request.getRoleName()), SysRole::getRoleName, request.getRoleName())
               .like(StrUtil.isNotBlank(request.getRoleCode()), SysRole::getRoleCode, request.getRoleCode())
               .eq(request.getStatus() != null, SysRole::getStatus, request.getStatus())
               .orderByDesc(SysRole::getCreateTime);
        
        Page<SysRole> result = this.page(page, wrapper);
        
        List<RoleVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }
    
    @Override
    public List<RoleVO> listAllRoles() {
        List<SysRole> roles = this.list();
        return roles.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
    
    @Override
    public RoleVO getRoleById(Long id) {
        // 业务校验：角色ID不能为空
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空");
        }
        
        SysRole role = this.getById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return convertToVO(role);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRole(RoleCreateRequest request) {
        // 业务校验：角色名称不能为空
        if (StrUtil.isBlank(request.getRoleName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色名称不能为空");
        }
        
        // 业务校验：角色编码不能为空
        if (StrUtil.isBlank(request.getRoleCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色编码不能为空");
        }
        
        // 业务校验：角色状态值必须是0或1
        if (request.getStatus() != null && request.getStatus() != 0 && request.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色状态值不正确");
        }
        
        // 检查角色编码是否已存在
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleCode, request.getRoleCode());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.ROLE_CODE_DUPLICATE);
        }
        
        SysRole role = new SysRole();
        BeanUtil.copyProperties(request, role);
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        this.save(role);
        
        log.info("创建角色成功，角色编码: {}, 角色名称: {}", role.getRoleCode(), role.getRoleName());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, RoleUpdateRequest request) {
        // 业务校验：角色ID不能为空
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空");
        }
        
        SysRole role = this.getById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        
        // 业务校验：角色状态值必须是0或1
        if (request.getStatus() != null && request.getStatus() != 0 && request.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色状态值不正确");
        }
        
        // 如果修改了角色编码，检查是否与其他角色冲突
        if (StrUtil.isNotBlank(request.getRoleCode()) && !request.getRoleCode().equals(role.getRoleCode())) {
            LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysRole::getRoleCode, request.getRoleCode());
            wrapper.ne(SysRole::getId, id);
            if (this.count(wrapper) > 0) {
                throw new BusinessException(ErrorCode.ROLE_CODE_DUPLICATE);
            }
        }
        
        BeanUtil.copyProperties(request, role);
        this.updateById(role);
        
        log.info("更新角色成功，角色ID: {}", id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        // 业务校验：角色ID不能为空
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空");
        }
        
        SysRole role = this.getById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        
        // 检查是否有用户使用该角色
        LambdaQueryWrapper<SysUserRole> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(SysUserRole::getRoleId, id);
        if (userRoleMapper.selectCount(urWrapper) > 0) {
            throw new BusinessException(ErrorCode.ROLE_HAS_USERS);
        }
        
        this.removeById(id);
        
        // 删除角色菜单关联
        LambdaQueryWrapper<SysRoleMenu> rmWrapper = new LambdaQueryWrapper<>();
        rmWrapper.eq(SysRoleMenu::getRoleId, id);
        roleMenuMapper.delete(rmWrapper);
        
        log.info("删除角色成功，角色ID: {}, 角色编码: {}", id, role.getRoleCode());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoleMenus(Long roleId, RoleMenuAssignRequest request) {
        // 业务校验：角色ID不能为空
        if (roleId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空");
        }
        
        SysRole role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        
        // 业务校验：菜单ID列表不能为空
        if (request.getMenuIds() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "菜单ID列表不能为空");
        }
        
        // 业务校验：菜单必须存在
        for (Long menuId : request.getMenuIds()) {
            SysMenu menu = menuService.getById(menuId);
            if (menu == null) {
                throw new BusinessException(ErrorCode.MENU_NOT_FOUND, "菜单ID " + menuId + " 不存在");
            }
        }
        
        // 删除原有菜单关联
        LambdaQueryWrapper<SysRoleMenu> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysRoleMenu::getRoleId, roleId);
        roleMenuMapper.delete(deleteWrapper);
        
        // 保存新菜单关联
        for (Long menuId : request.getMenuIds()) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenuMapper.insert(roleMenu);
        }
        
        log.info("分配角色菜单成功，角色ID: {}, 菜单数量: {}", roleId, request.getMenuIds().size());
    }
    
    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        // 业务校验：角色ID不能为空
        if (roleId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空");
        }
        
        SysRole role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId, roleId);
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(wrapper);
        
        return roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<MenuVO> getRoleMenuTree(Long roleId) {
        // 业务校验：角色ID不能为空
        if (roleId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色ID不能为空");
        }
        
        SysRole role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        
        List<Long> menuIds = getRoleMenuIds(roleId);
        List<MenuVO> allMenus = menuService.getMenuTree();
        
        // 过滤出角色拥有的菜单
        return filterMenus(allMenus, menuIds);
    }
    
    private List<MenuVO> filterMenus(List<MenuVO> menus, List<Long> menuIds) {
        return menus.stream()
                .filter(menu -> menuIds.contains(menu.getId()))
                .peek(menu -> {
                    if (menu.getChildren() != null) {
                        menu.setChildren(filterMenus(menu.getChildren(), menuIds));
                    }
                })
                .collect(Collectors.toList());
    }
    
    private RoleVO convertToVO(SysRole role) {
        RoleVO vo = new RoleVO();
        BeanUtil.copyProperties(role, vo);
        return vo;
    }
}
