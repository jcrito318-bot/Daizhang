package com.company.daizhang.module.bank.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.bank.dto.*;
import com.company.daizhang.module.bank.service.BankService;
import com.company.daizhang.module.bank.vo.BankReconciliationVO;
import com.company.daizhang.module.bank.vo.BankTransactionVO;
import com.company.daizhang.module.bank.vo.UnmatchedItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 银行对账控制器
 */
@Tag(name = "银行对账管理")
@RestController
@RequestMapping("/bank")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    @Operation(summary = "导入银行流水")
    @PostMapping("/transaction/import")
    public Result<Integer> importTransactions(@Valid @RequestBody BankTransactionImportRequest request) {
        Integer count = bankService.importBankTransactions(request);
        return Result.success("成功导入" + count + "条银行流水", count);
    }

    @Operation(summary = "分页查询银行流水")
    @GetMapping("/transaction/page")
    public Result<PageResult<BankTransactionVO>> pageTransactions(BankTransactionQueryRequest request) {
        PageResult<BankTransactionVO> page = bankService.pageBankTransactions(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询银行流水")
    @GetMapping("/transaction/{id}")
    public Result<BankTransactionVO> getTransactionById(@PathVariable Long id) {
        BankTransactionVO vo = bankService.getTransactionById(id);
        return Result.success(vo);
    }

    @Operation(summary = "删除银行流水")
    @DeleteMapping("/transaction/{id}")
    public Result<Void> deleteTransaction(@PathVariable Long id) {
        bankService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "自动匹配")
    @PostMapping("/match/auto")
    public Result<Integer> autoMatch(@Valid @RequestBody AutoMatchRequest request) {
        Integer count = bankService.autoMatch(request);
        return Result.success("自动匹配完成，共匹配" + count + "条", count);
    }

    @Operation(summary = "手动匹配")
    @PostMapping("/match/manual")
    public Result<Void> manualMatch(@Valid @RequestBody ManualMatchRequest request) {
        bankService.manualMatch(request);
        return Result.success();
    }

    @Operation(summary = "取消匹配")
    @PostMapping("/match/cancel/{id}")
    public Result<Void> cancelMatch(@PathVariable Long id) {
        bankService.cancelMatch(id);
        return Result.success();
    }

    @Operation(summary = "生成对账单")
    @PostMapping("/reconciliation/generate")
    public Result<BankReconciliationVO> generateReconciliation(@Valid @RequestBody ReconciliationGenerateRequest request) {
        BankReconciliationVO vo = bankService.generateReconciliation(request);
        return Result.success(vo);
    }

    @Operation(summary = "查询对账单详情")
    @GetMapping("/reconciliation/{id}")
    public Result<BankReconciliationVO> getReconciliation(@PathVariable Long id) {
        BankReconciliationVO vo = bankService.getReconciliation(id);
        return Result.success(vo);
    }

    @Operation(summary = "分页查询对账单")
    @GetMapping("/reconciliation/page")
    public Result<PageResult<BankReconciliationVO>> pageReconciliations(BankTransactionQueryRequest request) {
        PageResult<BankReconciliationVO> page = bankService.pageReconciliations(request);
        return Result.success(page);
    }

    @Operation(summary = "智能匹配")
    @PostMapping("/match/smart")
    @RequireAccountSetAccess
    public Result<List<Map<String, Object>>> smartMatch(@RequestParam Long accountSetId) {
        List<Map<String, Object>> suggestions = bankService.smartMatch(accountSetId);
        return Result.success(suggestions);
    }

    @Operation(summary = "导出余额调节表")
    @GetMapping("/reconciliation/{id}/export")
    public void exportReconciliation(@PathVariable Long id, HttpServletResponse response) {
        byte[] data = bankService.exportReconciliation(id);
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String encodedFileName = URLEncoder.encode("余额调节表.xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName);
            response.getOutputStream().write(data);
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new BusinessException("导出余额调节表失败");
        }
    }

    @Operation(summary = "未达账项列表")
    @GetMapping("/unmatched-items")
    @RequireAccountSetAccess
    public Result<List<UnmatchedItemVO>> listUnmatchedItems(
            @RequestParam Long accountSetId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        List<UnmatchedItemVO> list = bankService.listUnmatchedItems(accountSetId, year, month);
        return Result.success(list);
    }

    @Operation(summary = "未达账项生成凭证")
    @PostMapping("/unmatched-items/{transactionId}/generate-voucher")
    public Result<Long> generateVoucherFromUnmatched(@PathVariable Long transactionId) {
        Long voucherId = bankService.generateVoucherFromUnmatched(transactionId);
        return Result.success(voucherId);
    }
}
