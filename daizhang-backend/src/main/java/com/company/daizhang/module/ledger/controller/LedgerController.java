package com.company.daizhang.module.ledger.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.ledger.dto.LedgerQueryRequest;
import com.company.daizhang.module.ledger.dto.SubjectBalanceQueryRequest;
import com.company.daizhang.module.ledger.service.LedgerService;
import com.company.daizhang.module.ledger.vo.AccountCheckVO;
import com.company.daizhang.module.ledger.vo.AgingAnalysisVO;
import com.company.daizhang.module.ledger.vo.AuxiliaryBalanceVO;
import com.company.daizhang.module.ledger.vo.AuxiliaryDetailLedgerVO;
import com.company.daizhang.module.ledger.vo.CashJournalVO;
import com.company.daizhang.module.ledger.vo.DetailLedgerVO;
import com.company.daizhang.module.ledger.vo.GeneralLedgerVO;
import com.company.daizhang.module.ledger.vo.MultiColumnLedgerVO;
import com.company.daizhang.module.ledger.vo.QuantityAmountLedgerVO;
import com.company.daizhang.module.ledger.vo.ReconciliationVO;
import com.company.daizhang.module.ledger.vo.SubjectBalanceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 账簿查询控制器
 */
@Slf4j
@Tag(name = "账簿查询")
@RestController
@RequestMapping("/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @Operation(summary = "明细账")
    @GetMapping("/detail")
    public Result<PageResult<DetailLedgerVO>> detailLedger(LedgerQueryRequest request) {
        PageResult<DetailLedgerVO> result = ledgerService.detailLedger(request);
        return Result.success(result);
    }

    @Operation(summary = "总账")
    @GetMapping("/general")
    public Result<List<GeneralLedgerVO>> generalLedger(LedgerQueryRequest request) {
        List<GeneralLedgerVO> result = ledgerService.generalLedger(request);
        return Result.success(result);
    }

    @Operation(summary = "科目余额表")
    @GetMapping("/subject-balance")
    public Result<List<SubjectBalanceVO>> subjectBalance(SubjectBalanceQueryRequest request) {
        List<SubjectBalanceVO> result = ledgerService.subjectBalance(request);
        return Result.success(result);
    }

    @Operation(summary = "现金日记账")
    @GetMapping("/cash-journal")
    public Result<PageResult<CashJournalVO>> cashJournal(LedgerQueryRequest request) {
        PageResult<CashJournalVO> result = ledgerService.cashJournal(request);
        return Result.success(result);
    }

    @Operation(summary = "银行日记账")
    @GetMapping("/bank-journal")
    public Result<PageResult<CashJournalVO>> bankJournal(LedgerQueryRequest request) {
        PageResult<CashJournalVO> result = ledgerService.bankJournal(request);
        return Result.success(result);
    }

    @Operation(summary = "多栏账")
    @GetMapping("/multi-column")
    @RequireAccountSetAccess
    public Result<MultiColumnLedgerVO> multiColumnLedger(@RequestParam Long accountSetId,
                                                          @RequestParam Long subjectId,
                                                          @RequestParam Integer year,
                                                          @RequestParam(required = false) Integer month) {
        MultiColumnLedgerVO result = ledgerService.multiColumnLedger(accountSetId, subjectId, year, month);
        return Result.success(result);
    }

    @Operation(summary = "数量金额账")
    @GetMapping("/quantity-amount")
    @RequireAccountSetAccess
    public Result<QuantityAmountLedgerVO> quantityAmountLedger(@RequestParam Long accountSetId,
                                                                @RequestParam Long subjectId,
                                                                @RequestParam Integer year,
                                                                @RequestParam(required = false) Integer month) {
        QuantityAmountLedgerVO result = ledgerService.quantityAmountLedger(accountSetId, subjectId, year, month);
        return Result.success(result);
    }

    @Operation(summary = "辅助核算明细账")
    @GetMapping("/auxiliary-detail")
    @RequireAccountSetAccess
    public Result<AuxiliaryDetailLedgerVO> auxiliaryDetailLedger(@RequestParam Long accountSetId,
                                                                   @RequestParam Long subjectId,
                                                                   @RequestParam Long auxiliaryId,
                                                                   @RequestParam Integer year,
                                                                   @RequestParam(required = false) Integer month) {
        AuxiliaryDetailLedgerVO result = ledgerService.auxiliaryDetailLedger(accountSetId, subjectId, auxiliaryId, year, month);
        return Result.success(result);
    }

    @Operation(summary = "辅助核算余额表")
    @GetMapping("/auxiliary-balance")
    @RequireAccountSetAccess
    public Result<List<AuxiliaryBalanceVO>> auxiliaryBalance(@RequestParam Long accountSetId,
                                                               @RequestParam(required = false) Long categoryId,
                                                               @RequestParam Integer year,
                                                               @RequestParam(required = false) Integer month) {
        // 按"科目 + 辅助核算项"维度汇总期初/本期发生/期末借贷方余额
        // 期初从年初至查询月份之前(不含)的已过账凭证明细累计,本期为查询期间发生额
        List<AuxiliaryBalanceVO> result = ledgerService.auxiliaryBalance(accountSetId, categoryId, year, month);
        return Result.success(result);
    }

    @Operation(summary = "导出明细账Excel")
    @GetMapping("/detail/export")
    @RequireAccountSetAccess
    public void exportDetailLedger(@RequestParam Long accountSetId,
                                   @RequestParam Long subjectId,
                                   @RequestParam Integer year,
                                   @RequestParam(required = false) Integer month,
                                   HttpServletResponse response) {
        byte[] data = ledgerService.exportDetailLedger(accountSetId, subjectId, year, month);
        writeExcelResponse(response, data, "明细账_" + year + "年" + (month != null ? month + "月" : "") + ".xlsx");
    }

    @Operation(summary = "导出总账Excel")
    @GetMapping("/general/export")
    @RequireAccountSetAccess
    public void exportGeneralLedger(@RequestParam Long accountSetId,
                                   @RequestParam Integer year,
                                   @RequestParam(required = false) Integer month,
                                   HttpServletResponse response) {
        byte[] data = ledgerService.exportGeneralLedger(accountSetId, year, month);
        writeExcelResponse(response, data, "总账_" + year + "年" + (month != null ? month + "月" : "") + ".xlsx");
    }

    @Operation(summary = "导出科目余额表Excel")
    @GetMapping("/subject-balance/export")
    @RequireAccountSetAccess
    public void exportSubjectBalance(@RequestParam Long accountSetId,
                                     @RequestParam Integer year,
                                     @RequestParam(required = false) Integer startMonth,
                                     @RequestParam(required = false) Integer endMonth,
                                     HttpServletResponse response) {
        byte[] data = ledgerService.exportSubjectBalance(accountSetId, year, startMonth, endMonth);
        Integer start = startMonth != null ? startMonth : 1;
        Integer end = endMonth != null ? endMonth : 12;
        writeExcelResponse(response, data, "科目余额表_" + year + "年" + start + "-" + end + "月.xlsx");
    }

    @Operation(summary = "导出现金日记账Excel")
    @GetMapping("/cash-journal/export")
    @RequireAccountSetAccess
    public void exportCashJournal(@RequestParam Long accountSetId,
                                  @RequestParam Integer year,
                                  @RequestParam Integer month,
                                  HttpServletResponse response) {
        ledgerService.exportCashJournal(accountSetId, year, month, response);
    }

    @Operation(summary = "导出银行日记账Excel")
    @GetMapping("/bank-journal/export")
    @RequireAccountSetAccess
    public void exportBankJournal(@RequestParam Long accountSetId,
                                  @RequestParam Integer year,
                                  @RequestParam Integer month,
                                  @RequestParam(required = false) Long bankAccountId,
                                  HttpServletResponse response) {
        ledgerService.exportBankJournal(accountSetId, year, month, bankAccountId, response);
    }

    @Operation(summary = "账龄分析")
    @GetMapping("/aging-analysis")
    @RequireAccountSetAccess
    public Result<List<AgingAnalysisVO>> agingAnalysis(@RequestParam Long accountSetId,
                                                       @RequestParam Integer year,
                                                       @RequestParam(required = false) Integer month,
                                                       @RequestParam String subjectType) {
        List<AgingAnalysisVO> result = ledgerService.agingAnalysis(accountSetId, year, month, subjectType);
        return Result.success(result);
    }

    @Operation(summary = "往来对账")
    @GetMapping("/reconciliation")
    @RequireAccountSetAccess
    public Result<ReconciliationVO> reconciliation(@RequestParam Long accountSetId,
                                                    @RequestParam Long subjectId,
                                                    @RequestParam Long auxiliaryId,
                                                    @RequestParam Integer year,
                                                    @RequestParam(required = false) Integer month) {
        ReconciliationVO result = ledgerService.reconciliation(accountSetId, subjectId, auxiliaryId, year, month);
        return Result.success(result);
    }

    @Operation(summary = "账账核对")
    @GetMapping("/account-check")
    @RequireAccountSetAccess
    public Result<List<AccountCheckVO>> accountCheck(@RequestParam Long accountSetId,
                                                     @RequestParam Integer year,
                                                     @RequestParam(required = false) Integer month) {
        List<AccountCheckVO> result = ledgerService.accountCheck(accountSetId, year, month);
        return Result.success(result);
    }

    /**
     * 将Excel字节数组写入响应
     */
    private void writeExcelResponse(HttpServletResponse response, byte[] data, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName);
        try (OutputStream out = response.getOutputStream()) {
            out.write(data);
            out.flush();
        } catch (IOException e) {
            log.error("写入Excel响应失败", e);
            throw new RuntimeException("导出失败", e);
        }
    }
}
