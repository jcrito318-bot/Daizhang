package com.company.daizhang.module.system.service.impl;

import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.system.service.DataBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 数据备份服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataBackupServiceImpl implements DataBackupService {

    private static final String DATA_DIR = "./data";
    private static final String BACKUP_DIR = "./data/backup";
    private static final String DATA_FILE = "daizhang.mv.db";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public String backup() {
        Path sourcePath = Paths.get(DATA_DIR, DATA_FILE);
        if (!Files.exists(sourcePath)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "数据库文件不存在");
        }

        // 确保备份目录存在
        Path backupDirPath = Paths.get(BACKUP_DIR);
        try {
            Files.createDirectories(backupDirPath);
        } catch (IOException e) {
            log.error("创建备份目录失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建备份目录失败");
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String backupFileName = "daizhang_backup_" + timestamp + ".mv.db";
        Path backupPath = backupDirPath.resolve(backupFileName);

        try {
            Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("数据库备份成功，备份文件: {}", backupFileName);
            return backupFileName;
        } catch (IOException e) {
            log.error("数据库备份失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "数据库备份失败");
        }
    }

    @Override
    public List<Map<String, Object>> listBackups() {
        Path backupDirPath = Paths.get(BACKUP_DIR);
        if (!Files.exists(backupDirPath)) {
            return new ArrayList<>();
        }

        try (Stream<Path> paths = Files.list(backupDirPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".mv.db"))
                    .map(p -> {
                        Map<String, Object> info = new HashMap<>();
                        info.put("fileName", p.getFileName().toString());
                        try {
                            info.put("size", Files.size(p));
                            info.put("lastModified", Files.getLastModifiedTime(p).toMillis());
                        } catch (IOException e) {
                            log.warn("读取备份文件信息失败: {}", p.getFileName(), e);
                        }
                        return info;
                    })
                    .sorted((a, b) -> {
                        Long ta = (Long) a.get("lastModified");
                        Long tb = (Long) b.get("lastModified");
                        if (ta == null) ta = 0L;
                        if (tb == null) tb = 0L;
                        return tb.compareTo(ta);
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("列出备份文件失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "列出备份文件失败");
        }
    }

    @Override
    public void restore(String fileName) {
        Path backupPath = Paths.get(BACKUP_DIR, fileName);
        if (!Files.exists(backupPath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "备份文件不存在");
        }

        Path targetPath = Paths.get(DATA_DIR, DATA_FILE);
        try {
            Files.copy(backupPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("数据库恢复成功，备份文件: {}，需要重启应用使恢复生效", fileName);
        } catch (IOException e) {
            log.error("数据库恢复失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "数据库恢复失败");
        }
    }

    @Override
    public void deleteBackup(String fileName) {
        Path backupPath = Paths.get(BACKUP_DIR, fileName);
        if (!Files.exists(backupPath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "备份文件不存在");
        }

        try {
            Files.delete(backupPath);
            log.info("删除备份文件成功: {}", fileName);
        } catch (IOException e) {
            log.error("删除备份文件失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "删除备份文件失败");
        }
    }
}
