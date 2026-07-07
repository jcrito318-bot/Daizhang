package com.company.daizhang.module.system.controller;

import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.system.service.DataBackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据备份控制器
 */
@Tag(name = "数据备份管理")
@RestController
@RequestMapping("/system/backup")
@RequiredArgsConstructor
public class DataBackupController {

    private final DataBackupService dataBackupService;

    @Operation(summary = "备份数据库")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> backup() {
        String fileName = dataBackupService.backup();
        return Result.success(fileName);
    }

    @Operation(summary = "列出所有备份文件")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        List<Map<String, Object>> list = dataBackupService.listBackups();
        return Result.success(list);
    }

    @Operation(summary = "恢复数据库（需要重启应用）")
    @PostMapping("/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> restore(@RequestParam String fileName) {
        dataBackupService.restore(fileName);
        return Result.success();
    }

    @Operation(summary = "删除备份文件")
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@RequestParam String fileName) {
        dataBackupService.deleteBackup(fileName);
        return Result.success();
    }
}
