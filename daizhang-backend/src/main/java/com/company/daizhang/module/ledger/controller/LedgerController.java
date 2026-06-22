package com.company.daizhang.module.ledger.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.ledger.dto.LedgerQueryRequest;
import com.company.daizhang.module.ledger.dto.SubjectBalanceQueryRequest;
import com.company.daizhang.module.ledger.service.LedgerService;
import com.company.daizhang.module.ledger.vo.CashJournalVO;
import com.company.daizhang.module.ledger.vo.DetailLedgerVO;
import com.company.daizhang.module.ledger.vo.GeneralLedgerVO;
import com.company.daizhang.module.ledger.vo.SubjectBalanceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 账簿查询控制器
 */
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
}
