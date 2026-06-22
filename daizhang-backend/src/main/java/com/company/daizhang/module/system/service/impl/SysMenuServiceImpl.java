package com.company.daizhang.module.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.system.dto.MenuCreateRequest;
import com.company.daizhang.module.system.dto.MenuUpdateRequest;
import com.company.daizhang.module.system.entity.SysMenu;
import com.company.daizhang.module.system.entity.SysRoleMenu;
import com.company.daizhang.module.system.mapper.SysMenuMapper;
import com.company.daizhang.module.system.mapper.SysRoleMenuMapper;
import com.company.daizhang.module.system.service.SysMenuService;
import com.company.daizhang.module.system.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {
    
    private final SysRoleMenuMapper roleMenuMapper;
    
    @Override
    public List<MenuVO> getMenuTree() {
        List<SysMenu> allMenus = this.list();
        return buildMenuTree(allMenus, 0L);
    }
    
    @Override
    public MenuVO getMenuById(Long id) {
        // 业务校验：菜单ID不能为空
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "菜单ID不能为空");
        }
        
        SysMenu menu = this.getById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        return convertToVO(menu);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createMenu(MenuCreateRequest request) {
        // 业务校验：菜单名称不能为空
        if (StrUtil.isBlank(request.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "菜单名称不能为空");
        }
        
        // 业务校验：菜单类型必须是0-目录 1-菜单 2-按钮
        if (request.getMenuType() != null && request.getMenuType() != 0 && request.getMenuType() != 1 && request.getMenuType() != 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "菜单类型不正确");
        }
        
        // 业务校验：排序号不能为负数
        if (request.getSortOrder() != null && request.getSortOrder() < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "排序号不能为负数");
        }
        
        // 业务校验：是否可见必须是0或1
        if (request.getVisible() != null && request.getVisible() != 0 && request.getVisible() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "是否可见值不正确");
        }
        
        // 业务校验：状态值必须是0或1
        if (request.getStatus() != null && request.getStatus() != 0 && request.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态值不正确");
        }
        
        // 如果有上级菜单，校验上级菜单是否存在
        if (request.getParentId() != null && request.getParentId() > 0) {
            SysMenu parentMenu = this.getById(request.getParentId());
            if (parentMenu == null) {
                throw new BusinessException(ErrorCode.MENU_PARENT_NOT_FOUND);
            }
        }
        
        SysMenu menu = new SysMenu();
        BeanUtil.copyProperties(request, menu);
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getSortOrder() == null) {
            menu.setSortOrder(0);
        }
        if (menu.getVisible() == null) {
            menu.setVisible(1);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(1);
        }
        this.save(menu);
        
        log.info("创建菜单成功，菜单名称: {}", menu.getName());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(Long id, MenuUpdateRequest request) {
        // 业务校验：菜单ID不能为空
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "菜单ID不能为空");
        }
        
        SysMenu menu = this.getById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        
        // 业务校验：上级菜单不能选择自身
        if (request.getParentId() != null && request.getParentId().equals(id)) {
            throw new BusinessException(ErrorCode.MENU_SELF_REFERENCE);
        }
        
        // 业务校验：菜单类型必须是0-目录 1-菜单 2-按钮
        if (request.getMenuType() != null && request.getMenuType() != 0 && request.getMenuType() != 1 && request.getMenuType() != 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "菜单类型不正确");
        }
        
        // 业务校验：排序号不能为负数
        if (request.getSortOrder() != null && request.getSortOrder() < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "排序号不能为负数");
        }
        
        // 业务校验：是否可见必须是0或1
        if (request.getVisible() != null && request.getVisible() != 0 && request.getVisible() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "是否可见值不正确");
        }
        
        // 业务校验：状态值必须是0或1
        if (request.getStatus() != null && request.getStatus() != 0 && request.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态值不正确");
        }
        
        // 如果有上级菜单，校验上级菜单是否存在
        if (request.getParentId() != null && request.getParentId() > 0) {
            SysMenu parentMenu = this.getById(request.getParentId());
            if (parentMenu == null) {
                throw new BusinessException(ErrorCode.MENU_PARENT_NOT_FOUND);
            }
        }
        
        BeanUtil.copyProperties(request, menu);
        this.updateById(menu);
        
        log.info("更新菜单成功，菜单ID: {}", id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        // 业务校验：菜单ID不能为空
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "菜单ID不能为空");
        }
        
        SysMenu menu = this.getById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        
        // 检查是否有子菜单
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId, id);
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.MENU_HAS_CHILDREN);
        }
        
        this.removeById(id);
        
        // 删除角色菜单关联
        LambdaQueryWrapper<SysRoleMenu> rmWrapper = new LambdaQueryWrapper<>();
        rmWrapper.eq(SysRoleMenu::getMenuId, id);
        roleMenuMapper.delete(rmWrapper);
        
        log.info("删除菜单成功，菜单ID: {}, 菜单名称: {}", id, menu.getName());
    }
    
    @Override
    public List<MenuVO> getMenuTreeByUserId(Long userId) {
        // 业务校验：用户ID不能为空
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        
        List<SysMenu> menus = baseMapper.selectMenusByUserId(userId);
        return buildMenuTree(menus, 0L);
    }
    
    @Override
    public List<String> getPermissionsByUserId(Long userId) {
        // 业务校验：用户ID不能为空
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        
        List<SysMenu> menus = baseMapper.selectMenusByUserId(userId);
        return menus.stream()
                .filter(menu -> menu.getPermission() != null && !menu.getPermission().isEmpty())
                .map(SysMenu::getPermission)
                .collect(Collectors.toList());
    }
    
    private List<MenuVO> buildMenuTree(List<SysMenu> menus, Long parentId) {
        List<MenuVO> tree = new ArrayList<>();
        
        for (SysMenu menu : menus) {
            if (parentId.equals(menu.getParentId())) {
                MenuVO vo = convertToVO(menu);
                vo.setChildren(buildMenuTree(menus, menu.getId()));
                tree.add(vo);
            }
        }
        
        // 按排序号排序
        tree.sort((a, b) -> {
            int sortA = a.getSortOrder() != null ? a.getSortOrder() : 0;
            int sortB = b.getSortOrder() != null ? b.getSortOrder() : 0;
            return sortA - sortB;
        });
        
        return tree;
    }
    
    private MenuVO convertToVO(SysMenu menu) {
        MenuVO vo = new MenuVO();
        BeanUtil.copyProperties(menu, vo);
        return vo;
    }
}
