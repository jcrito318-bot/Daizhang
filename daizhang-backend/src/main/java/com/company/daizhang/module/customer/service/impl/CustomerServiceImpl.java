package com.company.daizhang.module.customer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.customer.dto.CustomerCreateRequest;
import com.company.daizhang.module.customer.dto.CustomerQueryRequest;
import com.company.daizhang.module.customer.dto.CustomerUpdateRequest;
import com.company.daizhang.module.customer.entity.Customer;
import com.company.daizhang.module.customer.mapper.CustomerMapper;
import com.company.daizhang.module.customer.service.CustomerService;
import com.company.daizhang.module.customer.vo.CustomerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 客户服务实现
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    @Override
    public PageResult<CustomerVO> pageCustomers(CustomerQueryRequest request) {
        Page<Customer> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(request.getCustomerCode()), Customer::getCustomerCode, request.getCustomerCode())
               .like(StrUtil.isNotBlank(request.getCustomerName()), Customer::getCustomerName, request.getCustomerName())
               .eq(StrUtil.isNotBlank(request.getCustomerType()), Customer::getCustomerType, request.getCustomerType())
               .eq(StrUtil.isNotBlank(request.getIndustry()), Customer::getIndustry, request.getIndustry())
               .eq(StrUtil.isNotBlank(request.getTaxpayerType()), Customer::getTaxpayerType, request.getTaxpayerType())
               .like(StrUtil.isNotBlank(request.getContactPhone()), Customer::getContactPhone, request.getContactPhone())
               .eq(request.getStatus() != null, Customer::getStatus, request.getStatus())
               .eq(request.getAccountSetId() != null, Customer::getAccountSetId, request.getAccountSetId())
               .orderByDesc(Customer::getCreateTime);

        Page<Customer> result = this.page(page, wrapper);

        List<CustomerVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public List<CustomerVO> listAllCustomers() {
        List<Customer> list = this.list();
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerVO getCustomerById(Long id) {
        Customer customer = this.getById(id);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        return convertToVO(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCustomer(CustomerCreateRequest request) {
        // 检查编码是否已存在
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getCustomerCode, request.getCustomerCode());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(400, "客户编码已存在");
        }

        Customer customer = new Customer();
        BeanUtil.copyProperties(request, customer);
        if (customer.getStatus() == null) {
            customer.setStatus(1);
        }
        this.save(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomer(Long id, CustomerUpdateRequest request) {
        Customer customer = this.getById(id);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }

        BeanUtil.copyProperties(request, customer);
        this.updateById(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomer(Long id) {
        Customer customer = this.getById(id);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }

        this.removeById(id);
    }

    private CustomerVO convertToVO(Customer customer) {
        CustomerVO vo = new CustomerVO();
        BeanUtil.copyProperties(customer, vo);
        return vo;
    }
}
