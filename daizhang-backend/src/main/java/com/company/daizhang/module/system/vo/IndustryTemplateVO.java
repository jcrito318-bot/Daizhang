package com.company.daizhang.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 行业模板视图对象
 */
@Data
public class IndustryTemplateVO {

    private Long id;

    private String templateCode;

    private String templateName;

    private String industryType;

    private String accountingStandard;

    private String description;

    private String subjectConfig;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
