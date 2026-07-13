package com.company.daizhang.module.system.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.system.service.LoginLogService;
import com.company.daizhang.module.system.vo.LoginLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 登录日志控制器
 */
@Tag(name = "登录日志管理")
@RestController
@RequestMapping("/system/login-log")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogService loginLogService;

    @Operation(summary = "分页查询登录日志")
    @GetMapping("/page")
    public Result<PageResult<LoginLogVO>> page(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer loginStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<LoginLogVO> page = loginLogService.pageLogs(username, loginStatus, startDate, endDate, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "清理指定日期前的登录日志")
    @DeleteMapping("/clean")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> clean(@RequestParam String beforeDate) {
        loginLogService.deleteLogs(beforeDate);
        return Result.success();
    }
}
