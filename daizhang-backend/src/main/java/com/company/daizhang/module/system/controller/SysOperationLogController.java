package com.company.daizhang.module.system.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.system.entity.SysOperationLog;
import com.company.daizhang.module.system.service.SysOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志控制器
 */
@Tag(name = "操作日志管理")
@RestController
@RequestMapping("/system/log")
@RequiredArgsConstructor
public class SysOperationLogController {
    
    private final SysOperationLogService operationLogService;
    
    @Operation(summary = "分页查询操作日志")
    @GetMapping("/page")
    public Result<PageResult<SysOperationLog>> page(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<SysOperationLog> page = operationLogService.pageLogs(username, operation, startDate, endDate, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "清理操作日志")
    @DeleteMapping("/clean")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> clean(@RequestParam(required = false) Integer keepDays) {
        operationLogService.cleanOperationLogs(keepDays);
        return Result.success();
    }
}
