package com.company.daizhang.module.customer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.customer.dto.PaymentCreateRequest;
import com.company.daizhang.module.customer.dto.PaymentQueryRequest;
import com.company.daizhang.module.customer.dto.PaymentUpdateRequest;
import com.company.daizhang.module.customer.entity.Customer;
import com.company.daizhang.module.customer.entity.PaymentRecord;
import com.company.daizhang.module.customer.entity.ServiceContract;
import com.company.daizhang.module.customer.mapper.CustomerMapper;
import com.company.daizhang.module.customer.mapper.PaymentRecordMapper;
import com.company.daizhang.module.customer.mapper.ServiceContractMapper;
import com.company.daizhang.module.customer.service.PaymentService;
import com.company.daizhang.module.customer.vo.PaymentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 收款记录服务实现
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecord> implements PaymentService {

    private final CustomerMapper customerMapper;
    private final ServiceContractMapper contractMapper;
    private final AccountSetAccessService accountSetAccessService;

    @Override
    public PageResult<PaymentVO> pagePayments(PaymentQueryRequest request) {
        Page<PaymentRecord> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getContractId() != null, PaymentRecord::getContractId, request.getContractId())
               .eq(request.getCustomerId() != null, PaymentRecord::getCustomerId, request.getCustomerId())
               .eq(StrUtil.isNotBlank(request.getPaymentMethod()), PaymentRecord::getPaymentMethod, request.getPaymentMethod())
               .eq(StrUtil.isNotBlank(request.getPaymentType()), PaymentRecord::getPaymentType, request.getPaymentType())
               .orderByDesc(PaymentRecord::getPaymentDate);

        // IDOR治理:仅返回当前用户可访问账套下客户的收款记录(实体无accountSetId,通过customer关联链过滤)
        Set<Long> accessibleIds = accountSetAccessService.listAccessibleAccountSetIds();
        if (accessibleIds != null) {
            if (accessibleIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize());
            }
            List<Long> customerIds = listAccessibleCustomerIds(accessibleIds);
            if (customerIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize());
            }
            wrapper.in(PaymentRecord::getCustomerId, customerIds);
        }

        Page<PaymentRecord> result = this.page(page, wrapper);

        List<PaymentVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public List<PaymentVO> listPaymentsByCustomerId(Long customerId) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRecord::getCustomerId, customerId)
               .orderByDesc(PaymentRecord::getPaymentDate);

        // IDOR治理:仅返回当前用户可访问账套下客户的收款记录(实体无accountSetId,通过customer关联链过滤)
        Set<Long> accessibleIds = accountSetAccessService.listAccessibleAccountSetIds();
        if (accessibleIds != null) {
            if (accessibleIds.isEmpty()) {
                return Collections.emptyList();
            }
            List<Long> customerIds = listAccessibleCustomerIds(accessibleIds);
            if (customerIds.isEmpty()) {
                return Collections.emptyList();
            }
            wrapper.in(PaymentRecord::getCustomerId, customerIds);
        }

        List<PaymentRecord> list = this.list(wrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentVO> listPaymentsByContractId(Long contractId) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRecord::getContractId, contractId)
               .orderByDesc(PaymentRecord::getPaymentDate);

        // IDOR治理:仅返回当前用户可访问账套下客户的收款记录(实体无accountSetId,通过customer关联链过滤)
        Set<Long> accessibleIds = accountSetAccessService.listAccessibleAccountSetIds();
        if (accessibleIds != null) {
            if (accessibleIds.isEmpty()) {
                return Collections.emptyList();
            }
            List<Long> customerIds = listAccessibleCustomerIds(accessibleIds);
            if (customerIds.isEmpty()) {
                return Collections.emptyList();
            }
            wrapper.in(PaymentRecord::getCustomerId, customerIds);
        }

        List<PaymentRecord> list = this.list(wrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentVO getPaymentById(Long id) {
        PaymentRecord payment = this.getById(id);
        if (payment == null) {
            throw new BusinessException(404, "收款记录不存在");
        }
        // IDOR治理:校验当前用户对该收款记录所属账套的访问权(读操作用checkAccess)
        Customer customer = customerMapper.selectById(payment.getCustomerId());
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        accountSetAccessService.checkAccess(customer.getAccountSetId());
        return convertToVO(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPayment(PaymentCreateRequest request) {
        // 检查客户是否存在
        Customer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        // IDOR治理:校验当前用户对该收款记录所属账套的所有者权限(写操作用checkOwner)
        accountSetAccessService.checkOwner(customer.getAccountSetId());

        // 如果指定了合同，检查合同是否存在且归属于该客户
        if (request.getContractId() != null) {
            ServiceContract contract = contractMapper.selectById(request.getContractId());
            if (contract == null) {
                throw new BusinessException(404, "合同不存在");
            }
            // 校验合同归属,防止为任意客户关联任意合同(越权)
            if (contract.getCustomerId() == null
                    || !contract.getCustomerId().equals(request.getCustomerId())) {
                throw new BusinessException(403, "合同不属于该客户，不能关联");
            }
        }

        PaymentRecord payment = new PaymentRecord();
        BeanUtil.copyProperties(request, payment);
        this.save(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePayment(Long id, PaymentUpdateRequest request) {
        PaymentRecord payment = this.getById(id);
        if (payment == null) {
            throw new BusinessException(404, "收款记录不存在");
        }
        // IDOR治理:校验当前用户对该收款记录所属账套的所有者权限(写操作用checkOwner)
        Customer customer = customerMapper.selectById(payment.getCustomerId());
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        accountSetAccessService.checkOwner(customer.getAccountSetId());

        // 若变更了合同,校验新合同存在且归属于同一客户,防止收款记录关联到错误合同
        if (request.getContractId() != null
                && !request.getContractId().equals(payment.getContractId())) {
            ServiceContract newContract = contractMapper.selectById(request.getContractId());
            if (newContract == null) {
                throw new BusinessException(404, "合同不存在");
            }
            if (newContract.getCustomerId() == null
                    || !newContract.getCustomerId().equals(payment.getCustomerId())) {
                throw new BusinessException(403, "合同不属于该客户，不能关联");
            }
        }

        BeanUtil.copyProperties(request, payment);
        this.updateById(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePayment(Long id) {
        PaymentRecord payment = this.getById(id);
        if (payment == null) {
            throw new BusinessException(404, "收款记录不存在");
        }
        // IDOR治理:校验当前用户对该收款记录所属账套的所有者权限(写操作用checkOwner)
        Customer customer = customerMapper.selectById(payment.getCustomerId());
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        accountSetAccessService.checkOwner(customer.getAccountSetId());

        this.removeById(id);
    }

    /**
     * IDOR治理:查询指定账套集合下的客户ID集合(用于实体无accountSetId时按customerId过滤)
     */
    private List<Long> listAccessibleCustomerIds(Set<Long> accessibleIds) {
        LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
        customerWrapper.in(Customer::getAccountSetId, accessibleIds);
        return customerMapper.selectList(customerWrapper).stream()
                .map(Customer::getId)
                .collect(Collectors.toList());
    }

    private PaymentVO convertToVO(PaymentRecord payment) {
        PaymentVO vo = new PaymentVO();
        BeanUtil.copyProperties(payment, vo);

        // 填充客户名称
        if (payment.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(payment.getCustomerId());
            if (customer != null) {
                vo.setCustomerName(customer.getCustomerName());
            }
        }

        // 填充合同编号
        if (payment.getContractId() != null) {
            ServiceContract contract = contractMapper.selectById(payment.getContractId());
            if (contract != null) {
                vo.setContractNo(contract.getContractNo());
            }
        }

        return vo;
    }
}
