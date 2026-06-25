package com.company.daizhang.module.bank.controller;

import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.bank.service.BankVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 银行流水生成凭证控制器
 */
@Slf4j
@Tag(name = "银行流水生成凭证")
@RestController
@RequestMapping("/bank/voucher")
@RequiredArgsConstructor
public class BankVoucherController {

    private final BankVoucherService bankVoucherService;

    @Operation(summary = "单笔银行流水生成凭证")
    @PostMapping("/generate/{transactionId}")
    public Result<Long> generateVoucher(@PathVariable Long transactionId) {
        Long voucherId = bankVoucherService.generateVoucher(transactionId);
        return Result.success(voucherId);
    }

    @Operation(summary = "批量生成银行流水凭证")
    @PostMapping("/batch-generate")
    public Result<List<Long>> batchGenerateVouchers(@RequestParam Long accountSetId) {
        List<Long> voucherIds = bankVoucherService.batchGenerateVouchers(accountSetId);
        return Result.success(voucherIds);
    }
}
