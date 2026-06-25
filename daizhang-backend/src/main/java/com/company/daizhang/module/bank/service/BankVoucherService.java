package com.company.daizhang.module.bank.service;

import java.util.List;

/**
 * 银行流水生成凭证服务
 */
public interface BankVoucherService {

    /**
     * 单笔银行流水生成凭证
     *
     * @param transactionId 银行流水ID
     * @return 凭证ID
     */
    Long generateVoucher(Long transactionId);

    /**
     * 批量生成所有未匹配的银行流水凭证
     *
     * @param accountSetId 账套ID
     * @return 生成的凭证ID列表
     */
    List<Long> batchGenerateVouchers(Long accountSetId);
}
