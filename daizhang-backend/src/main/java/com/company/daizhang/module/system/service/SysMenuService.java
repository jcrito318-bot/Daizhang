package com.company.daizhang.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.module.system.dto.MenuCreateRequest;
import com.company.daizhang.module.system.dto.MenuUpdateRequest;
import com.company.daizhang.module.system.entity.SysMenu;
import com.company.daizhang.module.system.vo.MenuVO;

import java.util.List;

/**
 * 菜单服务接口
 */
public interface SysMenuService extends IService<SysMenu> {
    
    /**
     * 查询菜单树
     */
    List<MenuVO> getMenuTree();
    
    /**
     * 根据ID查询菜单
     */
    MenuVO getMenuById(Long id);
    
    /**
     * 创建菜单
     */
    void createMenu(MenuCreateRequest request);
    
    /**
     * 更新菜单
     */
    void updateMenu(Long id, MenuUpdateRequest request);
    
    /**
     * 删除菜单
     */
    void deleteMenu(Long id);
    
    /**
     * 根据用户ID查询菜单树
     */
    List<MenuVO> getMenuTreeByUserId(Long userId);
    
    /**
     * 根据用户ID查询权限列表
     */
    List<String> getPermissionsByUserId(Long userId);
}
