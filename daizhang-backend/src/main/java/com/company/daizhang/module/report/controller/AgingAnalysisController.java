package com.company.daizhang.module.report.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.report.service.AgingAnalysisService;
import com.company.daizhang.module.report.vo.AgingAnalysisVO;
import com.company.daizhang.module.report.vo.AgingItemVO;
import com.company.daizhang.module.report.vo.AgingSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 账龄分析控制器
 * <p>
 * 提供应收/应付账龄分析,代账给客户出经营建议时常用。
 * 所有端点均通过 {@code @RequireAccountSetAccess} 校验账套访问权(IDOR 治理)。
 */
@Tag(name = "账龄分析")
@RestController
@RequestMapping("/report/aging")
@RequiredArgsConstructor
public class AgingAnalysisController {

    private final AgingAnalysisService agingAnalysisService;

    @Operation(summary = "应收账龄分析(按客户维度)")
    @GetMapping("/receivable")
    @RequireAccountSetAccess
    public Result<List<AgingItemVO>> receivableAging(
            @Parameter(description = "账套ID", required = true)
            @RequestParam Long accountSetId,
            @Parameter(description = "截止日期(yyyy-MM-dd,默认本月最后一天)")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate asOfDate) {
        List<AgingItemVO> result = agingAnalysisService.receivableAging(accountSetId, asOfDate);
        return Result.success(result);
    }

    @Operation(summary = "应付账龄分析(按供应商维度)")
    @GetMapping("/payable")
    @RequireAccountSetAccess
    public Result<List<AgingItemVO>> payableAging(
            @Parameter(description = "账套ID", required = true)
            @RequestParam Long accountSetId,
            @Parameter(description = "截止日期(yyyy-MM-dd,默认本月最后一天)")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate asOfDate) {
        List<AgingItemVO> result = agingAnalysisService.payableAging(accountSetId, asOfDate);
        return Result.success(result);
    }

    @Operation(summary = "账龄分析汇总(应收/应付总额及逾期金额)")
    @GetMapping("/summary")
    @RequireAccountSetAccess
    public Result<AgingSummaryVO> agingSummary(
            @Parameter(description = "账套ID", required = true)
            @RequestParam Long accountSetId,
            @Parameter(description = "截止日期(yyyy-MM-dd,默认本月最后一天)")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate asOfDate) {
        AgingSummaryVO result = agingAnalysisService.agingSummary(accountSetId, asOfDate);
        return Result.success(result);
    }

    @Operation(summary = "完整账龄分析(应收+应付+汇总)")
    @GetMapping
    @RequireAccountSetAccess
    public Result<AgingAnalysisVO> agingAnalysis(
            @Parameter(description = "账套ID", required = true)
            @RequestParam Long accountSetId,
            @Parameter(description = "截止日期(yyyy-MM-dd,默认本月最后一天)")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate asOfDate) {
        AgingAnalysisVO result = agingAnalysisService.agingAnalysis(accountSetId, asOfDate);
        return Result.success(result);
    }
}
