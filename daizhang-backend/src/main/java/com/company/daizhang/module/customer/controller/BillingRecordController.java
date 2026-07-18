package com.company.daizhang.module.customer.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.customer.dto.BillingRecordCreateRequest;
import com.company.daizhang.module.customer.dto.BillingRecordQueryRequest;
import com.company.daizhang.module.customer.dto.BillingRecordUpdateRequest;
import com.company.daizhang.module.customer.service.BillingRecordService;
import com.company.daizhang.module.customer.vo.BillingRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户开票记录管理控制器
 */
@Tag(name = "客户开票记录管理")
@RestController
@RequestMapping("/billing-record")
@RequiredArgsConstructor
public class BillingRecordController {

    private final BillingRecordService billingRecordService;

    @Operation(summary = "分页查询开票记录")
    @GetMapping("/page")
    public Result<PageResult<BillingRecordVO>> page(@Valid BillingRecordQueryRequest request) {
        PageResult<BillingRecordVO> page = billingRecordService.pageBillingRecords(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询开票记录")
    @GetMapping("/{id}")
    public Result<BillingRecordVO> getById(@PathVariable Long id) {
        BillingRecordVO vo = billingRecordService.getBillingRecordById(id);
        return Result.success(vo);
    }

    @Operation(summary = "根据客户ID查询开票记录列表")
    @GetMapping("/customer/{customerId}")
    public Result<List<BillingRecordVO>> listByCustomerId(@PathVariable Long customerId) {
        List<BillingRecordVO> list = billingRecordService.listBillingRecordsByCustomerId(customerId);
        return Result.success(list);
    }

    @Operation(summary = "创建开票记录（自动计算税额和不含税金额）")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody BillingRecordCreateRequest request) {
        Long id = billingRecordService.createBillingRecord(request);
        return Result.success(id);
    }

    @Operation(summary = "更新开票记录")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody BillingRecordUpdateRequest request) {
        billingRecordService.updateBillingRecord(id, request);
        return Result.success();
    }

    @Operation(summary = "删除开票记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        billingRecordService.deleteBillingRecord(id);
        return Result.success();
    }

    @Operation(summary = "作废开票记录")
    @PutMapping("/{id}/void")
    public Result<Void> voidBillingRecord(@PathVariable Long id) {
        billingRecordService.voidBillingRecord(id);
        return Result.success();
    }

    @Operation(summary = "标记开票记录为已收款")
    @PutMapping("/{id}/paid")
    public Result<Void> markAsPaid(@PathVariable Long id,
                                    @RequestParam(required = false) Long paymentRecordId) {
        billingRecordService.markAsPaid(id, paymentRecordId);
        return Result.success();
    }
}
