package com.company.daizhang.module.salary.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.salary.dto.*;
import com.company.daizhang.module.salary.service.SalaryService;
import com.company.daizhang.module.salary.vo.EmployeeVO;
import com.company.daizhang.module.salary.vo.SalaryItemVO;
import com.company.daizhang.module.salary.vo.SalarySheetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 薪资管理控制器
 */
@Tag(name = "薪资管理")
@RestController
@RequestMapping("/salary")
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;

    // ==================== 员工管理 ====================

    @Operation(summary = "分页查询员工")
    @GetMapping("/employee/page")
    public Result<PageResult<EmployeeVO>> pageEmployees(EmployeeQueryRequest request) {
        PageResult<EmployeeVO> page = salaryService.pageEmployees(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询员工")
    @GetMapping("/employee/{id}")
    public Result<EmployeeVO> getEmployeeById(@PathVariable Long id) {
        EmployeeVO vo = salaryService.getEmployeeById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建员工")
    @PostMapping("/employee")
    public Result<Void> createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        salaryService.createEmployee(request);
        return Result.success();
    }

    @Operation(summary = "更新员工")
    @PutMapping("/employee/{id}")
    public Result<Void> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request) {
        salaryService.updateEmployee(id, request);
        return Result.success();
    }

    @Operation(summary = "删除员工")
    @DeleteMapping("/employee/{id}")
    public Result<Void> deleteEmployee(@PathVariable Long id) {
        salaryService.deleteEmployee(id);
        return Result.success();
    }

    // ==================== 薪资项目管理 ====================

    @Operation(summary = "分页查询薪资项目")
    @GetMapping("/item/page")
    public Result<PageResult<SalaryItemVO>> pageSalaryItems(SalaryItemQueryRequest request) {
        PageResult<SalaryItemVO> page = salaryService.pageSalaryItems(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询薪资项目")
    @GetMapping("/item/{id}")
    public Result<SalaryItemVO> getSalaryItemById(@PathVariable Long id) {
        SalaryItemVO vo = salaryService.getSalaryItemById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建薪资项目")
    @PostMapping("/item")
    public Result<Void> createSalaryItem(@Valid @RequestBody SalaryItemCreateRequest request) {
        salaryService.createSalaryItem(request);
        return Result.success();
    }

    @Operation(summary = "更新薪资项目")
    @PutMapping("/item/{id}")
    public Result<Void> updateSalaryItem(@PathVariable Long id, @Valid @RequestBody SalaryItemUpdateRequest request) {
        salaryService.updateSalaryItem(id, request);
        return Result.success();
    }

    @Operation(summary = "删除薪资项目")
    @DeleteMapping("/item/{id}")
    public Result<Void> deleteSalaryItem(@PathVariable Long id) {
        salaryService.deleteSalaryItem(id);
        return Result.success();
    }

    // ==================== 薪资表管理 ====================

    @Operation(summary = "分页查询薪资表")
    @GetMapping("/sheet/page")
    public Result<PageResult<SalarySheetVO>> pageSalarySheets(SalarySheetQueryRequest request) {
        PageResult<SalarySheetVO> page = salaryService.pageSalarySheets(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询薪资表")
    @GetMapping("/sheet/{id}")
    public Result<SalarySheetVO> getSalarySheetById(@PathVariable Long id) {
        SalarySheetVO vo = salaryService.getSalarySheetById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建薪资表")
    @PostMapping("/sheet")
    public Result<Void> createSalarySheet(@Valid @RequestBody SalarySheetCreateRequest request) {
        salaryService.createSalarySheet(request);
        return Result.success();
    }

    @Operation(summary = "更新薪资表")
    @PutMapping("/sheet/{id}")
    public Result<Void> updateSalarySheet(@PathVariable Long id, @Valid @RequestBody SalarySheetUpdateRequest request) {
        salaryService.updateSalarySheet(id, request);
        return Result.success();
    }

    @Operation(summary = "删除薪资表")
    @DeleteMapping("/sheet/{id}")
    public Result<Void> deleteSalarySheet(@PathVariable Long id) {
        salaryService.deleteSalarySheet(id);
        return Result.success();
    }

    @Operation(summary = "确认薪资表")
    @PostMapping("/sheet/{id}/confirm")
    public Result<Void> confirmSalarySheet(@PathVariable Long id) {
        salaryService.confirmSalarySheet(id);
        return Result.success();
    }

    @Operation(summary = "发放薪资")
    @PostMapping("/sheet/{id}/pay")
    public Result<Void> paySalarySheet(@PathVariable Long id) {
        salaryService.paySalarySheet(id);
        return Result.success();
    }

    // ==================== 薪资计算 ====================

    @Operation(summary = "批量计算薪资")
    @PostMapping("/calculate")
    public Result<Void> calculateSalary(@Valid @RequestBody SalaryCalculateRequest request) {
        salaryService.calculateSalary(request);
        return Result.success();
    }

    @Operation(summary = "生成薪资凭证")
    @PostMapping("/voucher/generate")
    public Result<Void> generateSalaryVoucher(@Valid @RequestBody SalaryVoucherGenerateRequest request) {
        salaryService.generateSalaryVoucher(request);
        return Result.success();
    }
}
