package com.company.daizhang.module.voucher.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 凭证明细视图对象
 */
@Data
public class VoucherDetailVO {

    private Long id;

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
