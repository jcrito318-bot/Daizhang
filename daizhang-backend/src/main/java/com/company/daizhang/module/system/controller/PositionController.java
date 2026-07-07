package com.company.daizhang.module.system.controller;

import cn.hutool.core.bean.BeanUtil;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.system.dto.PositionRequest;
import com.company.daizhang.module.system.entity.Position;
import com.company.daizhang.module.system.service.PositionService;
import com.company.daizhang.module.system.vo.PositionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 岗位管理控制器
 */
@Slf4j
@Tag(name = "岗位管理")
@RestController
@RequestMapping("/system/position")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @Operation(summary = "分页查询岗位")
    @GetMapping("/page")
    public Result<PageResult<PositionVO>> page(@RequestParam(required = false) String positionName,
                                                @RequestParam(required = false) String positionCode,
                                                @RequestParam(required = false) Long departmentId,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<PositionVO> page = positionService.pagePositions(positionName, positionCode, departmentId,
                status, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "岗位列表")
    @GetMapping("/list")
    public Result<List<PositionVO>> list() {
        List<PositionVO> list = positionService.listPositions();
        return Result.success(list);
    }

    @Operation(summary = "创建岗位")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> create(@Valid @RequestBody PositionRequest request) {
        Position entity = new Position();
        BeanUtil.copyProperties(request, entity);
        positionService.createPosition(entity);
        return Result.success();
    }

    @Operation(summary = "更新岗位")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PositionRequest request) {
        Position entity = new Position();
        BeanUtil.copyProperties(request, entity);
        entity.setId(id);
        positionService.updatePosition(entity);
        return Result.success();
    }

    @Operation(summary = "删除岗位")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        positionService.deletePosition(id);
        return Result.success();
    }
}
