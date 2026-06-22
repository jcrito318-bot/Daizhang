package com.company.daizhang.module.system.controller;

import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.system.dto.MenuCreateRequest;
import com.company.daizhang.module.system.dto.MenuUpdateRequest;
import com.company.daizhang.module.system.service.SysMenuService;
import com.company.daizhang.module.system.vo.MenuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {
    
    private final SysMenuService menuService;
    
    @Operation(summary = "查询菜单树")
    @GetMapping("/tree")
    public Result<List<MenuVO>> tree() {
        List<MenuVO> tree = menuService.getMenuTree();
        return Result.success(tree);
    }
    
    @Operation(summary = "根据ID查询菜单")
    @GetMapping("/{id}")
    public Result<MenuVO> getById(@PathVariable Long id) {
        MenuVO menu = menuService.getMenuById(id);
        return Result.success(menu);
    }
    
    @Operation(summary = "创建菜单")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody MenuCreateRequest request) {
        menuService.createMenu(request);
        return Result.success();
    }
    
    @Operation(summary = "更新菜单")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MenuUpdateRequest request) {
        menuService.updateMenu(id, request);
        return Result.success();
    }
    
    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.success();
    }
}
