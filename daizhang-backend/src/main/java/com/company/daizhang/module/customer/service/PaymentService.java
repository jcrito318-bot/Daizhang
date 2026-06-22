package com.company.daizhang.module.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.customer.dto.PaymentCreateRequest;
import com.company.daizhang.module.customer.dto.PaymentQueryRequest;
import com.company.daizhang.module.customer.dto.PaymentUpdateRequest;
import com.company.daizhang.module.customer.entity.PaymentRecord;
import com.company.daizhang.module.customer.vo.PaymentVO;

import java.util.List;

/**
 * 收款记录服务接口
 */
public interface PaymentService extends IService<PaymentRecord> {

    /**
     * 分页查询收款记录
     */
    PageResult<PaymentVO> pagePayments(PaymentQueryRequest request);

    /**
     * 根据客户ID查询收款记录
     */
    List<PaymentVO> listPaymentsByCustomerId(Long customerId);

    /**
     * 根据合同ID查询收款记录
     */
    List<PaymentVO> listPaymentsByContractId(Long contractId);

    /**
     * 根据ID查询收款记录
     */
    PaymentVO getPaymentById(Long id);

    /**
     * 创建收款记录
     */
    void createPayment(PaymentCreateRequest request);

    /**
     * 更新收款记录
     */
    void updatePayment(Long id, PaymentUpdateRequest request);

    /**
     * 删除收款记录
     */
    void deletePayment(Long id);
}
