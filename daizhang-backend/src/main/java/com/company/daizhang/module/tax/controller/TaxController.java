package com.company.daizhang.module.tax.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.tax.dto.TaxCalculationCreateRequest;
import com.company.daizhang.module.tax.dto.TaxCalculationQueryRequest;
import com.company.daizhang.module.tax.dto.TaxCalculationUpdateRequest;
import com.company.daizhang.module.tax.dto.TaxDeclarationCreateRequest;
import com.company.daizhang.module.tax.dto.TaxDeclarationQueryRequest;
import com.company.daizhang.module.tax.dto.TaxDeclarationUpdateRequest;
import com.company.daizhang.module.tax.service.TaxCalculationRecordService;
import com.company.daizhang.module.tax.service.TaxDeclarationService;
import com.company.daizhang.module.tax.service.TaxService;
import com.company.daizhang.module.tax.vo.TaxCalculationResultVO;
import com.company.daizhang.module.tax.vo.TaxCalculationVO;
import com.company.daizhang.module.tax.vo.TaxCheckResultVO;
import com.company.daizhang.module.tax.vo.TaxCheckSummaryVO;
import com.company.daizhang.module.tax.vo.TaxDeadlineReminderVO;
import com.company.daizhang.module.tax.vo.TaxDeclarationFormVO;
import com.company.daizhang.module.tax.vo.TaxDeclarationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 税务管理控制器
 * 包含：申报表生成、申报记录CRUD、税务计算记录CRUD、税额自动计算触发
 */
