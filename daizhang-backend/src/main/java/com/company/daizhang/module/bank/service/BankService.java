package com.company.daizhang.module.bank.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.bank.dto.*;
import com.company.daizhang.module.bank.entity.BankTransaction;
import com.company.daizhang.module.bank.vo.BankReconciliationVO;
import com.company.daizhang.module.bank.vo.BankTransactionVO;
import com.company.daizhang.module.bank.vo.UnmatchedItemVO;

import java.util.List;
import java.util.Map;

/**
 * 银行对账服务接口
 */
public interface BankService extends IService<BankTransaction> {

    /**
     * 导入银行流水
     */
    Integer importBankTransactions(BankTransactionImportRequest request);

    /**
     * 分页查询银行流水
     */
    PageResult<BankTransactionVO> pageBankTransactions(BankTransactionQueryRequest request);

    /**
     * 根据ID查询银行流水
     */
    BankTransactionVO getTransactionById(Long id);

    /**
     * 自动匹配
     */
    Integer autoMatch(AutoMatchRequest request);

    /**
     * 手动匹配
     */
    void manualMatch(ManualMatchRequest request);

    /**
     * 取消匹配
     */
    void cancelMatch(Long transactionId);

    /**
     * 生成对账单
     */
    BankReconciliationVO generateReconciliation(ReconciliationGenerateRequest request);

    /**
     * 查询对账单
     */
    BankReconciliationVO getReconciliation(Long id);

    /**
     * 查询对账单列表
     */
    PageResult<BankReconciliationVO> pageReconciliations(BankTransactionQueryRequest request);

    /**
     * 智能匹配（精确匹配+模糊匹配）
     */
    List<Map<String, Object>> smartMatch(Long accountSetId);

    /**
     * 导出余额调节表Excel
     */
    byte[] exportReconciliation(Long reconciliationId);

    /**
     * 未达账项列表
     *
     * @param accountSetId 账套ID
     * @param year         年度
     * @param month        月份
     */
    List<UnmatchedItemVO> listUnmatchedItems(Long accountSetId, Integer year, Integer month);

    /**
     * 未达账项生成凭证
     *
     * @param transactionId 银行流水ID
     * @return 凭证ID
     */
    Long generateVoucherFromUnmatched(Long transactionId);
}
