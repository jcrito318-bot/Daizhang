package com.company.daizhang.module.report.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.report.service.CashFlowStatementService;
import com.company.daizhang.module.report.vo.CashFlowStatementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 现金流量表控制器（直接法）
 * <p>
 * 独立于 ReportController，提供基于《企业会计准则第31号—现金流量表》的
 * 23项标准现金流量表查询接口。
 */
@Tag(name = "财务报表-现金流量表")
@RestController
@RequestMapping("/report/cash-flow")
@RequiredArgsConstructor
public class CashFlowStatementController {

    private final CashFlowStatementService cashFlowStatementService;

    @Operation(summary = "现金流量表（直接法）")
    @GetMapping
    @RequireAccountSetAccess
    public Result<CashFlowStatementVO> cashFlowStatement(@RequestParam Long accountSetId,
                                                         @RequestParam Integer year,
                                                         @RequestParam Integer month) {
        CashFlowStatementVO result = cashFlowStatementService.generateCashFlowStatement(accountSetId, year, month);
        return Result.success(result);
    }
}
