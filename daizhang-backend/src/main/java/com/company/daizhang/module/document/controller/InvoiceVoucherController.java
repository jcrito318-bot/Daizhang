package com.company.daizhang.module.document.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.document.service.InvoiceVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 发票生成凭证控制器
 */
@Slf4j
@Tag(name = "发票生成凭证")
@RestController
@RequestMapping("/invoice/voucher")
@RequiredArgsConstructor
public class InvoiceVoucherController {

    private final InvoiceVoucherService invoiceVoucherService;

    @Operation(summary = "进项发票生成凭证")
    @PostMapping("/input/{id}")
    public Result<Long> generateInputVoucher(@PathVariable Long id) {
        Long voucherId = invoiceVoucherService.generateInputVoucher(id);
        return Result.success(voucherId);
    }

    @Operation(summary = "销项发票生成凭证")
    @PostMapping("/output/{id}")
    public Result<Long> generateOutputVoucher(@PathVariable Long id) {
        Long voucherId = invoiceVoucherService.generateOutputVoucher(id);
        return Result.success(voucherId);
    }

    @Operation(summary = "批量进项发票生成凭证")
    @PostMapping("/input/batch")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER)
    public Result<List<Long>> batchGenerateInputVouchers(@RequestParam Long accountSetId,
                                                          @RequestParam(required = false) String startDate,
                                                          @RequestParam(required = false) String endDate) {
        List<Long> voucherIds = invoiceVoucherService.batchGenerateInputVouchers(accountSetId, startDate, endDate);
        return Result.success(voucherIds);
    }

    @Operation(summary = "批量销项发票生成凭证")
    @PostMapping("/output/batch")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER)
    public Result<List<Long>> batchGenerateOutputVouchers(@RequestParam Long accountSetId,
                                                          @RequestParam(required = false) String startDate,
                                                          @RequestParam(required = false) String endDate) {
        List<Long> voucherIds = invoiceVoucherService.batchGenerateOutputVouchers(accountSetId, startDate, endDate);
        return Result.success(voucherIds);
    }
}
