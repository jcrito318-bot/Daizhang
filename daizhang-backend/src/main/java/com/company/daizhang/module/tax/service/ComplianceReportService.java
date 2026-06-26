package com.company.daizhang.module.tax.service;

import com.company.daizhang.module.tax.vo.ComplianceReportVO;

/**
 * 财税合规评估服务
 */
public interface ComplianceReportService {

    /**
     * 生成单账套财税合规评估报告
     *
     * @param accountSetId 账套ID
     * @param year         年度
     * @param month        月份
     * @return 合规评估报告
     */
    ComplianceReportVO generateComplianceReport(Long accountSetId, Integer year, Integer month);
}
