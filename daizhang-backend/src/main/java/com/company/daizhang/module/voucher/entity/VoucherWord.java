package com.company.daizhang.module.voucher.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 凭证字实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acc_voucher_word")
public class VoucherWord extends BaseEntity {

    private Long accountSetId;

    private String name;

    private String code;

    private Integer sortOrder;

    private Integer status;
}
