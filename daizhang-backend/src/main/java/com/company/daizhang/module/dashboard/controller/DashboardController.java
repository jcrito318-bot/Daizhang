package com.company.daizhang.module.dashboard.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.dashboard.service.DashboardService;
import com.company.daizhang.module.dashboard.vo.DashboardVO;
import com.company.daizhang.module.dashboard.vo.TodoItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public Result<DashboardVO> getDashboard(@RequestParam(required = false) Long accountSetId) {
        DashboardVO vo = dashboardService.getDashboard(accountSetId);
        return Result.success(vo);
    }

    @Operation(summary = "分页查询待办事项", description = "合并服务任务、凭证审核、税务申报，按创建时间倒序分页")
    @GetMapping("/todo")
    public Result<PageResult<TodoItemVO>> pageTodoItems(@RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer size) {
        PageResult<TodoItemVO> result = dashboardService.pageTodoItems(page, size);
        return Result.success(result);
    }
}
