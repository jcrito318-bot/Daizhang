package com.company.daizhang.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统设置视图对象
 */
@Data
public class SysConfigVO {

    private Long id;

    private String configKey;

    private String configValue;

    private String configName;

    private String configType;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
