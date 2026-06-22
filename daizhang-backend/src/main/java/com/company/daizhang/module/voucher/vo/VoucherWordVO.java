package com.company.daizhang.module.voucher.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 凭证字视图对象
 */
@Data
public class VoucherWordVO {

    private Long id;

    private Long accountSetId;

    private String name;

    private String code;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createTime;
}
