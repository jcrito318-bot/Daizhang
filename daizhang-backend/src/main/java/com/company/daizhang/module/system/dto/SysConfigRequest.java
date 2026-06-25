package com.company.daizhang.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 系统设置请求
 */
@Data
public class SysConfigRequest {

    @NotBlank(message = "参数键不能为空")
    private String configKey;

    private String configValue;

    private String configName;

    private String configType;

    private String remark;
}
