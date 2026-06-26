package com.company.daizhang.module.voucher.service;

import com.company.daizhang.module.voucher.vo.MinimalAccountSetVO;
import com.company.daizhang.module.voucher.vo.MinimalVoucherBatchResultVO;

import java.util.List;

/**
 * 极简记账服务接口
 * 用于零申报/无票客户的批量快速记账
 */
public interface MinimalVoucherService {

    /**
     * 识别极简/零申报账套
     * 筛选无销项发票、无进项发票、无银行流水的账套
     *
     * @param year  年度
     * @param month 月份
     * @return 极简账套列表
     */
    List<MinimalAccountSetVO> identifyMinimalAccountSets(Integer year, Integer month);

    /**
     * 批量生成极简凭证
     * 为选中的账套批量生成工资发放、水电等标准凭证
     *
     * @param accountSetIds 账套ID列表
     * @param year          年度
     * @param month         月份
     * @param voucherTypes  凭证类型：SALARY(工资)/UTILITY(水电)/RENT(租金)
     * @return 批量结果
     */
    MinimalVoucherBatchResultVO batchGenerateMinimalVouchers(List<Long> accountSetIds,
                                                              Integer year, Integer month,
                                                              List<String> voucherTypes);

    /**
     * 批量审核极简凭证
     * 为指定账套指定期间的所有未审核凭证批量审核
     *
     * @param accountSetIds 账套ID列表
     * @param year          年度
     * @param month         月份
     * @return 成功审核的凭证数量
     */
    int batchAuditMinimalVouchers(List<Long> accountSetIds, Integer year, Integer month);
}
