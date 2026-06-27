package com.company.daizhang.module.salary.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 薪资凭证生成请求
 */
@Data
public class SalaryVoucherGenerateRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "年度不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;

    /**
     * 应付职工薪酬科目ID
     */
    @NotNull(message = "应付职工薪酬科目ID不能为空")
    private Long payableSubjectId;

    /**
     * 费用科目ID(如管理费用5602),用于计提工资的借方。
     * 若为空则自动查找编码5602科目作为借方费用科目。
     */
    private Long expenseSubjectId;

    /**
     * 银行存款科目ID
     */
    @NotNull(message = "银行存款科目ID不能为空")
    private Long bankSubjectId;

    /**
     * 社保科目ID
     */
    private Long socialSecuritySubjectId;

    /**
     * 公积金科目ID
     */
    private Long housingFundSubjectId;

    /**
     * 个税科目ID
     */
    private Long incomeTaxSubjectId;
}
