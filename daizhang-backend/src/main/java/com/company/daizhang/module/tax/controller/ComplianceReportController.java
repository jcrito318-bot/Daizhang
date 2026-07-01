package com.company.daizhang.module.tax.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.tax.service.ComplianceReportService;
import com.company.daizhang.module.tax.vo.ComplianceReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 财税合规评估控制器
 */
@Tag(name = "财税合规评估")
@RestController
@RequestMapping("/tax/compliance")
@RequiredArgsConstructor
public class ComplianceReportController {

    private final ComplianceReportService complianceReportService;

    @Operation(summary = "生成财税合规评估报告（5大维度/30+指标）")
    @GetMapping("/report")
    @RequireAccountSetAccess
    public Result<ComplianceReportVO> generateReport(@RequestParam Long accountSetId,
                                                      @RequestParam Integer year,
                                                      @RequestParam Integer month) {
        ComplianceReportVO report = complianceReportService.generateComplianceReport(accountSetId, year, month);
        return Result.success(report);
    }
}
