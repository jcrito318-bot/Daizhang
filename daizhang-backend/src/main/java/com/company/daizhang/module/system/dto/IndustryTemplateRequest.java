package com.company.daizhang.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 行业模板请求
 */
@Data
public class IndustryTemplateRequest {

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotBlank(message = "行业类型不能为空")
    private String industryType;

    @NotBlank(message = "会计准则不能为空")
    private String accountingStandard;

    /**
     * 描述
     */
    private String description;

    /**
     * 科目配置(JSON)
     */
    private String subjectConfig;

    /**
     * 状态(0-禁用 1-启用)
     */
    private Integer status;
}
