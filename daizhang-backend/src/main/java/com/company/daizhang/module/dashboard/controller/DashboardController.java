package com.company.daizhang.module.dashboard.controller;

import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.dashboard.service.DashboardService;
import com.company.daizhang.module.dashboard.vo.DashboardStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 仪表盘控制器
 */
@Tag(name = "仪表盘")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    @Operation(summary = "获取统计数据")
    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats(@RequestParam Long accountSetId) {
        DashboardStatsVO stats = dashboardService.getStats(accountSetId);
        return Result.success(stats);
    }
}
