package com.company.daizhang.module.voucher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 凭证明细请求
 */
@Data
public class VoucherDetailRequest {

    private Integer lineNo;

    @NotBlank(message = "摘要不能为空")
    private String summary;

    @NotNull(message = "科目ID不能为空")
    private Long subjectId;

    private Long auxiliaryId;

    private String subjectCode;

    private String subjectName;

    private Integer sortOrder;

    @NotNull(message = "借方金额不能为空")
    private BigDecimal debit;

    @NotNull(message = "贷方金额不能为空")
    private BigDecimal credit;

    private BigDecimal quantity;

    private BigDecimal unitPrice;
}
