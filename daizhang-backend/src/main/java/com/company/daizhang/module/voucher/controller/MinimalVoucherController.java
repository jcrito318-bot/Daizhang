package com.company.daizhang.module.voucher.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.voucher.service.MinimalVoucherService;
import com.company.daizhang.module.voucher.vo.MinimalAccountSetVO;
import com.company.daizhang.module.voucher.vo.MinimalVoucherBatchResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 极简记账控制器
 * 零申报/无票客户批量快速记账
 */
@Tag(name = "极简记账")
@RestController
@RequestMapping("/voucher/minimal")
@RequiredArgsConstructor
public class MinimalVoucherController {

    private final MinimalVoucherService minimalVoucherService;

    @Operation(summary = "识别极简/零申报账套")
    @GetMapping("/identify")
    public Result<List<MinimalAccountSetVO>> identify(@RequestParam Integer year,
                                                       @RequestParam Integer month) {
        List<MinimalAccountSetVO> list = minimalVoucherService.identifyMinimalAccountSets(year, month);
        return Result.success(list);
    }

    @Operation(summary = "批量生成极简凭证（工资/水电/租金）")
    @PostMapping("/batch-generate")
    @RequireAccountSetAccess
    public Result<MinimalVoucherBatchResultVO> batchGenerate(
            @RequestParam List<Long> accountSetIds,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(defaultValue = "SALARY") List<String> voucherTypes) {
        MinimalVoucherBatchResultVO result = minimalVoucherService.batchGenerateMinimalVouchers(
                accountSetIds, year, month, voucherTypes);
        return Result.success(result);
    }

    @Operation(summary = "批量审核极简凭证")
    @PostMapping("/batch-audit")
    @RequireAccountSetAccess
    public Result<Integer> batchAudit(
            @RequestParam List<Long> accountSetIds,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        int count = minimalVoucherService.batchAuditMinimalVouchers(accountSetIds, year, month);
        return Result.success(count);
    }
}
