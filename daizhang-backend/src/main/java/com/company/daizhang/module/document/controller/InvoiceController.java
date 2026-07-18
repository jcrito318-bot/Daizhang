package com.company.daizhang.module.document.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.document.dto.InputInvoiceRequest;
import com.company.daizhang.module.document.dto.InvoiceQueryRequest;
import com.company.daizhang.module.document.dto.OutputInvoiceRequest;
import com.company.daizhang.module.document.service.InvoiceService;
import com.company.daizhang.module.document.vo.InputInvoiceVO;
import com.company.daizhang.module.document.vo.InvoiceStatisticsVO;
import com.company.daizhang.module.document.vo.OutputInvoiceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 发票管理控制器
 */
@Slf4j
@Tag(name = "发票管理")
@RestController
@RequestMapping("/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    // ==================== 进项发票 ====================

    @Operation(summary = "分页查询进项发票")
    @GetMapping("/input/page")
    public Result<PageResult<InputInvoiceVO>> pageInput(@Valid InvoiceQueryRequest request) {
        PageResult<InputInvoiceVO> page = invoiceService.pageInputInvoices(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询进项发票")
    @GetMapping("/input/{id}")
    public Result<InputInvoiceVO> getInputById(@PathVariable Long id) {
        InputInvoiceVO invoice = invoiceService.getInputInvoiceById(id);
        return Result.success(invoice);
    }

    @Operation(summary = "创建进项发票")
    @PostMapping("/input")
    public Result<Void> createInput(@Valid @RequestBody InputInvoiceRequest request) {
        invoiceService.createInputInvoice(request);
        return Result.success();
    }

    @Operation(summary = "更新进项发票")
    @PutMapping("/input/{id}")
    public Result<Void> updateInput(@PathVariable Long id, @Valid @RequestBody InputInvoiceRequest request) {
        invoiceService.updateInputInvoice(id, request);
        return Result.success();
    }

    @Operation(summary = "删除进项发票")
    @DeleteMapping("/input/{id}")
    public Result<Void> deleteInput(@PathVariable Long id) {
        invoiceService.deleteInputInvoice(id);
        return Result.success();
    }

    @Operation(summary = "认证进项发票")
    @PostMapping("/input/{id}/authenticate")
    public Result<Void> authenticateInput(@PathVariable Long id) {
        invoiceService.authenticateInputInvoice(id);
        return Result.success();
    }

    // ==================== 销项发票 ====================

    @Operation(summary = "分页查询销项发票")
    @GetMapping("/output/page")
    public Result<PageResult<OutputInvoiceVO>> pageOutput(@Valid InvoiceQueryRequest request) {
        PageResult<OutputInvoiceVO> page = invoiceService.pageOutputInvoices(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询销项发票")
    @GetMapping("/output/{id}")
    public Result<OutputInvoiceVO> getOutputById(@PathVariable Long id) {
        OutputInvoiceVO invoice = invoiceService.getOutputInvoiceById(id);
        return Result.success(invoice);
    }

    @Operation(summary = "创建销项发票")
    @PostMapping("/output")
    public Result<Void> createOutput(@Valid @RequestBody OutputInvoiceRequest request) {
        invoiceService.createOutputInvoice(request);
        return Result.success();
    }

    @Operation(summary = "更新销项发票")
    @PutMapping("/output/{id}")
    public Result<Void> updateOutput(@PathVariable Long id, @Valid @RequestBody OutputInvoiceRequest request) {
        invoiceService.updateOutputInvoice(id, request);
        return Result.success();
    }

    @Operation(summary = "删除销项发票")
    @DeleteMapping("/output/{id}")
    public Result<Void> deleteOutput(@PathVariable Long id) {
        invoiceService.deleteOutputInvoice(id);
        return Result.success();
    }

    @Operation(summary = "作废销项发票")
    @PostMapping("/output/{id}/void")
    public Result<Void> voidOutput(@PathVariable Long id) {
        invoiceService.voidOutputInvoice(id);
        return Result.success();
    }

    // ==================== 统计 ====================

    @Operation(summary = "发票统计")
    @GetMapping("/statistics")
    @RequireAccountSetAccess
    public Result<InvoiceStatisticsVO> statistics(@RequestParam Long accountSetId,
                                                   @RequestParam Integer year,
                                                   @RequestParam Integer month) {
        InvoiceStatisticsVO statistics = invoiceService.getInvoiceStatistics(accountSetId, year, month);
        return Result.success(statistics);
    }
}
