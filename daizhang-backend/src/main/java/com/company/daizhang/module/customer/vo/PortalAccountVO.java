package com.company.daizhang.module.customer.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户看账门户视图对象
 */
@Data
public class PortalAccountVO {

    private Long id;

    private Long customerId;

    private Long accountSetId;

    private String portalUsername;

    /**
     * 到期日期
     */
    private LocalDate expireDate;

    /**
     * 状态(0-禁用 1-正常)
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
