package com.company.daizhang.module.voucher.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
    @DecimalMin(value = "0", message = "借方金额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "金额精度超出范围(整数最多15位,小数最多2位)")
    private BigDecimal debit;

    @NotNull(message = "贷方金额不能为空")
    @DecimalMin(value = "0", message = "贷方金额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "金额精度超出范围(整数最多15位,小数最多2位)")
    private BigDecimal credit;

    @DecimalMin(value = "0", message = "数量不能为负数")
    @Digits(integer = 15, fraction = 4, message = "数量精度超出范围")
    private BigDecimal quantity;

    @DecimalMin(value = "0", message = "单价不能为负数")
    @Digits(integer = 15, fraction = 2, message = "金额精度超出范围(整数最多15位,小数最多2位)")
    private BigDecimal unitPrice;
}
