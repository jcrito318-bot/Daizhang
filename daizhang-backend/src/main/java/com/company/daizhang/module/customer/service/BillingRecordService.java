package com.company.daizhang.module.customer.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.customer.dto.BillingRecordCreateRequest;
import com.company.daizhang.module.customer.dto.BillingRecordQueryRequest;
import com.company.daizhang.module.customer.dto.BillingRecordUpdateRequest;
import com.company.daizhang.module.customer.vo.BillingRecordVO;

import java.util.List;

/**
 * 客户开票记录服务
 */
public interface BillingRecordService {

    /**
     * 分页查询开票记录
     */
    PageResult<BillingRecordVO> pageBillingRecords(BillingRecordQueryRequest request);

    /**
     * 根据ID查询开票记录
     */
    BillingRecordVO getBillingRecordById(Long id);

    /**
     * 根据客户ID查询开票记录列表
     */
    List<BillingRecordVO> listBillingRecordsByCustomerId(Long customerId);

    /**
     * 创建开票记录（自动计算税额和不含税金额）
     */
    Long createBillingRecord(BillingRecordCreateRequest request);

    /**
     * 更新开票记录
     */
    void updateBillingRecord(Long id, BillingRecordUpdateRequest request);

    /**
     * 删除开票记录
     */
    void deleteBillingRecord(Long id);

    /**
     * 作废开票记录
     */
    void voidBillingRecord(Long id);

    /**
     * 标记开票记录为已收款
     */
    void markAsPaid(Long id, Long paymentRecordId);
}
