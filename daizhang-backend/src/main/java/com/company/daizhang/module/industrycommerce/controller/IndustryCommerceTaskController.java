package com.company.daizhang.module.industrycommerce.controller;

import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.industrycommerce.dto.IndustryCommerceTaskCreateRequest;
import com.company.daizhang.module.industrycommerce.dto.IndustryCommerceTaskUpdateRequest;
import com.company.daizhang.module.industrycommerce.service.IndustryCommerceTaskService;
import com.company.daizhang.module.industrycommerce.vo.IndustryCommerceTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工商外勤任务管理控制器
 */
@Tag(name = "工商外勤任务")
@RestController
@RequestMapping("/ic/task")
@RequiredArgsConstructor
public class IndustryCommerceTaskController {

    private final IndustryCommerceTaskService industryCommerceTaskService;

    @Operation(summary = "根据工商服务ID查询外勤任务列表")
    @GetMapping("/list")
    public Result<List<IndustryCommerceTaskVO>> list(@RequestParam Long serviceId) {
        List<IndustryCommerceTaskVO> list = industryCommerceTaskService.listTasksByServiceId(serviceId);
        return Result.success(list);
    }

    @Operation(summary = "创建外勤任务")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody IndustryCommerceTaskCreateRequest request) {
        Long id = industryCommerceTaskService.createTask(request);
        return Result.success(id);
    }

    @Operation(summary = "更新外勤任务")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody IndustryCommerceTaskUpdateRequest request) {
        industryCommerceTaskService.updateTask(id, request);
        return Result.success();
    }

    @Operation(summary = "删除外勤任务")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        industryCommerceTaskService.deleteTask(id);
        return Result.success();
    }

    @Operation(summary = "外勤任务派工")
    @PostMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id, @RequestParam Long assigneeId) {
        industryCommerceTaskService.assignTask(id, assigneeId);
        return Result.success();
    }

    @Operation(summary = "完成外勤任务")
    @PostMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        industryCommerceTaskService.completeTask(id);
        return Result.success();
    }
}