@Slf4j
@Tag(name = "税务管理")
@RestController
@RequestMapping("/tax")
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;
    private final TaxDeclarationService taxDeclarationService;
    private final TaxCalculationRecordService taxCalculationRecordService;

    // ==================== 申报表生成（动态生成VO，不落库） ====================

    @Operation(summary = "生成申报表")
    @GetMapping("/declaration/form")
    @RequireAccountSetAccess
    public Result<TaxDeclarationFormVO> generateDeclarationForm(@RequestParam Long accountSetId,
                                                                @RequestParam Integer year,
                                                                @RequestParam Integer month,
                                                                @RequestParam String formType) {
        TaxDeclarationFormVO form = taxService.generateDeclarationForm(accountSetId, year, month, formType);
        return Result.success(form);
    }

    @Operation(summary = "导出申报表Excel")
    @GetMapping("/declaration/form/export")
    @RequireAccountSetAccess
    public ResponseEntity<byte[]> exportDeclarationForm(@RequestParam Long accountSetId,
                                                        @RequestParam Integer year,
                                                        @RequestParam Integer month,
                                                        @RequestParam String formType) {
        byte[] data = taxService.exportDeclarationForm(accountSetId, year, month, formType);
        String fileName = URLEncoder.encode("申报表_" + year + "年" + month + "月.xlsx", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set("Content-Disposition", "attachment;filename*=utf-8''" + fileName);
        headers.setContentLength(data.length);

        return ResponseEntity.ok().headers(headers).body(data);
    }

    @Operation(summary = "获取申报到期提醒")
    @GetMapping("/deadline-reminder")
    public Result<List<TaxDeadlineReminderVO>> deadlineReminder() {
        List<TaxDeadlineReminderVO> reminders = taxService.getDeadlineReminders();
        return Result.success(reminders);
    }

    @Operation(summary = "单账套税务检查（漏报/错报/状态异常）")
    @GetMapping("/check")
    @RequireAccountSetAccess
    public Result<List<TaxCheckResultVO>> checkTaxDeclaration(@RequestParam Long accountSetId,
                                                               @RequestParam Integer year,
                                                               @RequestParam Integer month) {
        List<TaxCheckResultVO> results = taxService.checkTaxDeclaration(accountSetId, year, month);
        return Result.success(results);
    }

    @Operation(summary = "全账套税务检查汇总（漏报/错报检查）")
    @GetMapping("/check/all")
    public Result<TaxCheckSummaryVO> checkAllTaxDeclarations(@RequestParam Integer year,
                                                              @RequestParam Integer month) {
        TaxCheckSummaryVO summary = taxService.checkAllTaxDeclarations(year, month);
        return Result.success(summary);
    }

    // ==================== 申报记录CRUD（tax_declaration表） ====================

    @Operation(summary = "分页查询税务申报记录")
    @GetMapping("/declaration/page")
    public Result<PageResult<TaxDeclarationVO>> pageDeclarations(TaxDeclarationQueryRequest request) {
        PageResult<TaxDeclarationVO> page = taxDeclarationService.pageDeclarations(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询税务申报记录")
    @GetMapping("/declaration/{id}")
    public Result<TaxDeclarationVO> getDeclarationById(@PathVariable Long id) {
        TaxDeclarationVO vo = taxDeclarationService.getDeclarationById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建税务申报记录")
    @PostMapping("/declaration")
    public Result<Void> createDeclaration(@Valid @RequestBody TaxDeclarationCreateRequest request) {
        taxDeclarationService.createDeclaration(request);
        return Result.success();
    }

    @Operation(summary = "更新税务申报记录")
    @PutMapping("/declaration/{id}")
    public Result<Void> updateDeclaration(@PathVariable Long id, @Valid @RequestBody TaxDeclarationUpdateRequest request) {
        taxDeclarationService.updateDeclaration(id, request);
        return Result.success();
    }

    @Operation(summary = "删除税务申报记录")
    @DeleteMapping("/declaration/{id}")
    public Result<Void> deleteDeclaration(@PathVariable Long id) {
        taxDeclarationService.deleteDeclaration(id);
        return Result.success();
    }

    @Operation(summary = "执行申报")
    @PostMapping("/declaration/{id}/declare")
    public Result<Void> declare(@PathVariable Long id) {
        taxDeclarationService.declare(id);
        return Result.success();
    }

    @Operation(summary = "执行缴款")
    @PostMapping("/declaration/{id}/pay")
    public Result<Void> pay(@PathVariable Long id,
                           @RequestParam(required = false) BigDecimal actualAmount) {
        taxDeclarationService.pay(id, actualAmount);
        return Result.success();
    }

    // ==================== 税务计算记录CRUD（tax_calculation表） ====================

    @Operation(summary = "分页查询税务计算记录")
    @GetMapping("/calculation/page")
    public Result<PageResult<TaxCalculationVO>> pageCalculations(TaxCalculationQueryRequest request) {
        PageResult<TaxCalculationVO> page = taxCalculationRecordService.pageCalculations(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询税务计算记录")
    @GetMapping("/calculation/{id}")
    public Result<TaxCalculationVO> getCalculationById(@PathVariable Long id) {
        TaxCalculationVO vo = taxCalculationRecordService.getCalculationById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建税务计算记录")
    @PostMapping("/calculation")
    public Result<Void> createCalculation(@Valid @RequestBody TaxCalculationCreateRequest request) {
        taxCalculationRecordService.createCalculation(request);
        return Result.success();
    }

    @Operation(summary = "更新税务计算记录")
    @PutMapping("/calculation/{id}")
    public Result<Void> updateCalculation(@PathVariable Long id, @Valid @RequestBody TaxCalculationUpdateRequest request) {
        taxCalculationRecordService.updateCalculation(id, request);
        return Result.success();
    }

    @Operation(summary = "删除税务计算记录")
    @DeleteMapping("/calculation/{id}")
    public Result<Void> deleteCalculation(@PathVariable Long id) {
        taxCalculationRecordService.deleteCalculation(id);
        return Result.success();
    }

    @Operation(summary = "触发税额自动计算并持久化")
    @PostMapping("/calculation/calculate")
    @RequireAccountSetAccess
    public Result<List<TaxCalculationResultVO>> calculateTax(@RequestParam Long accountSetId,
                                                              @RequestParam Integer year,
                                                              @RequestParam Integer month) {
        List<TaxCalculationResultVO> results = taxCalculationRecordService.calculateTax(accountSetId, year, month);
        return Result.success(results);
    }
}
