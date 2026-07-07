package com.company.daizhang.module.ledger.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.ledger.dto.LedgerQueryRequest;
import com.company.daizhang.module.ledger.dto.SubjectBalanceQueryRequest;
import com.company.daizhang.module.ledger.vo.AccountCheckVO;
import com.company.daizhang.module.ledger.vo.AgingAnalysisVO;
import com.company.daizhang.module.ledger.vo.AuxiliaryDetailLedgerVO;
import com.company.daizhang.module.ledger.vo.CashJournalVO;
import com.company.daizhang.module.ledger.vo.DetailLedgerVO;
import com.company.daizhang.module.ledger.vo.GeneralLedgerVO;
import com.company.daizhang.module.ledger.vo.MultiColumnLedgerVO;
import com.company.daizhang.module.ledger.vo.QuantityAmountLedgerVO;
import com.company.daizhang.module.ledger.vo.ReconciliationVO;
import com.company.daizhang.module.ledger.vo.SubjectBalanceVO;
import jakarta.servlet.http.HttpServletResponse;

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

    /**
     * 多栏账
     */
    MultiColumnLedgerVO multiColumnLedger(Long accountSetId, Long subjectId, Integer year, Integer month);

    /**
     * 数量金额账
     */
    QuantityAmountLedgerVO quantityAmountLedger(Long accountSetId, Long subjectId, Integer year, Integer month);

    /**
     * 辅助核算明细账
     *
     * @param accountSetId 账套ID
     * @param subjectId    科目ID
     * @param auxiliaryId  辅助核算项目ID
     * @param year         年度
     * @param month        月份（可为空，表示查询全年）
     */
    AuxiliaryDetailLedgerVO auxiliaryDetailLedger(Long accountSetId, Long subjectId, Long auxiliaryId, Integer year, Integer month);

    /**
     * 导出明细账Excel
     */
    byte[] exportDetailLedger(Long accountSetId, Long subjectId, Integer year, Integer month);

    /**
     * 导出总账Excel
     */
    byte[] exportGeneralLedger(Long accountSetId, Integer year, Integer month);

    /**
     * 导出科目余额表Excel
     */
    byte[] exportSubjectBalance(Long accountSetId, Integer year, Integer startMonth, Integer endMonth);

    /**
     * 账龄分析
     *
     * @param accountSetId 账套ID
     * @param year         年度
     * @param month         月份（可为空，表示查询全年）
     * @param subjectType  科目类型：receivable(应收) 或 payable(应付)
     */
    List<AgingAnalysisVO> agingAnalysis(Long accountSetId, Integer year, Integer month, String subjectType);

    /**
     * 往来对账
     *
     * @param accountSetId 账套ID
     * @param subjectId    科目ID
     * @param auxiliaryId  辅助核算项目ID
     * @param year         年度
     * @param month        月份（可为空，表示查询全年）
     */
    ReconciliationVO reconciliation(Long accountSetId, Long subjectId, Long auxiliaryId, Integer year, Integer month);

    /**
     * 账账核对
     *
     * @param accountSetId 账套ID
     * @param year         年度
     * @param month        月份（可为空，表示查询全年）
     */
    List<AccountCheckVO> accountCheck(Long accountSetId, Integer year, Integer month);

    /**
     * 导出现金日记账Excel
     */
    void exportCashJournal(Long accountSetId, Integer year, Integer month, HttpServletResponse response);

    /**
     * 导出银行日记账Excel
     *
     * @param bankAccountId 银行科目ID（可选，为空时导出全部银行科目日记账）
     */
    void exportBankJournal(Long accountSetId, Integer year, Integer month, Long bankAccountId, HttpServletResponse response);
}
