package com.company.daizhang.module.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.customer.dto.CustomerCreateRequest;
import com.company.daizhang.module.customer.dto.CustomerQueryRequest;
import com.company.daizhang.module.customer.dto.CustomerUpdateRequest;
import com.company.daizhang.module.customer.entity.Customer;
import com.company.daizhang.module.customer.vo.ArrearsVO;
import com.company.daizhang.module.customer.vo.CustomerProfileVO;
import com.company.daizhang.module.customer.vo.CustomerVO;

import java.util.List;
import java.util.Map;

/**
 * 客户服务接口
 */
public interface CustomerService extends IService<Customer> {

    /**
     * 分页查询客户
     */
    PageResult<CustomerVO> pageCustomers(CustomerQueryRequest request);

    /**
     * 查询所有客户
     */
    List<CustomerVO> listAllCustomers();

    /**
     * 根据ID查询客户
     */
    CustomerVO getCustomerById(Long id);

    /**
     * 创建客户
     */
    void createCustomer(CustomerCreateRequest request);

    /**
     * 更新客户
     */
    void updateCustomer(Long id, CustomerUpdateRequest request);

    /**
     * 删除客户
     */
    void deleteCustomer(Long id);

    /**
     * 按等级/状态/行业分页查询客户
     *
     * @param customerLevel  客户等级
     * @param customerStatus 客户状态
     * @param industryType   行业类型
     * @param pageNum        页码
     * @param pageSize       每页数量
     */
    PageResult<CustomerVO> pageCustomersByLevel(String customerLevel, Integer customerStatus,
                                                String industryType, int pageNum, int pageSize);

    /**
     * 客户360度画像
     *
     * @param customerId 客户ID
     */
    CustomerProfileVO getCustomerProfile(Long customerId);

    /**
     * 更新客户状态
     *
     * @param customerId 客户ID
     * @param status     客户状态(0-潜在 1-在服 2-流失)
     */
    void updateCustomerStatus(Long customerId, Integer status);

    /**
     * 更新客户等级
     *
     * @param customerId 客户ID
     * @param level      客户等级(VIP/重要/普通/潜在)
     */
    void updateCustomerLevel(Long customerId, String level);

    /**
     * 客户统计(按等级/状态/行业分组)
     */
    List<Map<String, Object>> getCustomerStatistics();

    /**
     * 欠款列表分页查询
     *
     * @param customerName 客户名称
     * @param pageNum      页码
     * @param pageSize     每页数量
     */
    PageResult<ArrearsVO> pageArrears(String customerName, int pageNum, int pageSize);

    /**
     * 欠款详情
     *
     * @param customerId 客户ID
     */
    ArrearsVO getArrearsDetail(Long customerId);

    /**
     * 催收提醒列表
     */
    List<Map<String, Object>> getCollectionReminders();
}
