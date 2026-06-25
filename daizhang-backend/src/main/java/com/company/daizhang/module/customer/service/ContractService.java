package com.company.daizhang.module.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.customer.dto.ContractCreateRequest;
import com.company.daizhang.module.customer.dto.ContractQueryRequest;
import com.company.daizhang.module.customer.dto.ContractUpdateRequest;
import com.company.daizhang.module.customer.entity.ServiceContract;
import com.company.daizhang.module.customer.vo.ContractRenewalReminderVO;
import com.company.daizhang.module.customer.vo.ContractVO;

import java.util.List;

/**
 * 服务合同服务接口
 */
public interface ContractService extends IService<ServiceContract> {

    /**
     * 分页查询合同
     */
    PageResult<ContractVO> pageContracts(ContractQueryRequest request);

    /**
     * 根据客户ID查询合同列表
     */
    List<ContractVO> listContractsByCustomerId(Long customerId);

    /**
     * 根据ID查询合同
     */
    ContractVO getContractById(Long id);

    /**
     * 创建合同
     */
    void createContract(ContractCreateRequest request);

    /**
     * 更新合同
     */
    void updateContract(Long id, ContractUpdateRequest request);

    /**
     * 删除合同
     */
    void deleteContract(Long id);

    /**
     * 获取即将到期的合同（续费提醒）
     *
     * @param daysThreshold 天数阈值，查询未来daysThreshold天内到期的合同
     */
    List<ContractRenewalReminderVO> getRenewalReminders(Integer daysThreshold);
}
