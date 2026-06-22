package com.company.daizhang.module.report.service;

import com.company.daizhang.module.report.dto.ReportQueryRequest;
import com.company.daizhang.module.report.vo.BalanceSheetVO;
import com.company.daizhang.module.report.vo.IncomeStatementVO;
import com.company.daizhang.module.report.vo.SubjectBalanceTableVO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 财务报表服务
 */
public interface ReportService {

    /**
     * 资产负债表
     */
    BalanceSheetVO balanceSheet(ReportQueryRequest request);

    /**
     * 利润表
     */
    IncomeStatementVO incomeStatement(ReportQueryRequest request);

    /**
     * 科目余额表
     */
    SubjectBalanceTableVO subjectBalanceTable(ReportQueryRequest request);

    /**
     * 导出资产负债表Excel
     */
    void exportBalanceSheet(ReportQueryRequest request, HttpServletResponse response);

    /**
     * 导出利润表Excel
     */
    void exportIncomeStatement(ReportQueryRequest request, HttpServletResponse response);

    /**
     * 导出科目余额表Excel
     */
    void exportSubjectBalanceTable(ReportQueryRequest request, HttpServletResponse response);
}
