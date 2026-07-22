package com.company.daizhang.module.tax.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.tax.dto.TaxRiskWarningRequest;
import com.company.daizhang.module.tax.service.TaxRiskWarningService;
import com.company.daizhang.module.tax.vo.TaxRiskWarningVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 税务风险预警控制器
 */
@Tag(name = "税务风险预警")
@RestController
@RequestMapping("/tax/risk-warning")
@RequiredArgsConstructor
public class TaxRiskWarningController {

    private final TaxRiskWarningService taxRiskWarningService;

    @Operation(summary = "分页查询风险预警")
    @GetMapping("/page")
    @RequireAccountSetAccess
    public Result<PageResult<TaxRiskWarningVO>> pageWarnings(
            @RequestParam Long accountSetId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer riskLevel,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<TaxRiskWarningVO> page = taxRiskWarningService.pageWarnings(
                accountSetId, year, month, riskLevel, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "创建风险预警")
    @PostMapping
    @RequireAccountSetAccess(RequireAccountSetAccess.AccessLevel.OWNER)
    public Result<Void> create(@Valid @RequestBody TaxRiskWarningRequest request) {
        taxRiskWarningService.createWarning(request);
        return Result.success();
    }

    @Operation(summary = "处理风险预警")
    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestParam(required = false) String handleRemark) {
        taxRiskWarningService.handleWarning(id, handleRemark);
        return Result.success();
    }

    @Operation(summary = "忽略风险预警")
    @PostMapping("/{id}/ignore")
    public Result<Void> ignore(@PathVariable Long id) {
        taxRiskWarningService.ignoreWarning(id);
        return Result.success();
    }

    @Operation(summary = "扫描生成风险预警")
    @PostMapping("/scan")
    @RequireAccountSetAccess
    public Result<Void> scan(
            @RequestParam Long accountSetId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        taxRiskWarningService.scanRiskWarnings(accountSetId, year, month);
        return Result.success();
    }
}
