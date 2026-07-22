package com.company.daizhang.module.salary.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.salary.dto.*;
import com.company.daizhang.module.salary.service.PayslipPushService;
import com.company.daizhang.module.salary.service.SalaryService;
import com.company.daizhang.module.salary.vo.EmployeeVO;
import com.company.daizhang.module.salary.vo.PayslipPushRecordVO;
import com.company.daizhang.module.salary.vo.PayslipPushResultVO;
import com.company.daizhang.module.salary.vo.SalaryItemVO;
import com.company.daizhang.module.salary.vo.SalarySheetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
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
    private final PayslipPushService payslipPushService;

    // ==================== 员工管理 ====================

    @Operation(summary = "分页查询员工")
    @GetMapping("/employee/page")
    public Result<PageResult<EmployeeVO>> pageEmployees(@Valid EmployeeQueryRequest request) {
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
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        salaryService.createEmployee(request);
        return Result.success();
    }

    @Operation(summary = "更新员工")
    @PutMapping("/employee/{id}")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request) {
        salaryService.updateEmployee(id, request);
        return Result.success();
    }

    @Operation(summary = "删除员工")
    @DeleteMapping("/employee/{id}")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> deleteEmployee(@PathVariable Long id) {
        salaryService.deleteEmployee(id);
        return Result.success();
    }

    // ==================== 薪资项目管理 ====================

    @Operation(summary = "分页查询薪资项目")
    @GetMapping("/item/page")
    public Result<PageResult<SalaryItemVO>> pageSalaryItems(@Valid SalaryItemQueryRequest request) {
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
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> createSalaryItem(@Valid @RequestBody SalaryItemCreateRequest request) {
        salaryService.createSalaryItem(request);
        return Result.success();
    }

    @Operation(summary = "更新薪资项目")
    @PutMapping("/item/{id}")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> updateSalaryItem(@PathVariable Long id, @Valid @RequestBody SalaryItemUpdateRequest request) {
        salaryService.updateSalaryItem(id, request);
        return Result.success();
    }

    @Operation(summary = "删除薪资项目")
    @DeleteMapping("/item/{id}")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> deleteSalaryItem(@PathVariable Long id) {
        salaryService.deleteSalaryItem(id);
        return Result.success();
    }

    // ==================== 薪资表管理 ====================

    @Operation(summary = "分页查询薪资表")
    @GetMapping("/sheet/page")
    public Result<PageResult<SalarySheetVO>> pageSalarySheets(@Valid SalarySheetQueryRequest request) {
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
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> createSalarySheet(@Valid @RequestBody SalarySheetCreateRequest request) {
        salaryService.createSalarySheet(request);
        return Result.success();
    }

    @Operation(summary = "更新薪资表")
    @PutMapping("/sheet/{id}")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> updateSalarySheet(@PathVariable Long id, @Valid @RequestBody SalarySheetUpdateRequest request) {
        salaryService.updateSalarySheet(id, request);
        return Result.success();
    }

    @Operation(summary = "删除薪资表")
    @DeleteMapping("/sheet/{id}")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> deleteSalarySheet(@PathVariable Long id) {
        salaryService.deleteSalarySheet(id);
        return Result.success();
    }

    @Operation(summary = "确认薪资表")
    @PostMapping("/sheet/{id}/confirm")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> confirmSalarySheet(@PathVariable Long id) {
        salaryService.confirmSalarySheet(id);
        return Result.success();
    }

    @Operation(summary = "发放薪资")
    @PostMapping("/sheet/{id}/pay")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> paySalarySheet(@PathVariable Long id) {
        salaryService.paySalarySheet(id);
        return Result.success();
    }

    // ==================== 薪资计算 ====================

    @Operation(summary = "批量计算薪资")
    @PostMapping("/calculate")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> calculateSalary(@Valid @RequestBody SalaryCalculateRequest request) {
        salaryService.calculateSalary(request);
        return Result.success();
    }

    @Operation(summary = "生成薪资凭证")
    @PostMapping("/voucher/generate")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> generateSalaryVoucher(@Valid @RequestBody SalaryVoucherGenerateRequest request) {
        salaryService.generateSalaryVoucher(request);
        return Result.success();
    }

    // ==================== 薪资导出 ====================

    @Operation(summary = "导出银行代发工资文件（Excel）")
    @GetMapping("/export/bank-disbursement")
    @RequireAccountSetAccess
    public void exportBankDisbursementFile(@RequestParam Long accountSetId,
                                           @RequestParam Integer year,
                                           @RequestParam Integer month,
                                           HttpServletResponse response) {
        salaryService.exportBankDisbursementFile(accountSetId, year, month, response);
    }

    @Operation(summary = "导出工资条（Excel）")
    @GetMapping("/export/payslip")
    @RequireAccountSetAccess
    public void exportPayslips(@RequestParam Long accountSetId,
                               @RequestParam Integer year,
                               @RequestParam Integer month,
                               HttpServletResponse response) {
        salaryService.exportPayslips(accountSetId, year, month, response);
    }

    // ==================== 工资条推送(B7) ====================

    @Operation(summary = "批量推送工资条（B7）")
    @PostMapping("/payslip/push")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<PayslipPushResultVO> pushPayslip(@RequestParam Long salarySheetId) {
        PayslipPushResultVO result = payslipPushService.batchPushPayslip(salarySheetId);
        return Result.success(result);
    }

    @Operation(summary = "查询工资条推送记录（B7）")
    @GetMapping("/payslip/push/records")
    public Result<PageResult<PayslipPushRecordVO>> pagePushRecords(
            @RequestParam Long salarySheetId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<PayslipPushRecordVO> result = payslipPushService.pagePushRecords(salarySheetId, page, size);
        return Result.success(result);
    }
}
