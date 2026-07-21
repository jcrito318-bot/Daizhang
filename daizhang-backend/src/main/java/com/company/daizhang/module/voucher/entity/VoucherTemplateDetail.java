package com.company.daizhang.module.voucher.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 凭证模板明细 POJO
 * <p>
 * 不映射数据库表,作为 {@link VoucherTemplate#getDetailJson()} 字段的 JSON
 * 序列化/反序列化目标类型。由 Service 层使用 Jackson 与 JSON 字符串互转。
 * <p>
 * JSON 字段对应: subjectCode / subjectName / debitAmount / creditAmount / summary
 */
@Data
public class VoucherTemplateDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 科目编码
     */
    private String subjectCode;

    /**
     * 科目名称
     */
    private String subjectName;

    /**
     * 借方金额
     */
    private BigDecimal debitAmount;

    /**
     * 贷方金额
     */
    private BigDecimal creditAmount;

    /**
     * 摘要
     */
    private String summary;
}
