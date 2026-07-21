package com.company.daizhang.module.amortization.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.amortization.dto.AmortizationRequest;
import com.company.daizhang.module.amortization.service.AmortizationService;
import com.company.daizhang.module.amortization.vo.AmortizationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 长期待摊费用控制器
 */
@Tag(name = "长期待摊费用")
@RestController
@RequestMapping("/amortization")
@RequiredArgsConstructor
public class AmortizationController {

    private final AmortizationService amortizationService;

    @Operation(summary = "分页查询长期待摊费用")
    @GetMapping("/page")
    @RequireAccountSetAccess
    public Result<PageResult<AmortizationVO>> page(
            @RequestParam(required = false) Long accountSetId,
            @RequestParam(required = false) String amortizationName,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<AmortizationVO> page = amortizationService.pageAmortizations(accountSetId, amortizationName, status, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询长期待摊费用")
    @GetMapping("/{id}")
    public Result<AmortizationVO> getById(@PathVariable Long id) {
        AmortizationVO vo = amortizationService.getAmortizationById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建长期待摊费用")
    @PostMapping
    // IDOR 防护(纵深防御):创建时强制校验账套所有者权限,防止跨账套越权创建
    @RequireAccountSetAccess(RequireAccountSetAccess.AccessLevel.OWNER)
    public Result<Void> create(@Valid @RequestBody AmortizationRequest request) {
        amortizationService.createAmortization(request);
        return Result.success();
    }

    @Operation(summary = "更新长期待摊费用")
    @PutMapping("/{id}")
    // IDOR 防护(纵深防御):更新时按 id 解析账套并校验所有者权限(required=false 因 id 非账套参数)
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AmortizationRequest request) {
        amortizationService.updateAmortization(id, request);
        return Result.success();
    }

    @Operation(summary = "删除长期待摊费用")
    @DeleteMapping("/{id}")
    // IDOR 防护(纵深防御):删除时按 id 解析账套并校验所有者权限(required=false 因 id 非账套参数)
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> delete(@PathVariable Long id) {
        amortizationService.deleteAmortization(id);
        return Result.success();
    }

    @Operation(summary = "执行月摊销")
    @PostMapping("/{id}/amortize")
    // IDOR 防护(纵深防御):摊销时按 id 解析账套并校验所有者权限(required=false 因 id 非账套参数)
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> amortize(@PathVariable Long id,
                                 @RequestParam Integer year,
                                 @RequestParam Integer month) {
        amortizationService.amortize(id, year, month);
        return Result.success();
    }

    @Operation(summary = "批量摊销所有摊销中的费用")
    @PostMapping("/batch-amortize")
    @RequireAccountSetAccess
    public Result<Void> batchAmortize(@RequestParam Long accountSetId,
                                      @RequestParam Integer year,
                                      @RequestParam Integer month) {
        amortizationService.batchAmortize(accountSetId, year, month);
        return Result.success();
    }

    @Operation(summary = "生成摊销凭证")
    @PostMapping("/{id}/voucher")
    // IDOR 防护(纵深防御):生成凭证时按 id 解析账套并校验所有者权限(required=false 因 id 非账套参数)
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Long> generateAmortizationVoucher(@PathVariable Long id,
                                                     @RequestParam Integer year,
                                                     @RequestParam Integer month) {
        Long voucherId = amortizationService.generateAmortizationVoucher(id, year, month);
        return Result.success(voucherId);
    }

    @Operation(summary = "批量生成摊销凭证")
    @PostMapping("/batch-voucher")
    @RequireAccountSetAccess
    public Result<java.util.List<Long>> batchGenerateAmortizationVouchers(@RequestParam Long accountSetId,
                                                                           @RequestParam Integer year,
                                                                           @RequestParam Integer month) {
        java.util.List<Long> voucherIds = amortizationService.batchGenerateAmortizationVouchers(accountSetId, year, month);
        return Result.success(voucherIds);
    }
}
