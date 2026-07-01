package com.company.daizhang.module.salary.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.salary.dto.SalaryFormulaRequest;
import com.company.daizhang.module.salary.service.SalaryFormulaService;
import com.company.daizhang.module.salary.vo.SalaryFormulaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 薪资公式管理控制器
 */
@Slf4j
@Tag(name = "薪资公式管理")
@RestController
@RequestMapping("/salary/formula")
@RequiredArgsConstructor
public class SalaryFormulaController {

    private final SalaryFormulaService salaryFormulaService;

    @Operation(summary = "查询薪资公式列表")
    @GetMapping("/list")
    @RequireAccountSetAccess
    public Result<List<SalaryFormulaVO>> list(@RequestParam Long accountSetId) {
        List<SalaryFormulaVO> list = salaryFormulaService.listFormulas(accountSetId);
        return Result.success(list);
    }

    @Operation(summary = "创建薪资公式")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody SalaryFormulaRequest request) {
        salaryFormulaService.createFormula(request);
        return Result.success();
    }

    @Operation(summary = "更新薪资公式")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SalaryFormulaRequest request) {
        salaryFormulaService.updateFormula(id, request);
        return Result.success();
    }

    @Operation(summary = "删除薪资公式")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        salaryFormulaService.deleteFormula(id);
        return Result.success();
    }
}
