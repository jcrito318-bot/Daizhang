package com.company.daizhang.module.document.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 票据按类型统计视图对象
 */
@Data
public class DocumentTypeStatVO {

    /**
     * 票据类型：1-发票 2-银行回单 3-费用单据 4-其他
     */
    private Integer documentType;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 数量
     */
    private Integer count;

    /**
     * 金额合计
     */
    private BigDecimal totalAmount;
}
