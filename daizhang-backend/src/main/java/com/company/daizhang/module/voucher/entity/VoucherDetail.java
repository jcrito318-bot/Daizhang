package com.company.daizhang.module.voucher.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 凭证明细实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acc_voucher_detail")
public class VoucherDetail extends BaseEntity {

    private Long voucherId;

    private Integer lineNo;

    private String summary;

    private Long subjectId;

    private String subjectCode;

    private String subjectName;

    private Long auxiliaryId;

    private BigDecimal debit;

    private BigDecimal credit;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private Integer sortOrder;
}
