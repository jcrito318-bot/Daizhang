package com.company.daizhang.module.customer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.customer.dto.ContractCreateRequest;
import com.company.daizhang.module.customer.dto.ContractQueryRequest;
import com.company.daizhang.module.customer.dto.ContractUpdateRequest;
import com.company.daizhang.module.customer.entity.BillingRecord;
import com.company.daizhang.module.customer.entity.Customer;
import com.company.daizhang.module.customer.entity.PaymentRecord;
import com.company.daizhang.module.customer.entity.ServiceContract;
import com.company.daizhang.module.customer.mapper.BillingRecordMapper;
import com.company.daizhang.module.customer.mapper.CustomerMapper;
import com.company.daizhang.module.customer.mapper.PaymentRecordMapper;
import com.company.daizhang.module.customer.mapper.ServiceContractMapper;
import com.company.daizhang.module.customer.service.ContractService;
import com.company.daizhang.module.customer.vo.ContractRenewalReminderVO;
import com.company.daizhang.module.customer.vo.ContractVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 服务合同服务实现
 */
@Service
@RequiredArgsConstructor
public class ContractServiceImpl extends ServiceImpl<ServiceContractMapper, ServiceContract> implements ContractService {

    private final CustomerMapper customerMapper;
    private final BillingRecordMapper billingRecordMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final AccountSetAccessService accountSetAccessService;

    @Transactional(readOnly = true)
    @Override
    public PageResult<ContractVO> pageContracts(ContractQueryRequest request) {
        Page<ServiceContract> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<ServiceContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(request.getContractNo()), ServiceContract::getContractNo, request.getContractNo())
               .like(StrUtil.isNotBlank(request.getContractName()), ServiceContract::getContractName, request.getContractName())
               .eq(request.getCustomerId() != null, ServiceContract::getCustomerId, request.getCustomerId())
               .eq(StrUtil.isNotBlank(request.getContractType()), ServiceContract::getContractType, request.getContractType())
               .eq(request.getStatus() != null, ServiceContract::getStatus, request.getStatus())
               .orderByDesc(ServiceContract::getCreateTime);

