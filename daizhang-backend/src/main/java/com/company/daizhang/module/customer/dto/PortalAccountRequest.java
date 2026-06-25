package com.company.daizhang.module.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 客户看账门户请求
 */
@Data
public class PortalAccountRequest {

    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotBlank(message = "门户用户名不能为空")
    private String portalUsername;

    /**
     * 门户密码(明文，服务端加密)
     */
    private String portalPassword;

    /**
     * 到期日期
     */
    private LocalDate expireDate;

    /**
     * 状态(0-禁用 1-正常)
     */
    private Integer status;
}
