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

    /**
     * 恢复进行中标志位：恢复期间置为 true，恢复结束后置为 false。
     * 其他写操作 Service 可通过 {@link #isRestoring()} 判断是否应拒绝写入，
     * 避免恢复过程中数据被覆盖或数据库文件被占用导致恢复失败。
     */
    private static volatile boolean restoring = false;

    /**
     * 恢复操作同步锁：保证同一时刻只有一个恢复操作在执行，避免并发恢复冲突。
     */
    private static final Object RESTORE_LOCK = new Object();

    /**
     * 判断系统是否正在恢复数据库。
     * 恢复期间应对数据库的写操作予以拒绝，防止恢复的数据被覆盖或数据库文件被占用。
     *
     * @return true 表示正在恢复数据库，此时应避免对数据库进行写操作
     */
    public static boolean isRestoring() {
        return restoring;
    }

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
        // 加锁保证同一时刻只有一个恢复操作执行，避免并发恢复导致数据库文件被并发覆盖。
        // 恢复期间通过 restoring 标志位阻断应用对数据库的写入，防止恢复的数据被覆盖
        // 或数据库文件被占用导致恢复失败。其他写操作 Service 可通过 isRestoring() 判断。
        log.info("开始数据库恢复，备份文件: {}", fileName);
        synchronized (RESTORE_LOCK) {
            restoring = true;
            try {
                Path backupPath = resolveSafeBackupPath(fileName);
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
            } finally {
                restoring = false;
            }
        }
    }

    @Override
    public void deleteBackup(String fileName) {
        Path backupPath = resolveSafeBackupPath(fileName);
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

    /**
     * 安全解析备份文件路径，防止路径穿越攻击（如 fileName 含 ../ 跳出备份目录）
     */
    private Path resolveSafeBackupPath(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名不能为空");
        }
        // 拒绝包含路径分隔符或父目录引用的文件名
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法的文件名");
        }
        Path backupDir = Paths.get(BACKUP_DIR).toAbsolutePath().normalize();
        Path backupPath = Paths.get(BACKUP_DIR, fileName).toAbsolutePath().normalize();
        // 二次校验：解析后路径必须仍位于备份目录内
        if (!backupPath.startsWith(backupDir)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法的文件名");
        }
        return backupPath;
    }
}
