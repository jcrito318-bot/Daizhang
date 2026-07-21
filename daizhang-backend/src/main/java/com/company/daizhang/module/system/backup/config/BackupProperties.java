package com.company.daizhang.module.system.backup.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 备份配置属性 (P3.3)
 * <p>
 * 对应 application.yml 中 {@code app.backup.*} 配置项。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.backup")
public class BackupProperties {

    /**
     * 备份文件存储目录
     */
    private String directory = "./backups";

    /**
     * 是否启用定时自动备份
     */
    private boolean autoEnabled = true;

    /**
     * 定时备份 cron 表达式(默认每日凌晨 2 点)
     */
    private String cron = "0 0 2 * * ?";

    /**
     * 最多保留的备份份数(超出则清理最旧的)
     */
    private int maxKeep = 30;
}