        // IDOR治理:仅返回当前用户可访问账套下的合同(超级管理员返回null表示不限制)
        Set<Long> accessibleIds = accountSetAccessService.listAccessibleAccountSetIds();
        if (accessibleIds != null) {
            if (accessibleIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize());
            }
            wrapper.in(ServiceContract::getAccountSetId, accessibleIds);
        }

        Page<ServiceContract> result = this.page(page, wrapper);

        List<ContractVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Transactional(readOnly = true)
    @Override
    public List<ContractVO> listContractsByCustomerId(Long customerId) {
        // IDOR治理:校验当前用户对该客户所属账套的访问权(读操作用checkAccess)
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        accountSetAccessService.checkAccess(customer.getAccountSetId());

        LambdaQueryWrapper<ServiceContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceContract::getCustomerId, customerId)
               .orderByDesc(ServiceContract::getCreateTime);

        List<ServiceContract> list = this.list(wrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public ContractVO getContractById(Long id) {
        ServiceContract contract = this.getById(id);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
        // IDOR治理:校验当前用户对该合同所属账套的访问权
        accountSetAccessService.checkAccess(contract.getAccountSetId());
        return convertToVO(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createContract(ContractCreateRequest request) {
        // 检查客户是否存在
        Customer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        // IDOR治理:校验当前用户对该客户所属账套的所有者权限(写操作用checkOwner)
        accountSetAccessService.checkOwner(customer.getAccountSetId());

        // 检查合同编号是否已存在
        LambdaQueryWrapper<ServiceContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceContract::getContractNo, request.getContractNo());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(400, "合同编号已存在");
        }

        ServiceContract contract = new ServiceContract();
        BeanUtil.copyProperties(request, contract);
        // 从客户记录继承账套ID（表中account_set_id为NOT NULL）
        contract.setAccountSetId(customer.getAccountSetId());
        if (contract.getStatus() == null) {
            contract.setStatus(0);
        }
        this.save(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContract(Long id, ContractUpdateRequest request) {
        ServiceContract contract = this.getById(id);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
        // IDOR治理:校验当前用户对该合同所属账套的所有者权限
        accountSetAccessService.checkOwner(contract.getAccountSetId());
        // 注意:ContractUpdateRequest不含contractNo字段,合同编号不可通过update修改,无需查重
        // 保存原状态,防止copyProperties覆盖status绕过状态机(状态变更只能走专用接口)
        Integer originalStatus = contract.getStatus();
        // 使用ignoreNullValue避免Hutool默认拷贝null导致部分更新时字段被置空
        BeanUtil.copyProperties(request, contract, cn.hutool.core.bean.copier.CopyOptions.create().ignoreNullValue());
        contract.setId(id);
        contract.setStatus(originalStatus);
        this.updateById(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateContract(Long id) {
        ServiceContract contract = this.getById(id);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
        // IDOR治理:校验当前用户对该合同所属账套的所有者权限
        accountSetAccessService.checkOwner(contract.getAccountSetId());
        // 激活仅允许草稿(0)状态
        if (contract.getStatus() == null || contract.getStatus() != 0) {
            throw new BusinessException("合同当前状态不允许此操作");
        }
        contract.setStatus(1);
        this.updateById(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeContract(Long id) {
        ServiceContract contract = this.getById(id);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
        // IDOR治理:校验当前用户对该合同所属账套的所有者权限
        accountSetAccessService.checkOwner(contract.getAccountSetId());
        // 完结仅允许执行中(1)状态
        if (contract.getStatus() == null || contract.getStatus() != 1) {
            throw new BusinessException("合同当前状态不允许此操作");
        }

        // 校验是否存在未完成的开票/收款记录,避免完结合同后产生悬空业务记录
        // 1.未作废(status!=2)的开票记录视为未完成
        LambdaQueryWrapper<BillingRecord> billingWrapper = new LambdaQueryWrapper<>();
        billingWrapper.eq(BillingRecord::getContractId, id).ne(BillingRecord::getStatus, 2);
        if (billingRecordMapper.selectCount(billingWrapper) > 0) {
            throw new BusinessException("合同存在未完成的开票/收款记录，无法完结");
        }
        // 2.未被任何开票记录引用的收款记录视为未关联/未完成
        LambdaQueryWrapper<PaymentRecord> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.eq(PaymentRecord::getContractId, id);
        List<PaymentRecord> payments = paymentRecordMapper.selectList(paymentWrapper);
        if (!payments.isEmpty()) {
            List<Long> paymentIds = payments.stream()
                    .map(PaymentRecord::getId)
                    .collect(Collectors.toList());
            LambdaQueryWrapper<BillingRecord> linkedWrapper = new LambdaQueryWrapper<>();
            linkedWrapper.in(BillingRecord::getPaymentRecordId, paymentIds);
            Set<Long> linkedIds = billingRecordMapper.selectList(linkedWrapper).stream()
                    .map(BillingRecord::getPaymentRecordId)
                    .collect(Collectors.toSet());
            if (payments.stream().anyMatch(p -> !linkedIds.contains(p.getId()))) {
                throw new BusinessException("合同存在未完成的开票/收款记录，无法完结");
            }
        }

        contract.setStatus(2);
        this.updateById(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateContract(Long id) {
        ServiceContract contract = this.getById(id);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
        // IDOR治理:校验当前用户对该合同所属账套的所有者权限
        accountSetAccessService.checkOwner(contract.getAccountSetId());
        // 终止允许执行中(1)或已完成(2)状态
        if (contract.getStatus() == null || (contract.getStatus() != 1 && contract.getStatus() != 2)) {
            throw new BusinessException("合同当前状态不允许此操作");
        }
        contract.setStatus(3);
        this.updateById(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContract(Long id) {
        ServiceContract contract = this.getById(id);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
        // IDOR治理:校验当前用户对该合同所属账套的所有者权限
        accountSetAccessService.checkOwner(contract.getAccountSetId());

        // 校验是否存在关联的开票/收款记录,避免删除后产生外键悬空
        LambdaQueryWrapper<BillingRecord> billingWrapper = new LambdaQueryWrapper<>();
        billingWrapper.eq(BillingRecord::getContractId, id);
        if (billingRecordMapper.selectCount(billingWrapper) > 0) {
            throw new BusinessException("合同存在关联的开票记录，无法删除");
        }
        LambdaQueryWrapper<PaymentRecord> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.eq(PaymentRecord::getContractId, id);
        if (paymentRecordMapper.selectCount(paymentWrapper) > 0) {
            throw new BusinessException("合同存在关联的收款记录，无法删除");
        }

        this.removeById(id);
    }

    @Override
    public List<ContractRenewalReminderVO> getRenewalReminders(Integer daysThreshold) {
        if (daysThreshold == null || daysThreshold <= 0) {
            daysThreshold = 30;
        }

        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.plusDays(daysThreshold);

        // 查询到期日在今天到阈值日期之间的执行中合同（包含已过期的）
        LambdaQueryWrapper<ServiceContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(ServiceContract::getEndDate)
               .eq(ServiceContract::getStatus, 1)
               .le(ServiceContract::getEndDate, thresholdDate)
               .orderByAsc(ServiceContract::getEndDate);

        // IDOR治理:仅返回当前用户可访问账套下的合同(超级管理员返回null表示不限制)
        Set<Long> accessibleIds = accountSetAccessService.listAccessibleAccountSetIds();
        if (accessibleIds != null) {
            if (accessibleIds.isEmpty()) {
                return Collections.emptyList();
            }
            wrapper.in(ServiceContract::getAccountSetId, accessibleIds);
        }

        List<ServiceContract> contracts = this.list(wrapper);

        List<ContractRenewalReminderVO> reminders = new ArrayList<>();
        for (ServiceContract contract : contracts) {
            ContractRenewalReminderVO vo = new ContractRenewalReminderVO();
            vo.setContractId(contract.getId());
            vo.setCustomerId(contract.getCustomerId());
            vo.setContractName(contract.getContractName());
            vo.setEndDate(contract.getEndDate());
            vo.setContractAmount(contract.getAmount());

            // 填充客户名称
            if (contract.getCustomerId() != null) {
                Customer customer = customerMapper.selectById(contract.getCustomerId());
                if (customer != null) {
                    vo.setCustomerName(customer.getCustomerName());
                }
            }

            // 计算剩余天数
            int daysRemaining = (int) java.time.temporal.ChronoUnit.DAYS.between(today, contract.getEndDate());
            vo.setDaysRemaining(daysRemaining);

            // 状态：已到期/即将到期
            vo.setStatus(daysRemaining < 0 ? "已到期" : "即将到期");

            reminders.add(vo);
        }

        // 按剩余天数升序排序（已到期在前）
        reminders.sort(Comparator.comparingInt(ContractRenewalReminderVO::getDaysRemaining));

        return reminders;
    }

    private ContractVO convertToVO(ServiceContract contract) {
        ContractVO vo = new ContractVO();
        BeanUtil.copyProperties(contract, vo);

        // 填充客户名称(客户可能已被删除产生孤儿数据,需兜底避免展示null)
        if (contract.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(contract.getCustomerId());
            if (customer != null) {
                vo.setCustomerName(customer.getCustomerName());
            } else {
                vo.setCustomerName("（客户已删除）");
            }
        }

        return vo;
    }
}
