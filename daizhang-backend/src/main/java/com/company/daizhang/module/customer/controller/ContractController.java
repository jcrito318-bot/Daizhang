package com.company.daizhang.module.customer.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.customer.dto.ContractCreateRequest;
import com.company.daizhang.module.customer.dto.ContractQueryRequest;
import com.company.daizhang.module.customer.dto.ContractUpdateRequest;
import com.company.daizhang.module.customer.service.ContractService;
import com.company.daizhang.module.customer.vo.ContractRenewalReminderVO;
import com.company.daizhang.module.customer.vo.ContractVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 服务合同管理控制器
 */
@Tag(name = "服务合同管理")
@RestController
@RequestMapping("/contract")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @Operation(summary = "分页查询合同")
    @GetMapping("/page")
    public Result<PageResult<ContractVO>> page(ContractQueryRequest request) {
        PageResult<ContractVO> page = contractService.pageContracts(request);
        return Result.success(page);
    }

    @Operation(summary = "根据客户ID查询合同列表")
    @GetMapping("/customer/{customerId}")
    public Result<List<ContractVO>> listByCustomerId(@PathVariable Long customerId) {
        List<ContractVO> list = contractService.listContractsByCustomerId(customerId);
        return Result.success(list);
    }

    @Operation(summary = "根据ID查询合同")
    @GetMapping("/{id}")
    public Result<ContractVO> getById(@PathVariable Long id) {
        ContractVO contract = contractService.getContractById(id);
        return Result.success(contract);
    }

    @Operation(summary = "创建合同")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ContractCreateRequest request) {
        contractService.createContract(request);
        return Result.success();
    }

    @Operation(summary = "更新合同")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ContractUpdateRequest request) {
        contractService.updateContract(id, request);
        return Result.success();
    }

    @Operation(summary = "激活合同（草稿→执行中）")
    @PutMapping("/{id}/activate")
    public Result<Void> activate(@PathVariable Long id) {
        contractService.activateContract(id);
        return Result.success();
    }

    @Operation(summary = "完结合同（执行中→已完成）")
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        contractService.completeContract(id);
        return Result.success();
    }

    @Operation(summary = "终止合同（执行中/已完成→已终止）")
    @PutMapping("/{id}/terminate")
    public Result<Void> terminate(@PathVariable Long id) {
        contractService.terminateContract(id);
        return Result.success();
    }

    @Operation(summary = "删除合同")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        contractService.deleteContract(id);
        return Result.success();
    }

    @Operation(summary = "合同续费提醒")
    @GetMapping("/renewal-reminder")
    public Result<List<ContractRenewalReminderVO>> renewalReminder(
            @RequestParam(defaultValue = "30") Integer daysThreshold) {
        List<ContractRenewalReminderVO> reminders = contractService.getRenewalReminders(daysThreshold);
        return Result.success(reminders);
    }
}
