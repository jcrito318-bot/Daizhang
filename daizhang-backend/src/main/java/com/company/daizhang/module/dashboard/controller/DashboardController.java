package com.company.daizhang.module.dashboard.controller;

import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.dashboard.service.DashboardService;
import com.company.daizhang.module.dashboard.vo.DashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 代账公司运营看板控制器
 */
@Tag(name = "代账公司运营看板")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取运营看板总览（含统计、客户摘要、待办看板）")
    @GetMapping
    public Result<DashboardVO> getDashboard() {
        DashboardVO vo = dashboardService.getDashboard();
        return Result.success(vo);
    }
}
