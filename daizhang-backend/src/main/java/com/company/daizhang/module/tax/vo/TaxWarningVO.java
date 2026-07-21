package com.company.daizhang.module.tax.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 税负预警视图对象
 * <p>
 * 描述单个账套某月的增值税 / 企业所得税税负率,与行业基准对比后的预警等级与建议。
 */
@Data
public class TaxWarningVO {

    private Long accountSetId;

    private Integer year;

    private Integer month;

    /**
     * 账套所属行业代码(取自 AccountSet.industryType,未匹配时为 DEFAULT)
     */
    private String industryCode;

    /**
     * 账套所属行业名称
     */
    private String industryName;

    // ==================== 增值税 ====================

    /**
     * 增值税实际税负率(0.0300 = 3.00%,保留4位小数)
     */
    private BigDecimal vatActualRate;

    /**
     * 增值税行业基准税负率
     */
    private BigDecimal vatBenchmarkRate;

    /**
     * 增值税税负率预警下限
     */
    private BigDecimal vatWarningLow;

    /**
     * 增值税税负率预警上限
     */
    private BigDecimal vatWarningHigh;

    /**
     * 增值税税负率预警等级: normal/warning/danger
     */
    private String vatWarningLevel;

    // ==================== 企业所得税 ====================

    /**
     * 企业所得税实际税负率(保留4位小数)
     */
    private BigDecimal eitActualRate;

    /**
     * 企业所得税行业基准税负率
     */
    private BigDecimal eitBenchmarkRate;

    /**
     * 企业所得税税负率预警下限
     */
    private BigDecimal eitWarningLow;

    /**
     * 企业所得税税负率预警上限
     */
    private BigDecimal eitWarningHigh;

    /**
     * 企业所得税税负率预警等级: normal/warning/danger
     */
    private String eitWarningLevel;

    /**
     * 实际缴纳增值税(取自 tax_declaration.actual_amount)
     */
    private BigDecimal vatActualAmount;

    /**
     * 实际缴纳企业所得税(取自 tax_declaration.actual_amount)
     */
    private BigDecimal eitActualAmount;

    /**
     * 不含税销售收入(主营业务收入,科目 6001 贷方发生额)
     */
    private BigDecimal salesRevenue;

    /**
     * 建议(根据预警等级动态生成,给代账会计提前提醒客户)
     */
    private List<String> suggestions;

    /**
     * 预警明细(红色/黄色预警文字描述列表)
     */
    private List<String> warnings;
}
