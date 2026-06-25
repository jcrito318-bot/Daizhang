package com.company.daizhang.module.period.controller;

import com.company.daizhang.common.annotation.OperationLog;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.period.dto.TrialBalanceRequest;
import com.company.daizhang.module.period.service.PeriodService;
import com.company.daizhang.module.period.vo.ClosePeriodResultVO;
import com.company.daizhang.module.period.vo.TrialBalanceResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 期末处理控制器
 */
@Tag(name = "期末处理")
@RestController
@RequestMapping("/period")
@RequiredArgsConstructor
public class PeriodController {

    private final PeriodService periodService;

    @Operation(summary = "试算平衡")
    @GetMapping("/trial-balance")
    public Result<TrialBalanceResultVO> trialBalance(@Valid TrialBalanceRequest request) {
        TrialBalanceResultVO result = periodService.trialBalance(request);
        return Result.success(result);
    }

    @Operation(summary = "结账")
    @PostMapping("/close")
    @OperationLog("期末结账")
    public Result<ClosePeriodResultVO> close(@RequestParam Long accountSetId,
                                              @RequestParam int year,
                                              @RequestParam int month) {
        ClosePeriodResultVO result = periodService.closePeriod(accountSetId, year, month);
        return Result.success(result);
    }

    @Operation(summary = "反结账")
    @PostMapping("/reopen")
    @OperationLog("期末反结账")
    public Result<Void> reopen(@RequestParam Long accountSetId,
                               @RequestParam int year,
                               @RequestParam int month) {
        periodService.reopenPeriod(accountSetId, year, month);
        return Result.success();
    }

    @Operation(summary = "月末结转损益")
    @PostMapping("/carry-forward")
    @OperationLog("损益结转")
    public Result<Void> carryForward(@RequestParam Long accountSetId,
                                     @RequestParam int year,
                                     @RequestParam int month) {
        periodService.carryForward(accountSetId, year, month);
        return Result.success();
    }

    @Operation(summary = "年度结转")
    @PostMapping("/carry-forward-year")
    @OperationLog("年度结转")
    public Result<Void> carryForwardYear(@RequestParam Long accountSetId,
                                         @RequestParam Integer fromYear) {
        periodService.carryForwardYear(accountSetId, fromYear);
        return Result.success();
    }
}
