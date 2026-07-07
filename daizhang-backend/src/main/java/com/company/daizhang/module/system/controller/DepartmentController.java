package com.company.daizhang.module.system.controller;

import cn.hutool.core.bean.BeanUtil;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.system.dto.DepartmentRequest;
import com.company.daizhang.module.system.entity.Department;
import com.company.daizhang.module.system.service.DepartmentService;
import com.company.daizhang.module.system.vo.DepartmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 */
@Slf4j
@Tag(name = "部门管理")
@RestController
@RequestMapping("/system/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "部门树")
    @GetMapping("/tree")
    public Result<List<DepartmentVO>> tree() {
        List<DepartmentVO> tree = departmentService.getDepartmentTree();
        return Result.success(tree);
    }

    @Operation(summary = "创建部门")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> create(@Valid @RequestBody DepartmentRequest request) {
        Department entity = new Department();
        BeanUtil.copyProperties(request, entity);
        departmentService.createDepartment(entity);
        return Result.success();
    }

    @Operation(summary = "更新部门")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        Department entity = new Department();
        BeanUtil.copyProperties(request, entity);
        entity.setId(id);
        departmentService.updateDepartment(entity);
        return Result.success();
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return Result.success();
    }
}
