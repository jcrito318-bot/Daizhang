package com.company.daizhang.module.document.service;

import java.util.List;

/**
 * 发票生成凭证服务
 */
public interface InvoiceVoucherService {

    /**
     * 进项发票生成凭证
     *
     * @param invoiceId 进项发票ID
     * @return 凭证ID
     */
    Long generateInputVoucher(Long invoiceId);

    /**
     * 销项发票生成凭证
     *
     * @param invoiceId 销项发票ID
     * @return 凭证ID
     */
    Long generateOutputVoucher(Long invoiceId);

    /**
     * 批量生成进项发票凭证
     *
     * @param accountSetId 账套ID
     * @param startDate    开始日期(yyyy-MM-dd)
     * @param endDate      结束日期(yyyy-MM-dd)
     * @return 生成的凭证ID列表
     */
    List<Long> batchGenerateInputVouchers(Long accountSetId, String startDate, String endDate);

    /**
     * 批量生成销项发票凭证
     *
     * @param accountSetId 账套ID
     * @param startDate    开始日期(yyyy-MM-dd)
     * @param endDate      结束日期(yyyy-MM-dd)
     * @return 生成的凭证ID列表
     */
    List<Long> batchGenerateOutputVouchers(Long accountSetId, String startDate, String endDate);
}
