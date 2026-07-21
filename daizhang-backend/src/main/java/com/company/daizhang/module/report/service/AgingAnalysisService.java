package com.company.daizhang.module.report.service;

import com.company.daizhang.module.report.vo.AgingAnalysisVO;
import com.company.daizhang.module.report.vo.AgingItemVO;
import com.company.daizhang.module.report.vo.AgingSummaryVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 账龄分析服务
 * <p>
 * 用于代账给客户出经营建议时,统计应收账款(1122)和应付账款(2202)的账龄分布。
 * <p>
 * 计算口径:
 * <ul>
 *   <li>按客户/供应商维度,统计截至 asOfDate 的未核销余额(借方累计 - 贷方累计,应付反之)</li>
 *   <li>按凭证日期与 asOfDate 的天数差分桶: 0-30/31-60/61-90/91-180/180+</li>
 *   <li>优先用 auxiliary_id = customer.id 匹配凭证;若该客户无任何 auxiliary_id 关联,
 *       兜底使用 VoucherDetail.summary LIKE %客户名称% 匹配</li>
 * </ul>
 */
public interface AgingAnalysisService {

    /**
     * 查询完整的账龄分析结果(应收 + 应付 + 汇总)
     *
     * @param accountSetId 账套ID
     * @param asOfDate     截止日期(null 时默认取本月最后一天)
     * @return 账龄分析结果
     */
    AgingAnalysisVO agingAnalysis(Long accountSetId, LocalDate asOfDate);

    /**
     * 查询客户应收账龄列表
     *
     * @param accountSetId 账套ID
     * @param asOfDate     截止日期(null 时默认取本月最后一天)
     * @return 客户应收账龄列表(仅含有未核销余额的客户)
     */
    List<AgingItemVO> receivableAging(Long accountSetId, LocalDate asOfDate);

    /**
     * 查询供应商应付账龄列表
     *
     * @param accountSetId 账套ID
     * @param asOfDate     截止日期(null 时默认取本月最后一天)
     * @return 供应商应付账龄列表(仅含有未核销余额的供应商)
     */
    List<AgingItemVO> payableAging(Long accountSetId, LocalDate asOfDate);

    /**
     * 查询账龄分析汇总信息
     *
     * @param accountSetId 账套ID
     * @param asOfDate     截止日期(null 时默认取本月最后一天)
     * @return 汇总信息
     */
    AgingSummaryVO agingSummary(Long accountSetId, LocalDate asOfDate);
}
