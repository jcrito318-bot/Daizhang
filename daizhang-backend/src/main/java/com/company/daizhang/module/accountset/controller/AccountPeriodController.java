package com.company.daizhang.module.accountset.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.accountset.service.AccountPeriodService;
import com.company.daizhang.module.accountset.vo.AccountPeriodVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会计期间控制器
 */
@Tag(name = "会计期间管理")
@RestController
@RequestMapping("/accountset/period")
@RequiredArgsConstructor
public class AccountPeriodController {
    
    private final AccountPeriodService accountPeriodService;
    
    @Operation(summary = "查询会计期间列表")
    @GetMapping("/list")
    @RequireAccountSetAccess
    public Result<List<AccountPeriodVO>> list(@RequestParam Long accountSetId) {
        List<AccountPeriodVO> periods = accountPeriodService.listPeriods(accountSetId);
        return Result.success(periods);
    }
    
    @Operation(summary = "初始化会计期间")
    @PostMapping("/init")
    @RequireAccountSetAccess
    public Result<Void> init(@RequestParam Long accountSetId, @RequestParam int year) {
        accountPeriodService.createPeriod(accountSetId, year);
        return Result.success();
    }
    
    @Operation(summary = "结账")
    @PostMapping("/{accountSetId}/close")
    @RequireAccountSetAccess
    public Result<Void> close(@PathVariable Long accountSetId,
                              @RequestParam int year,
                              @RequestParam int month) {
        accountPeriodService.closePeriod(accountSetId, year, month);
        return Result.success();
    }
    
    @Operation(summary = "反结账")
    @PostMapping("/{accountSetId}/reopen")
    @RequireAccountSetAccess
    public Result<Void> reopen(@PathVariable Long accountSetId,
                               @RequestParam int year,
                               @RequestParam int month) {
        accountPeriodService.reopenPeriod(accountSetId, year, month);
        return Result.success();
    }
}
