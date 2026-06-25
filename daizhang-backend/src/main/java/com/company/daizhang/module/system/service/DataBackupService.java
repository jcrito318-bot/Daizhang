package com.company.daizhang.module.system.service;

import java.util.List;
import java.util.Map;

/**
 * 数据备份服务接口
 */
public interface DataBackupService {

    /**
     * 备份数据库
     *
     * @return 备份文件名
     */
    String backup();

    /**
     * 列出所有备份文件
     */
    List<Map<String, Object>> listBackups();

    /**
     * 恢复数据库（需要重启应用）
     *
     * @param fileName 备份文件名
     */
    void restore(String fileName);

    /**
     * 删除备份文件
     *
     * @param fileName 备份文件名
     */
    void deleteBackup(String fileName);
}
