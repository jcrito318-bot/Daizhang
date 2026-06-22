package com.company.daizhang.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.dto.RoleCreateRequest;
import com.company.daizhang.module.system.dto.RoleMenuAssignRequest;
import com.company.daizhang.module.system.dto.RoleQueryRequest;
import com.company.daizhang.module.system.dto.RoleUpdateRequest;
import com.company.daizhang.module.system.entity.SysRole;
import com.company.daizhang.module.system.vo.MenuVO;
import com.company.daizhang.module.system.vo.RoleVO;

import java.util.List;

/**
 * 角色服务接口
 */
public interface SysRoleService extends IService<SysRole> {
    
    /**
     * 分页查询角色
     */
    PageResult<RoleVO> pageRoles(RoleQueryRequest request);
    
    /**
     * 查询所有角色
     */
    List<RoleVO> listAllRoles();
    
    /**
     * 根据ID查询角色
     */
    RoleVO getRoleById(Long id);
    
    /**
     * 创建角色
     */
    void createRole(RoleCreateRequest request);
    
    /**
     * 更新角色
     */
    void updateRole(Long id, RoleUpdateRequest request);
    
    /**
     * 删除角色
     */
    void deleteRole(Long id);
    
    /**
     * 分配角色菜单权限
     */
    void assignRoleMenus(Long roleId, RoleMenuAssignRequest request);
    
    /**
     * 查询角色菜单ID列表
     */
    List<Long> getRoleMenuIds(Long roleId);
    
    /**
     * 查询角色菜单树
     */
    List<MenuVO> getRoleMenuTree(Long roleId);
}
