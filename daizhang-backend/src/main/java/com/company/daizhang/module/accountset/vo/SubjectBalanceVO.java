package com.company.daizhang.module.accountset.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 科目期初余额视图对象
 */
@Data
public class SubjectBalanceVO {

    private Long id;

    private Long accountSetId;

    private Long subjectId;

    private String subjectCode;

    private String subjectName;

    private Integer year;

    private Integer period;

    private BigDecimal beginDebit;

    private BigDecimal beginCredit;

    private Long auxiliaryId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 试算平衡汇总信息：借方合计
     */
    private BigDecimal totalDebit;

    /**
     * 试算平衡汇总信息：贷方合计
     */
    private BigDecimal totalCredit;

    /**
     * 试算平衡汇总信息：是否平衡
     */
    private Boolean balanced;
}
