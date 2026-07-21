package com.company.daizhang.module.biz.controller;

import cn.hutool.core.bean.BeanUtil;
import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.biz.dto.ServiceFlowNodeRequest;
import com.company.daizhang.module.biz.dto.ServiceTaskRequest;
import com.company.daizhang.module.biz.entity.ServiceFlowNode;
import com.company.daizhang.module.biz.entity.ServiceTask;
import com.company.daizhang.module.biz.service.ServiceFlowService;
import com.company.daizhang.module.biz.vo.ServiceFlowNodeVO;
import com.company.daizhang.module.biz.vo.ServiceTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 代账服务流程管理控制器
 */
@Slf4j
@Tag(name = "代账服务流程管理")
@RestController
@RequestMapping("/service-flow")
@RequiredArgsConstructor
public class ServiceFlowController {

    private final ServiceFlowService serviceFlowService;

    @Operation(summary = "流程节点列表")
    @GetMapping("/node/list")
    public Result<List<ServiceFlowNodeVO>> nodeList() {
        List<ServiceFlowNodeVO> list = serviceFlowService.listNodes();
        return Result.success(list);
    }

    @Operation(summary = "创建节点")
    @PostMapping("/node")
    public Result<Void> createNode(@Valid @RequestBody ServiceFlowNodeRequest request) {
        ServiceFlowNode entity = new ServiceFlowNode();
        BeanUtil.copyProperties(request, entity);
        serviceFlowService.createNode(entity);
        return Result.success();
    }

    @Operation(summary = "更新节点")
    @PutMapping("/node/{id}")
    public Result<Void> updateNode(@PathVariable Long id, @Valid @RequestBody ServiceFlowNodeRequest request) {
        ServiceFlowNode entity = new ServiceFlowNode();
        BeanUtil.copyProperties(request, entity);
        entity.setId(id);
        serviceFlowService.updateNode(entity);
        return Result.success();
    }

    @Operation(summary = "删除节点")
    @DeleteMapping("/node/{id}")
    public Result<Void> deleteNode(@PathVariable Long id) {
        serviceFlowService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "任务分页")
    @GetMapping("/task/page")
    @RequireAccountSetAccess(required = false)
    public Result<PageResult<ServiceTaskVO>> taskPage(@RequestParam(required = false) Long accountSetId,
                                                       @RequestParam(required = false) Integer year,
                                                       @RequestParam(required = false) Integer month,
                                                       @RequestParam(required = false) Integer taskStatus,
                                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<ServiceTaskVO> page = serviceFlowService.pageTasks(accountSetId, year, month, taskStatus, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询任务详情")
    @GetMapping("/task/{id}")
    public Result<ServiceTask> getTaskById(@PathVariable Long id) {
        ServiceTask task = serviceFlowService.getTaskById(id);
        return Result.success(task);
    }

    @Operation(summary = "创建任务")
    @PostMapping("/task")
    // IDOR 防护(纵深防御):edge-level 预校验,Service 层仍保留 checkOwner 作为兜底
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> createTask(@Valid @RequestBody ServiceTaskRequest request) {
        ServiceTask entity = new ServiceTask();
        BeanUtil.copyProperties(request, entity);
        serviceFlowService.createTask(entity);
        return Result.success();
    }

    @Operation(summary = "更新任务")
    @PutMapping("/task/{id}")
    // IDOR 防护(纵深防御):edge-level 预校验,Service 层仍保留 checkOwner 作为兜底
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> updateTask(@PathVariable Long id, @Valid @RequestBody ServiceTaskRequest request) {
        ServiceTask entity = new ServiceTask();
        BeanUtil.copyProperties(request, entity);
        entity.setId(id);
        serviceFlowService.updateTask(entity);
        return Result.success();
    }

    @Operation(summary = "分配任务")
    @PostMapping("/task/{id}/assign")
    // IDOR 防护(纵深防御):edge-level 预校验,Service 层仍保留 checkOwner 作为兜底
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> assignTask(@PathVariable Long id,
                                   @RequestParam Long assigneeId,
                                   @RequestParam String assigneeName) {
        serviceFlowService.assignTask(id, assigneeId, assigneeName);
        return Result.success();
    }

    @Operation(summary = "完成任务")
    @PostMapping("/task/{id}/complete")
    // IDOR 防护(纵深防御):edge-level 预校验,Service 层仍保留 checkOwner 作为兜底
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> completeTask(@PathVariable Long id) {
        serviceFlowService.completeTask(id);
        return Result.success();
    }

    @Operation(summary = "删除任务")
    @DeleteMapping("/task/{id}")
    // IDOR 防护(纵深防御):edge-level 预校验,Service 层仍保留 checkOwner 作为兜底
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> deleteTask(@PathVariable Long id) {
        serviceFlowService.deleteTask(id);
        return Result.success();
    }

    @Operation(summary = "员工工作负荷统计（按assigneeId分组，含任务数/逾期数/按时完成率/平均完成时长）")
    @GetMapping("/workload")
    public Result<java.util.List<com.company.daizhang.module.biz.vo.EmployeeWorkloadVO>> getEmployeeWorkload(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        java.util.List<com.company.daizhang.module.biz.vo.EmployeeWorkloadVO> list =
                serviceFlowService.getEmployeeWorkload(year, month);
        return Result.success(list);
    }
}
