package com.company.daizhang.module.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.customer.dto.CustomerCreateRequest;
import com.company.daizhang.module.customer.dto.CustomerQueryRequest;
import com.company.daizhang.module.customer.dto.CustomerUpdateRequest;
import com.company.daizhang.module.customer.entity.Customer;
import com.company.daizhang.module.customer.vo.CustomerVO;

import java.util.List;

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
}
