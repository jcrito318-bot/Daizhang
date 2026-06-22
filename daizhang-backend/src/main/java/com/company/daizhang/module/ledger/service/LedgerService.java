package com.company.daizhang.module.ledger.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.ledger.dto.LedgerQueryRequest;
import com.company.daizhang.module.ledger.dto.SubjectBalanceQueryRequest;
import com.company.daizhang.module.ledger.vo.CashJournalVO;
import com.company.daizhang.module.ledger.vo.DetailLedgerVO;
import com.company.daizhang.module.ledger.vo.GeneralLedgerVO;
import com.company.daizhang.module.ledger.vo.SubjectBalanceVO;

import java.util.List;

/**
 * 账簿查询服务
 */
public interface LedgerService {

    /**
     * 明细账
     */
    PageResult<DetailLedgerVO> detailLedger(LedgerQueryRequest request);

    /**
     * 总账
     */
    List<GeneralLedgerVO> generalLedger(LedgerQueryRequest request);

    /**
     * 科目余额表
     */
    List<SubjectBalanceVO> subjectBalance(SubjectBalanceQueryRequest request);

    /**
     * 现金日记账
     */
    PageResult<CashJournalVO> cashJournal(LedgerQueryRequest request);

    /**
     * 银行日记账
     */
    PageResult<CashJournalVO> bankJournal(LedgerQueryRequest request);
}
