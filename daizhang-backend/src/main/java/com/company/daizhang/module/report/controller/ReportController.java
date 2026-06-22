package com.company.daizhang.module.report.controller;

import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.report.dto.ReportQueryRequest;
import com.company.daizhang.module.report.service.ReportService;
import com.company.daizhang.module.report.vo.BalanceSheetVO;
import com.company.daizhang.module.report.vo.IncomeStatementVO;
import com.company.daizhang.module.report.vo.SubjectBalanceTableVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
