package com.company.daizhang.module.bank.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.bank.dto.*;
import com.company.daizhang.module.bank.entity.BankTransaction;
import com.company.daizhang.module.bank.vo.BankReconciliationVO;
import com.company.daizhang.module.bank.vo.BankTransactionVO;

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
}
