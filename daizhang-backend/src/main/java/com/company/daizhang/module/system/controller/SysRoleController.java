package com.company.daizhang.module.system.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.system.dto.RoleCreateRequest;
import com.company.daizhang.module.system.dto.RoleMenuAssignRequest;
import com.company.daizhang.module.system.dto.RoleQueryRequest;
import com.company.daizhang.module.system.dto.RoleUpdateRequest;
import com.company.daizhang.module.system.service.SysRoleService;
import com.company.daizhang.module.system.vo.MenuVO;
import com.company.daizhang.module.system.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {
    
    private final SysRoleService roleService;
    
    @Operation(summary = "分页查询角色")
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<RoleVO>> page(RoleQueryRequest request) {
        PageResult<RoleVO> page = roleService.pageRoles(request);
        return Result.success(page);
    }

    @Operation(summary = "查询所有角色")
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<RoleVO>> list() {
        List<RoleVO> list = roleService.listAllRoles();
        return Result.success(list);
    }

    @Operation(summary = "根据ID查询角色")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<RoleVO> getById(@PathVariable Long id) {
        RoleVO role = roleService.getRoleById(id);
        return Result.success(role);
    }
    
    @Operation(summary = "创建角色")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> create(@Valid @RequestBody RoleCreateRequest request) {
        roleService.createRole(request);
        return Result.success();
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        roleService.updateRole(id, request);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @Operation(summary = "分配角色菜单权限")
    @PutMapping("/{id}/menus")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody RoleMenuAssignRequest request) {
        roleService.assignRoleMenus(id, request);
        return Result.success();
    }
    
    @Operation(summary = "查询角色菜单ID列表")
    @GetMapping("/{id}/menu-ids")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Long>> getMenuIds(@PathVariable Long id) {
        List<Long> menuIds = roleService.getRoleMenuIds(id);
        return Result.success(menuIds);
    }

    @Operation(summary = "查询角色菜单树")
    @GetMapping("/{id}/menus")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<MenuVO>> getMenuTree(@PathVariable Long id) {
        List<MenuVO> tree = roleService.getRoleMenuTree(id);
        return Result.success(tree);
    }
}
