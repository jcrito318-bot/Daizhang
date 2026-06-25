package com.company.daizhang.module.voucher.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 凭证模板明细实体
 */
@Data
@TableName("acc_voucher_template_detail")
public class VoucherTemplateDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 行号
     */
    private Integer lineNo;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 科目ID
     */
    private Long subjectId;

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
    private BigDecimal debit;

    /**
     * 贷方金额
     */
    private BigDecimal credit;

    /**
     * 排序号
     */
    private Integer sortOrder;
}
