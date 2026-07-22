package com.company.daizhang.module.report.service;

import com.company.daizhang.module.report.dto.ReportQueryRequest;
import com.company.daizhang.module.report.vo.BalanceSheetVO;
import com.company.daizhang.module.report.vo.CashFlowAdjustmentVO;
import com.company.daizhang.module.report.vo.CashFlowStatementVO;
import com.company.daizhang.module.report.vo.CustomerBriefingVO;
import com.company.daizhang.module.report.vo.DepartmentExpenseReportVO;
import com.company.daizhang.module.report.vo.EquityChangeStatementVO;
import com.company.daizhang.module.report.vo.IncomeStatementVO;
import com.company.daizhang.module.report.vo.MultiYearComparisonVO;
import com.company.daizhang.module.report.vo.SubjectBalanceTableVO;
import com.company.daizhang.module.report.vo.YearOnYearVO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

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
     * 现金流量表
     */
    CashFlowStatementVO cashFlowStatement(Long accountSetId, Integer year, Integer month);

    /**
     * 导出现金流量表Excel
     */
    void exportCashFlowStatement(Long accountSetId, Integer year, Integer month, HttpServletResponse response);

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

    /**
     * 查询现金流量表调整项列表
     */
    List<CashFlowAdjustmentVO> listAdjustments(Long accountSetId, Integer year, Integer month);

    /**
     * 保存现金流量表调整项
     */
    void saveAdjustment(CashFlowAdjustmentVO request);

    /**
     * 删除现金流量表调整项
     */
    void deleteAdjustment(Long id);

    /**
     * 带调整的现金流量表
     */
    CashFlowStatementVO cashFlowStatementWithAdjustment(Long accountSetId, Integer year, Integer month);

    /**
     * 同比环比分析
     */
    List<YearOnYearVO> yearOnYearAnalysis(Long accountSetId, Integer year, Integer month);

    /**
     * 生成打印HTML
     *
     * @param reportType balance-sheet/income-statement/cash-flow-statement/subject-balance
     */
    String generatePrintHtml(Long accountSetId, Integer year, Integer month, String reportType);

    /**
     * 导出资产负债表PDF
     */
    void exportBalanceSheetPdf(Long accountSetId, Integer year, Integer month, HttpServletResponse response);

    /**
     * 导出利润表PDF
     */
    void exportIncomeStatementPdf(Long accountSetId, Integer year, Integer month, HttpServletResponse response);

    /**
     * 导出现金流量表PDF
     */
    void exportCashFlowStatementPdf(Long accountSetId, Integer year, Integer month, HttpServletResponse response);

    /**
     * 导出科目余额表PDF
     */
    void exportSubjectBalanceTablePdf(Long accountSetId, Integer year, Integer month, HttpServletResponse response);

    /**
     * 所有者权益变动表
     */
    EquityChangeStatementVO equityChangeStatement(Long accountSetId, Integer year, Integer month);

    /**
     * 部门费用分析报表
     * 按部门辅助核算归集费用类科目（损益类借方）发生额
     */
    DepartmentExpenseReportVO departmentExpenseReport(Long accountSetId, Integer year, Integer month);

    /**
     * 导出所有者权益变动表Excel
     */
    void exportEquityChangeStatement(ReportQueryRequest request, HttpServletResponse response);

    /**
     * 导出所有者权益变动表PDF
     */
    void exportEquityChangeStatementPdf(ReportQueryRequest request, HttpServletResponse response);

    /**
     * 导出部门费用分析表Excel
     */
    void exportDepartmentExpense(ReportQueryRequest request, HttpServletResponse response);

    /**
     * 导出部门费用分析表PDF
     */
    void exportDepartmentExpensePdf(ReportQueryRequest request, HttpServletResponse response);

    /**
     * 客户经营简报(B5)
     * 汇总单账套单月的关键经营指标
     */
    CustomerBriefingVO customerBriefing(Long accountSetId, Integer year, Integer month);

    /**
     * 导出客户经营简报Excel(B5)
     */
    void exportCustomerBriefing(Long accountSetId, Integer year, Integer month, HttpServletResponse response);

    /**
     * 多年度对比分析(B6)
     */
    MultiYearComparisonVO multiYearComparison(Long accountSetId, Integer startYear, Integer endYear);
}
