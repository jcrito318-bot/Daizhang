package com.company.daizhang.module.crm.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.crm.dto.OpportunityQueryRequest;
import com.company.daizhang.module.crm.dto.OpportunityRequest;
import com.company.daizhang.module.crm.service.OpportunityService;
import com.company.daizhang.module.crm.vo.OpportunityStatisticsVO;
import com.company.daizhang.module.crm.vo.OpportunityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商机管理控制器
 */
@Tag(name = "CRM商机管理")
@RestController
@RequestMapping("/crm/opportunity")
@RequiredArgsConstructor
public class OpportunityController {

    private final OpportunityService opportunityService;

    @Operation(summary = "分页查询商机")
    @GetMapping("/page")
    public Result<PageResult<OpportunityVO>> page(OpportunityQueryRequest request) {
        PageResult<OpportunityVO> page = opportunityService.pageOpportunities(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询商机")
    @GetMapping("/{id}")
    public Result<OpportunityVO> getById(@PathVariable Long id) {
        OpportunityVO opportunity = opportunityService.getOpportunityById(id);
        return Result.success(opportunity);
    }

    @Operation(summary = "创建商机")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody OpportunityRequest request) {
        opportunityService.createOpportunity(request);
        return Result.success();
    }

    @Operation(summary = "更新商机")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody OpportunityRequest request) {
        opportunityService.updateOpportunity(id, request);
        return Result.success();
    }

    @Operation(summary = "删除商机")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        opportunityService.deleteOpportunity(id);
        return Result.success();
    }

    @Operation(summary = "变更商机阶段")
    @PutMapping("/{id}/stage")
    public Result<Void> changeStage(@PathVariable Long id, @RequestParam String stage) {
        opportunityService.changeStage(id, stage);
        return Result.success();
    }

    @Operation(summary = "商机统计")
    @GetMapping("/statistics")
    public Result<OpportunityStatisticsVO> statistics() {
        OpportunityStatisticsVO statistics = opportunityService.getStatistics();
        return Result.success(statistics);
    }
}
