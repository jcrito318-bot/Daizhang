package com.company.daizhang.module.salary.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.salary.dto.SpecialDeductionQueryRequest;
import com.company.daizhang.module.salary.dto.SpecialDeductionRequest;
import com.company.daizhang.module.salary.service.SpecialDeductionService;
import com.company.daizhang.module.salary.vo.SpecialDeductionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 个税专项附加扣除管理控制器
 */
@Slf4j
@Tag(name = "个税专项附加扣除管理")
@RestController
@RequestMapping("/salary/special-deduction")
@RequiredArgsConstructor
public class SpecialDeductionController {

    private final SpecialDeductionService specialDeductionService;

    @Operation(summary = "分页查询专项附加扣除")
    @GetMapping("/page")
    public Result<PageResult<SpecialDeductionVO>> page(@Valid SpecialDeductionQueryRequest request) {
        PageResult<SpecialDeductionVO> page = specialDeductionService.pageDeductions(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询专项附加扣除")
    @GetMapping("/{id}")
    public Result<SpecialDeductionVO> getById(@PathVariable Long id) {
        SpecialDeductionVO vo = specialDeductionService.getDeductionById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建专项附加扣除")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody SpecialDeductionRequest request) {
        specialDeductionService.createDeduction(request);
        return Result.success();
    }

    @Operation(summary = "更新专项附加扣除")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SpecialDeductionRequest request) {
        specialDeductionService.updateDeduction(id, request);
        return Result.success();
    }

    @Operation(summary = "删除专项附加扣除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        specialDeductionService.deleteDeduction(id);
        return Result.success();
    }

    @Operation(summary = "计算员工某月专项附加扣除总额")
    @GetMapping("/calculate")
    public Result<BigDecimal> calculateMonthlyDeduction(@RequestParam Long employeeId,
                                                          @RequestParam Integer year,
                                                          @RequestParam Integer month) {
        BigDecimal amount = specialDeductionService.calculateMonthlyDeduction(employeeId, year, month);
        return Result.success(amount);
    }
}
