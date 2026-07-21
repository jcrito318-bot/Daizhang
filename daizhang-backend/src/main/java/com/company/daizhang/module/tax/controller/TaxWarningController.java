package com.company.daizhang.module.tax.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.tax.dto.TaxBenchmarkUpdateRequest;
import com.company.daizhang.module.tax.service.TaxWarningService;
import com.company.daizhang.module.tax.vo.TaxBenchmarkVO;
import com.company.daizhang.module.tax.vo.TaxTrendVO;
import com.company.daizhang.module.tax.vo.TaxWarningVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 税负预警控制器
 * <p>
 * 单账套税负率异常预警(增值税税负率偏低/偏高),帮助代账会计提前给客户预警
 */
@Slf4j
@Tag(name = "税负预警")
@RestController
@RequestMapping("/tax")
@RequiredArgsConstructor
public class TaxWarningController {

    private final TaxWarningService taxWarningService;

    @Operation(summary = "当月税负预警")
    @GetMapping("/warning")
    @RequireAccountSetAccess
    public Result<TaxWarningVO> getWarning(@RequestParam Long accountSetId,
                                           @RequestParam Integer year,
                                           @RequestParam Integer month) {
        TaxWarningVO vo = taxWarningService.getWarning(accountSetId, year, month);
        return Result.success(vo);
    }

    @Operation(summary = "全年税负趋势")
    @GetMapping("/trend")
    @RequireAccountSetAccess
    public Result<List<TaxTrendVO>> getTrend(@RequestParam Long accountSetId,
                                             @RequestParam Integer year) {
        List<TaxTrendVO> list = taxWarningService.getTrend(accountSetId, year);
        return Result.success(list);
    }

    @Operation(summary = "行业税负率基准列表")
    @GetMapping("/benchmarks")
    public Result<List<TaxBenchmarkVO>> listBenchmarks() {
        List<TaxBenchmarkVO> list = taxWarningService.listBenchmarks();
        return Result.success(list);
    }

    @Operation(summary = "更新行业税负率基准(ADMIN only)")
    @PutMapping("/benchmarks/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateBenchmark(@PathVariable Long id,
                                        @Valid @RequestBody TaxBenchmarkUpdateRequest request) {
        taxWarningService.updateBenchmark(id, request);
        return Result.success();
    }
}
