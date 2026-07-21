package com.company.daizhang.module.system.backup.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.system.backup.config.BackupProperties;
import com.company.daizhang.module.system.backup.entity.BackupRecord;
import com.company.daizhang.module.system.backup.mapper.BackupRecordMapper;
import com.company.daizhang.module.system.backup.service.BackupService;
import com.company.daizhang.module.system.backup.vo.BackupRecordVO;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.backup.dto.CreateBackupRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 数据备份服务实现 (P3.3)
 * <p>
 * 备份策略(H2 file mode):
 * <ul>
 *   <li>执行 H2 {@code SCRIPT TO 'path'} 导出全库 SQL(含建表语句与数据)</li>
 *   <li>读取 {@code application.yml} 并脱敏敏感字段(password/secret/key/token)</li>
 *   <li>打包为 ZIP(含 backup.sql + application.yml.masked + META.txt)</li>
 *   <li>记录到 backup_record 表</li>
 * </ul>
 * <p>
 * 恢复策略(简化方案):
 * <ul>
 *   <li>从 ZIP 解压 backup.sql</li>
 *   <li>执行 {@code DROP ALL OBJECTS} 清空当前数据库</li>
 *   <li>执行 {@code RUNSCRIPT FROM 'path'} 恢复</li>
 *   <li>提示用户重启应用以确保缓存/连接池状态干净</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupServiceImpl extends ServiceImpl<BackupRecordMapper, BackupRecord> implements BackupService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * ZIP 内 SQL 脚本文件名
     */
    private static final String ZIP_ENTRY_SQL = "backup.sql";

    /**
     * ZIP 内脱敏配置文件名
     */
    private static final String ZIP_ENTRY_CONFIG = "application.yml.masked";

    /**
     * ZIP 内元信息文件名
     */
    private static final String ZIP_ENTRY_META = "META.txt";

    private final BackupProperties backupProperties;
    private final DataSource dataSource;
    /**
     * 异步执行助手。使用 @Lazy 打破循环依赖:
     * BackupServiceImpl → BackupAsyncHelper → BackupService(Impl)。
     * Spring 注入助手代理,实际解析推迟到首次调用时,此时 Service 已完全初始化。
     */
    @Autowired
    @Lazy
    private BackupAsyncHelper backupAsyncHelper;

    @Override
    public Long createBackup(CreateBackupRequest request) {
        String backupType = (request != null && StrUtil.isNotBlank(request.getBackupType()))
                ? request.getBackupType() : "full";
        String remark = request != null ? request.getRemark() : null;

        // 立即创建一条 in_progress 记录,返回 ID 供前端轮询状态
        BackupRecord record = new BackupRecord();
        record.setBackupType(backupType);
        record.setTriggerType("manual");
        record.setStatus("in_progress");
        record.setRemark(remark);
        record.setCreatedBy(SecurityUtils.getCurrentUserId());
        record.setCreatedByName(SecurityUtils.getCurrentUsername());
        record.setCreatedTime(LocalDateTime.now());
        record.setFileSize(0L);
        save(record);

        Long recordId = record.getId();
        log.info("创建手动备份任务,recordId={}, backupType={}, 操作人={}",
                recordId, backupType, SecurityUtils.getCurrentUsername());

        // 异步执行实际备份(通过独立 Helper 走 Spring 代理触发 @Async)
        backupAsyncHelper.executeBackupAsync(recordId, backupType, "manual", remark);
        return recordId;
    }

    @Override
    public void executeBackup(Long recordId, String backupType, String triggerType, String remark) {
        log.info("开始执行备份,recordId={}, backupType={}, triggerType={}", recordId, backupType, triggerType);
        BackupRecord record = getById(recordId);
        if (record == null) {
            log.error("备份记录不存在,recordId={}", recordId);
            return;
        }

        // 确保备份目录存在
        Path backupDir = Paths.get(backupProperties.getDirectory()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            log.error("创建备份目录失败: {}", backupDir, e);
            markFailed(record, "创建备份目录失败: " + e.getMessage());
            return;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String zipFileName = "backup_" + timestamp + "_" + backupType + ".zip";
        Path zipPath = backupDir.resolve(zipFileName);

        // 临时 SQL 文件(SCRIPT TO 输出)
        Path tempSqlPath = backupDir.resolve("temp_" + timestamp + ".sql");
        Path tempConfigPath = backupDir.resolve("temp_" + timestamp + "_config.yml");

        try {
            // 1. 执行 H2 SCRIPT TO 导出全库 SQL
            log.info("执行 H2 SCRIPT TO 导出全库 SQL: {}", tempSqlPath);
            executeScriptTo(tempSqlPath.toString());

            // 2. 读取并脱敏 application.yml
            String maskedConfig = readAndMaskApplicationYml();
            Files.writeString(tempConfigPath, maskedConfig, StandardCharsets.UTF_8);

            // 3. 打包为 ZIP
            long fileSize = packageZip(zipPath, tempSqlPath, tempConfigPath, record, backupType, triggerType);

            // 4. 更新记录为成功
            record.setFileName(zipFileName);
            record.setFilePath(zipPath.toString());
            record.setFileSize(fileSize);
            record.setStatus("success");
            record.setRemark(remark);
            updateById(record);
            log.info("备份成功,recordId={}, fileName={}, size={}字节", recordId, zipFileName, fileSize);

        } catch (Exception e) {
            log.error("备份失败,recordId={}", recordId, e);
            markFailed(record, "备份失败: " + e.getMessage());
        } finally {
            // 清理临时文件
            safeDelete(tempSqlPath);
            safeDelete(tempConfigPath);
        }
    }

    @Override
    public PageResult<BackupRecordVO> pageBackups(String backupType, String status, String triggerType,
                                                   int pageNum, int pageSize) {
        Page<BackupRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BackupRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(backupType), BackupRecord::getBackupType, backupType)
                .eq(StrUtil.isNotBlank(status), BackupRecord::getStatus, status)
                .eq(StrUtil.isNotBlank(triggerType), BackupRecord::getTriggerType, triggerType)
                .orderByDesc(BackupRecord::getCreatedTime);
        Page<BackupRecord> result = this.page(page, wrapper);
        List<BackupRecordVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public BackupRecord getBackupByIdRequired(Long id) {
        BackupRecord record = getById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "备份记录不存在");
        }
        return record;
    }

    @Override
    public void deleteBackup(Long id) {
        BackupRecord record = getBackupByIdRequired(id);
        // 删除物理文件
        if (StrUtil.isNotBlank(record.getFilePath())) {
            Path filePath = Paths.get(record.getFilePath());
            try {
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("删除备份文件成功: {}", filePath);
                }
            } catch (IOException e) {
                log.warn("删除备份文件失败(将继续删除数据库记录): {}", filePath, e);
            }
        }
        // 逻辑删除数据库记录
        removeById(id);
        log.info("删除备份记录成功,id={}, 操作人={}", id, SecurityUtils.getCurrentUsername());
    }

    @Override
    public void restoreBackup(Long id, boolean confirm) {
        if (!confirm) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "恢复备份为危险操作,必须显式 confirm=true 才能执行");
        }
        BackupRecord record = getBackupByIdRequired(id);
        if (!"success".equals(record.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "只能恢复状态为 success 的备份,当前状态: " + record.getStatus());
        }
        Path zipPath = Paths.get(record.getFilePath());
        if (!Files.exists(zipPath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "备份文件不存在: " + record.getFileName());
        }

        log.warn("开始恢复备份,id={}, fileName={}, 操作人={}. 此操作将清空当前数据库!",
                id, record.getFileName(), SecurityUtils.getCurrentUsername());

        // 解压 ZIP 中的 backup.sql 到临时文件
        Path tempSqlPath = null;
        try {
            tempSqlPath = extractSqlFromZip(zipPath);
            if (tempSqlPath == null) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(),
                        "备份文件中未找到 " + ZIP_ENTRY_SQL);
            }
            log.info("已解压备份 SQL 脚本: {}", tempSqlPath);

            // 执行恢复:先 DROP ALL OBJECTS 清空,再 RUNSCRIPT FROM 恢复
            // 注意:H2 的 RUNSCRIPT 不会自动 DROP 已存在对象,需手动 DROP ALL OBJECTS
            executeRestore(tempSqlPath);

            log.warn("恢复备份成功,id={}, fileName={}. 建议重启应用以确保连接池/缓存状态干净.",
                    id, record.getFileName());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("恢复备份失败,id={}", id, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(),
                    "恢复备份失败: " + e.getMessage());
        } finally {
            if (tempSqlPath != null) {
                safeDelete(tempSqlPath);
            }
        }
    }

    @Override
    public void triggerAutoBackup() {
        log.info("定时自动备份触发");
        BackupRecord record = new BackupRecord();
        record.setBackupType("full");
        record.setTriggerType("auto");
        record.setStatus("in_progress");
        record.setRemark("定时自动备份");
        record.setCreatedBy(null);
        record.setCreatedByName("system");
        record.setCreatedTime(LocalDateTime.now());
        record.setFileSize(0L);
        save(record);

        Long recordId = record.getId();
        // 异步执行实际备份(通过独立 Helper 走 Spring 代理触发 @Async),完成后再清理超期备份
        backupAsyncHelper.executeAutoBackupAndCleanup(recordId, "full", "auto", "定时自动备份");
    }

    @Override
    public void cleanupOldBackups(int maxKeep) {
        if (maxKeep <= 0) {
            log.info("maxKeep={} 非正数,跳过清理", maxKeep);
            return;
        }
        // 按创建时间倒序查询所有 success 记录
        LambdaQueryWrapper<BackupRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BackupRecord::getStatus, "success")
                .orderByDesc(BackupRecord::getCreatedTime);
        List<BackupRecord> all = list(wrapper);
        if (all.size() <= maxKeep) {
            log.info("当前备份份数={}, 未超过 maxKeep={}, 跳过清理", all.size(), maxKeep);
            return;
        }
        // 删除超出 maxKeep 的最旧备份
        List<BackupRecord> toDelete = all.subList(maxKeep, all.size());
        log.info("清理超期旧备份,当前={}, maxKeep={}, 将删除={} 份", all.size(), maxKeep, toDelete.size());
        for (BackupRecord record : toDelete) {
            try {
                if (StrUtil.isNotBlank(record.getFilePath())) {
                    Path filePath = Paths.get(record.getFilePath());
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                    }
                }
                removeById(record.getId());
                log.info("已清理超期备份: id={}, fileName={}", record.getId(), record.getFileName());
            } catch (Exception e) {
                log.warn("清理超期备份失败: id={}, fileName={}", record.getId(), record.getFileName(), e);
            }
        }
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 执行 H2 SCRIPT TO 命令导出全库 SQL。
     */
    private void executeScriptTo(String targetPath) throws SQLException {
        // H2 SCRIPT TO 必须用 Statement 执行(不能 PreparedStatement),路径需用单引号包裹
        String sql = "SCRIPT TO '" + targetPath.replace("'", "''") + "'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    /**
     * 执行恢复:先 DROP ALL OBJECTS,再 RUNSCRIPT FROM。
     */
    private void executeRestore(Path sqlPath) throws SQLException {
        String scriptPath = sqlPath.toString().replace("'", "''");
        try (Connection conn = dataSource.getConnection()) {
            // 1. 清空当前所有对象(表/视图/索引等)
            try (PreparedStatement dropStmt = conn.prepareStatement("DROP ALL OBJECTS")) {
                dropStmt.execute();
                log.info("已执行 DROP ALL OBJECTS,当前数据库对象已清空");
            }
            // 2. 从 SQL 脚本恢复
            String runscriptSql = "RUNSCRIPT FROM '" + scriptPath + "'";
            try (PreparedStatement restoreStmt = conn.prepareStatement(runscriptSql)) {
                restoreStmt.execute();
                log.info("已执行 RUNSCRIPT FROM '{}',数据库已恢复", scriptPath);
            }
        }
    }

    /**
     * 从 classpath 读取 application.yml 并脱敏敏感字段。
     * 读不到则返回占位说明。
     */
    private String readAndMaskApplicationYml() {
        try {
            ClassPathResource resource = new ClassPathResource("application.yml");
            if (!resource.exists()) {
                return "# application.yml not found in classpath\n";
            }
            try (InputStream is = resource.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(maskConfigLine(line)).append('\n');
                }
                return sb.toString();
            }
        } catch (IOException e) {
            log.warn("读取 application.yml 失败,将使用占位说明", e);
            return "# application.yml read failed: " + e.getMessage() + "\n";
        }
    }

    /**
     * 对单行配置进行脱敏:若行包含敏感关键字且为 "key: value" 形式,将 value 替换为 ***。
     */
    private String maskConfigLine(String line) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        // 跳过注释行
        String trimmed = line.trim();
        if (trimmed.startsWith("#") || trimmed.startsWith("-")) {
            return line;
        }
        // 不包含冒号的行不处理
        int colonIdx = line.indexOf(':');
        if (colonIdx < 0) {
            return line;
        }
        String keyPart = line.substring(0, colonIdx).toLowerCase();
        // 仅当 key 含敏感关键字时脱敏
        if (!keyPart.matches(".*(?:password|passwd|pwd|secret|api[-_]?key|token|credential|authorization|cookie|private[-_]?key|salt|session[-_]?id).*")) {
            return line;
        }
        // 保留 key: 部分,将 value 替换为 ***
        // 注意环境变量占位 ${VAR:default} 也需脱敏
        return line.substring(0, colonIdx + 1) + " ***";
    }

    /**
     * 将 SQL 文件、脱敏配置、元信息打包为 ZIP。
     *
     * @return ZIP 文件大小(字节)
     */
    private long packageZip(Path zipPath, Path sqlPath, Path configPath, BackupRecord record,
                            String backupType, String triggerType) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath), StandardCharsets.UTF_8)) {
            // 1. backup.sql
            addToZip(zos, ZIP_ENTRY_SQL, sqlPath);
            // 2. application.yml.masked
            addToZip(zos, ZIP_ENTRY_CONFIG, configPath);
            // 3. META.txt
            String meta = buildMetaContent(record, backupType, triggerType);
            ZipEntry metaEntry = new ZipEntry(ZIP_ENTRY_META);
            zos.putNextEntry(metaEntry);
            zos.write(meta.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return Files.size(zipPath);
    }

    private void addToZip(ZipOutputStream zos, String entryName, Path filePath) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        Files.copy(filePath, zos);
        zos.closeEntry();
    }

    private String buildMetaContent(BackupRecord record, String backupType, String triggerType) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 代账系统数据库备份元信息\n");
        sb.append("backupType=").append(backupType).append('\n');
        sb.append("triggerType=").append(triggerType).append('\n');
        sb.append("createdAt=").append(LocalDateTime.now()).append('\n');
        sb.append("createdBy=").append(record.getCreatedByName()).append('\n');
        sb.append("remark=").append(record.getRemark() == null ? "" : record.getRemark()).append('\n');
        sb.append("database=h2\n");
        sb.append("formatVersion=1\n");
        return sb.toString();
    }

    /**
     * 从 ZIP 中解压 backup.sql 到临时文件,返回临时文件路径。
     * 找不到则返回 null。
     */
    private Path extractSqlFromZip(Path zipPath) throws IOException {
        Path tempDir = Files.createTempDirectory("backup-restore-");
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (ZIP_ENTRY_SQL.equals(entry.getName())) {
                    Path sqlFile = tempDir.resolve("restore.sql");
                    Files.copy(zis, sqlFile, StandardCopyOption.REPLACE_EXISTING);
                    return sqlFile;
                }
                zis.closeEntry();
            }
        }
        // 没找到 SQL,清理临时目录
        safeDelete(tempDir);
        return null;
    }

    private void markFailed(BackupRecord record, String reason) {
        record.setStatus("failed");
        String existingRemark = record.getRemark();
        record.setRemark(StrUtil.isBlank(existingRemark) ? reason : existingRemark + " | " + reason);
        try {
            updateById(record);
        } catch (Exception e) {
            log.error("更新备份记录为 failed 状态失败,recordId={}", record.getId(), e);
        }
    }

    private void safeDelete(Path path) {
        if (path == null) {
            return;
        }
        try {
            if (Files.isRegularFile(path)) {
                Files.deleteIfExists(path);
            } else if (Files.isDirectory(path)) {
                // 简单删除目录(仅当为空时)
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.debug("删除临时文件/目录失败(可忽略): {}", path, e);
        }
    }

    private BackupRecordVO toVO(BackupRecord record) {
        BackupRecordVO vo = new BackupRecordVO();
        vo.setId(record.getId());
        vo.setFileName(record.getFileName());
        vo.setFileSize(record.getFileSize());
        vo.setCreatedAt(record.getCreatedTime());
        vo.setCreatedBy(record.getCreatedBy());
        vo.setCreatedByName(record.getCreatedByName());
        vo.setType(record.getTriggerType());
        vo.setStatus(record.getStatus());
        vo.setBackupType(record.getBackupType());
        vo.setRemark(record.getRemark());
        return vo;
    }
}
