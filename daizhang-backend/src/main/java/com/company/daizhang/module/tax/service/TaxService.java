package com.company.daizhang.module.tax.service;

import com.company.daizhang.module.tax.vo.TaxCheckResultVO;
import com.company.daizhang.module.tax.vo.TaxCheckSummaryVO;
import com.company.daizhang.module.tax.vo.TaxDeadlineReminderVO;
import com.company.daizhang.module.tax.vo.TaxDeclarationFormVO;

import java.util.List;

/**
 * 申报服务接口
 */
public interface TaxService {

    /**
     * 生成申报表
     *
     * @param accountSetId 账套ID
     * @param year         年度
     * @param month        月份
     * @param formType     申报表类型: VAT/Surcharge/IncomeTax/PersonalTax
     */
    TaxDeclarationFormVO generateDeclarationForm(Long accountSetId, Integer year, Integer month, String formType);

    /**
     * 导出申报表Excel
     *
     * @param accountSetId 账套ID
     * @param year         年度
     * @param month        月份
     * @param formType     申报表类型
     */
    byte[] exportDeclarationForm(Long accountSetId, Integer year, Integer month, String formType);

    /**
     * 获取所有账套的申报到期提醒
     */
    List<TaxDeadlineReminderVO> getDeadlineReminders();

    /**
     * 单账套税务检查（漏报/错报/状态异常）
     *
     * @param accountSetId 账套ID
     * @param year         年度
     * @param month        月份
     * @return 检查结果列表
     */
    List<TaxCheckResultVO> checkTaxDeclaration(Long accountSetId, Integer year, Integer month);

    /**
     * 全账套税务检查汇总（漏报/错报）
     *
     * @param year  年度
     * @param month 月份
     * @return 检查汇总
     */
    TaxCheckSummaryVO checkAllTaxDeclarations(Integer year, Integer month);
}
