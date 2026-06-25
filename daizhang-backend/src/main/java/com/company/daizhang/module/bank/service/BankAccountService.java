package com.company.daizhang.module.bank.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.bank.dto.BankAccountQueryRequest;
import com.company.daizhang.module.bank.dto.BankAccountRequest;
import com.company.daizhang.module.bank.vo.BankAccountVO;

import java.util.List;

/**
 * 银行账户主数据服务接口
 */
public interface BankAccountService {

    /**
     * 分页查询银行账户
     */
    PageResult<BankAccountVO> pageBankAccounts(BankAccountQueryRequest request);

    /**
     * 查询某账套的所有银行账户
     */
    List<BankAccountVO> listByAccountSetId(Long accountSetId);

    /**
     * 根据ID查询银行账户
     */
    BankAccountVO getBankAccountById(Long id);

    /**
     * 创建银行账户
     */
    void createBankAccount(BankAccountRequest request);

    /**
     * 更新银行账户
     */
    void updateBankAccount(Long id, BankAccountRequest request);

    /**
     * 删除银行账户
     */
    void deleteBankAccount(Long id);

    /**
     * 更新账户状态
     */
    void updateStatus(Long id, Integer status);
}
