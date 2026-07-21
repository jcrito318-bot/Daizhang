package com.company.daizhang.module.system.backup.controller;

import com.company.daizhang.common.annotation.SensitiveOperation;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.system.backup.dto.CreateBackupRequest;
import com.company.daizhang.module.system.backup.dto.RestoreBackupRequest;
import com.company.daizhang.module.system.backup.entity.BackupRecord;
import com.company.daizhang.module.system.backup.service.BackupService;
import com.company.daizhang.module.system.backup.vo.BackupRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 数据备份控制器 (P3.3)
 * <p>
 * 所有写操作均要求 ADMIN 角色;恢复与删除为敏感操作,额外标注 {@link SensitiveOperation}
 * 要求前端二次确认(请求头 {@code X-Confirm: true})。
 */
@Slf4j
@Tag(name = "数据备份管理")
@RestController
@RequestMapping("/system/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @Operation(summary = "创建备份(异步执行,返回 backupId)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> create(@Valid @RequestBody(required = false) CreateBackupRequest request) {
        Long backupId = backupService.createBackup(request);
        return Result.success(backupId);
    }

    @Operation(summary = "备份记录分页查询")
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<BackupRecordVO>> page(
            @RequestParam(required = false) String backupType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String triggerType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<BackupRecordVO> page = backupService.pageBackups(backupType, status, triggerType, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "下载备份文件")
    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('ADMIN')")
    public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
        BackupRecord record = backupService.getBackupByIdRequired(id);
        Path filePath = Paths.get(record.getFilePath());
        if (!Files.exists(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "备份文件不存在");
            return;
        }
        String fileName = record.getFileName() != null ? record.getFileName() : "backup.zip";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/zip");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + encodedFileName);
        response.setContentLengthLong(Files.size(filePath));
        try (OutputStream os = response.getOutputStream()) {
            Files.copy(filePath, os);
            os.flush();
        }
        log.info("下载备份文件: id={}, fileName={}, 操作人={}", id, fileName,
                com.company.daizhang.common.utils.SecurityUtils.getCurrentUsername());
    }

    @Operation(summary = "恢复备份(危险操作,需二次确认)")
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    @SensitiveOperation("恢复数据库备份")
    public Result<Void> restore(@PathVariable Long id, @Valid @RequestBody RestoreBackupRequest request) {
        // 双重确认:X-Confirm 头(由 SensitiveOperationAspect 校验) + body.confirm
        boolean confirmed = request.getConfirm() != null && request.getConfirm();
        backupService.restoreBackup(id, confirmed);
        return Result.success();
    }

    @Operation(summary = "删除备份(敏感操作,需二次确认)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SensitiveOperation("删除数据库备份")
    public Result<Void> delete(@PathVariable Long id) {
        backupService.deleteBackup(id);
        return Result.success();
    }
}
