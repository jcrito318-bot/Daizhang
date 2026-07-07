package com.company.daizhang.module.report.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.report.dto.ReportQueryRequest;
import com.company.daizhang.module.report.service.ReportService;
import com.company.daizhang.module.report.vo.BalanceSheetVO;
import com.company.daizhang.module.report.vo.CashFlowAdjustmentVO;
import com.company.daizhang.module.report.vo.CashFlowStatementVO;
import com.company.daizhang.module.report.vo.DepartmentExpenseReportVO;
import com.company.daizhang.module.report.vo.EquityChangeStatementVO;
import com.company.daizhang.module.report.vo.IncomeStatementVO;
import com.company.daizhang.module.report.vo.SubjectBalanceTableVO;
import com.company.daizhang.module.report.vo.YearOnYearVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 财务报表控制器
 */
@Tag(name = "财务报表")
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "资产负债表")
    @GetMapping("/balance-sheet")
    public Result<BalanceSheetVO> balanceSheet(ReportQueryRequest request) {
        BalanceSheetVO result = reportService.balanceSheet(request);
        return Result.success(result);
    }

    @Operation(summary = "利润表")
    @GetMapping("/income-statement")
    public Result<IncomeStatementVO> incomeStatement(ReportQueryRequest request) {
        IncomeStatementVO result = reportService.incomeStatement(request);
        return Result.success(result);
    }

    @Operation(summary = "科目余额表")
    @GetMapping("/subject-balance-table")
    public Result<SubjectBalanceTableVO> subjectBalanceTable(ReportQueryRequest request) {
        SubjectBalanceTableVO result = reportService.subjectBalanceTable(request);
        return Result.success(result);
    }

    @Operation(summary = "现金流量表")
    @GetMapping("/cash-flow-statement")
    @RequireAccountSetAccess
    public Result<CashFlowStatementVO> cashFlowStatement(@RequestParam Long accountSetId,
                                                         @RequestParam Integer year,
                                                         @RequestParam Integer month) {
        CashFlowStatementVO result = reportService.cashFlowStatement(accountSetId, year, month);
        return Result.success(result);
    }

    @Operation(summary = "导出现金流量表Excel")
    @GetMapping("/cash-flow-statement/export")
    @RequireAccountSetAccess
    public void exportCashFlowStatement(@RequestParam Long accountSetId,
                                         @RequestParam Integer year,
                                         @RequestParam Integer month,
                                         HttpServletResponse response) {
        reportService.exportCashFlowStatement(accountSetId, year, month, response);
    }

    @Operation(summary = "导出资产负债表Excel")
    @GetMapping("/balance-sheet/export")
    public void exportBalanceSheet(ReportQueryRequest request, HttpServletResponse response) {
        reportService.exportBalanceSheet(request, response);
    }

    @Operation(summary = "导出利润表Excel")
    @GetMapping("/income-statement/export")
    public void exportIncomeStatement(ReportQueryRequest request, HttpServletResponse response) {
        reportService.exportIncomeStatement(request, response);
    }

    @Operation(summary = "导出科目余额表Excel")
    @GetMapping("/subject-balance-table/export")
    public void exportSubjectBalanceTable(ReportQueryRequest request, HttpServletResponse response) {
        reportService.exportSubjectBalanceTable(request, response);
    }

    @Operation(summary = "导出资产负债表PDF")
    @GetMapping("/balance-sheet/pdf")
    @RequireAccountSetAccess
    public void exportBalanceSheetPdf(@RequestParam Long accountSetId,
                                      @RequestParam Integer year,
                                      @RequestParam Integer month,
                                      HttpServletResponse response) {
        reportService.exportBalanceSheetPdf(accountSetId, year, month, response);
    }

    @Operation(summary = "导出利润表PDF")
    @GetMapping("/income-statement/pdf")
    @RequireAccountSetAccess
    public void exportIncomeStatementPdf(@RequestParam Long accountSetId,
                                         @RequestParam Integer year,
                                         @RequestParam Integer month,
                                         HttpServletResponse response) {
        reportService.exportIncomeStatementPdf(accountSetId, year, month, response);
    }

    @Operation(summary = "导出现金流量表PDF")
    @GetMapping("/cash-flow-statement/pdf")
    @RequireAccountSetAccess
    public void exportCashFlowStatementPdf(@RequestParam Long accountSetId,
                                           @RequestParam Integer year,
                                           @RequestParam Integer month,
                                           HttpServletResponse response) {
        reportService.exportCashFlowStatementPdf(accountSetId, year, month, response);
    }

    @Operation(summary = "导出科目余额表PDF")
    @GetMapping("/subject-balance-table/pdf")
    @RequireAccountSetAccess
    public void exportSubjectBalanceTablePdf(@RequestParam Long accountSetId,
                                             @RequestParam Integer year,
                                             @RequestParam Integer month,
                                             HttpServletResponse response) {
        reportService.exportSubjectBalanceTablePdf(accountSetId, year, month, response);
    }

    @Operation(summary = "查询现金流量表调整项列表")
    @GetMapping("/cash-flow-adjustment/list")
    @RequireAccountSetAccess
    public Result<List<CashFlowAdjustmentVO>> listAdjustments(@RequestParam Long accountSetId,
                                                              @RequestParam Integer year,
                                                              @RequestParam Integer month) {
        List<CashFlowAdjustmentVO> list = reportService.listAdjustments(accountSetId, year, month);
        return Result.success(list);
    }

    @Operation(summary = "保存现金流量表调整项")
    @PostMapping("/cash-flow-adjustment")
    public Result<Void> saveAdjustment(@RequestBody CashFlowAdjustmentVO request) {
        reportService.saveAdjustment(request);
        return Result.success();
    }

    @Operation(summary = "删除现金流量表调整项")
    @DeleteMapping("/cash-flow-adjustment/{id}")
    public Result<Void> deleteAdjustment(@PathVariable Long id) {
        reportService.deleteAdjustment(id);
        return Result.success();
    }

    @Operation(summary = "带调整的现金流量表")
    @GetMapping("/cash-flow-statement/adjusted")
    @RequireAccountSetAccess
    public Result<CashFlowStatementVO> cashFlowStatementWithAdjustment(@RequestParam Long accountSetId,
                                                                       @RequestParam Integer year,
                                                                       @RequestParam Integer month) {
        CashFlowStatementVO result = reportService.cashFlowStatementWithAdjustment(accountSetId, year, month);
        return Result.success(result);
    }

    @Operation(summary = "同比环比分析")
    @GetMapping("/year-on-year")
    @RequireAccountSetAccess
    public Result<List<YearOnYearVO>> yearOnYearAnalysis(@RequestParam Long accountSetId,
                                                          @RequestParam Integer year,
                                                          @RequestParam Integer month) {
        List<YearOnYearVO> result = reportService.yearOnYearAnalysis(accountSetId, year, month);
        return Result.success(result);
    }

    @Operation(summary = "生成报表打印HTML")
    @GetMapping(value = "/print", produces = MediaType.TEXT_HTML_VALUE)
    @RequireAccountSetAccess
    public String generatePrintHtml(@RequestParam Long accountSetId,
                                    @RequestParam Integer year,
                                    @RequestParam Integer month,
                                    @RequestParam String reportType) {
        return reportService.generatePrintHtml(accountSetId, year, month, reportType);
    }

    @Operation(summary = "所有者权益变动表")
    @GetMapping("/equity-change-statement")
    @RequireAccountSetAccess
    public Result<EquityChangeStatementVO> equityChangeStatement(@RequestParam Long accountSetId,
                                                                   @RequestParam Integer year,
                                                                   @RequestParam Integer month) {
        EquityChangeStatementVO result = reportService.equityChangeStatement(accountSetId, year, month);
        return Result.success(result);
    }

    @Operation(summary = "部门费用分析报表（按部门辅助核算归集费用类科目发生额）")
    @GetMapping("/department-expense")
    @RequireAccountSetAccess
    public Result<DepartmentExpenseReportVO> departmentExpenseReport(@RequestParam Long accountSetId,
                                                                       @RequestParam Integer year,
                                                                       @RequestParam Integer month) {
        DepartmentExpenseReportVO result = reportService.departmentExpenseReport(accountSetId, year, month);
        return Result.success(result);
    }

    @Operation(summary = "导出所有者权益变动表Excel")
    @GetMapping("/equity-change-statement/export")
    public void exportEquityChangeStatement(ReportQueryRequest request, HttpServletResponse response) {
        reportService.exportEquityChangeStatement(request, response);
    }

    @Operation(summary = "导出所有者权益变动表PDF")
    @GetMapping("/equity-change-statement/pdf")
    public void exportEquityChangeStatementPdf(ReportQueryRequest request, HttpServletResponse response) {
        reportService.exportEquityChangeStatementPdf(request, response);
    }

    @Operation(summary = "导出部门费用分析表Excel")
    @GetMapping("/department-expense/export")
    public void exportDepartmentExpense(ReportQueryRequest request, HttpServletResponse response) {
        reportService.exportDepartmentExpense(request, response);
    }

    @Operation(summary = "导出部门费用分析表PDF")
    @GetMapping("/department-expense/pdf")
    public void exportDepartmentExpensePdf(ReportQueryRequest request, HttpServletResponse response) {
        reportService.exportDepartmentExpensePdf(request, response);
    }
}
