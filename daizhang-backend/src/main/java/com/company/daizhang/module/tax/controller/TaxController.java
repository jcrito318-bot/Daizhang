package com.company.daizhang.module.tax.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.tax.dto.*;
import com.company.daizhang.module.tax.service.TaxService;
import com.company.daizhang.module.tax.vo.TaxCalculationVO;
import com.company.daizhang.module.tax.vo.TaxDeclarationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 税务管理控制器
 */
@Tag(name = "税务管理")
@RestController
@RequestMapping("/tax")
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;

    // ==================== 税务申报 ====================

    @Operation(summary = "分页查询税务申报")
    @GetMapping("/declaration/page")
    public Result<PageResult<TaxDeclarationVO>> pageDeclarations(TaxDeclarationQueryRequest request) {
        PageResult<TaxDeclarationVO> page = taxService.pageDeclarations(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询税务申报")
    @GetMapping("/declaration/{id}")
    public Result<TaxDeclarationVO> getDeclarationById(@PathVariable Long id) {
        TaxDeclarationVO vo = taxService.getDeclarationById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建税务申报")
    @PostMapping("/declaration")
    public Result<Void> createDeclaration(@Valid @RequestBody TaxDeclarationCreateRequest request) {
        taxService.createDeclaration(request);
        return Result.success();
    }

    @Operation(summary = "更新税务申报")
    @PutMapping("/declaration/{id}")
    public Result<Void> updateDeclaration(@PathVariable Long id, @Valid @RequestBody TaxDeclarationUpdateRequest request) {
        taxService.updateDeclaration(id, request);
        return Result.success();
    }

    @Operation(summary = "删除税务申报")
    @DeleteMapping("/declaration/{id}")
    public Result<Void> deleteDeclaration(@PathVariable Long id) {
        taxService.deleteDeclaration(id);
        return Result.success();
    }

    @Operation(summary = "申报税务")
    @PostMapping("/declaration/{id}/declare")
    public Result<Void> declare(@PathVariable Long id) {
        taxService.declare(id);
        return Result.success();
    }

    @Operation(summary = "缴纳税款")
    @PostMapping("/declaration/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        taxService.pay(id);
        return Result.success();
    }

    // ==================== 税务计算 ====================

    @Operation(summary = "分页查询税务计算")
    @GetMapping("/calculation/page")
    public Result<PageResult<TaxCalculationVO>> pageCalculations(TaxCalculationQueryRequest request) {
        PageResult<TaxCalculationVO> page = taxService.pageCalculations(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询税务计算")
    @GetMapping("/calculation/{id}")
    public Result<TaxCalculationVO> getCalculationById(@PathVariable Long id) {
        TaxCalculationVO vo = taxService.getCalculationById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建税务计算")
    @PostMapping("/calculation")
    public Result<Void> createCalculation(@Valid @RequestBody TaxCalculationCreateRequest request) {
        taxService.createCalculation(request);
        return Result.success();
    }

    @Operation(summary = "更新税务计算")
    @PutMapping("/calculation/{id}")
    public Result<Void> updateCalculation(@PathVariable Long id, @Valid @RequestBody TaxCalculationUpdateRequest request) {
        taxService.updateCalculation(id, request);
        return Result.success();
    }

    @Operation(summary = "删除税务计算")
    @DeleteMapping("/calculation/{id}")
    public Result<Void> deleteCalculation(@PathVariable Long id) {
        taxService.deleteCalculation(id);
        return Result.success();
    }

    // ==================== 税务统计 ====================

    @Operation(summary = "计算税额")
    @GetMapping("/calculate")
    public Result<BigDecimal> calculateTax(
            @RequestParam Long accountSetId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam String taxType) {
        BigDecimal totalTax = taxService.calculateTax(accountSetId, year, month, taxType);
        return Result.success(totalTax);
    }
}
