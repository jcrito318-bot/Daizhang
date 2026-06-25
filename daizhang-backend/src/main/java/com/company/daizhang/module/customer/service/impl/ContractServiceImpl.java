package com.company.daizhang.module.customer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.customer.dto.ContractCreateRequest;
import com.company.daizhang.module.customer.dto.ContractQueryRequest;
import com.company.daizhang.module.customer.dto.ContractUpdateRequest;
import com.company.daizhang.module.customer.entity.Customer;
import com.company.daizhang.module.customer.entity.ServiceContract;
import com.company.daizhang.module.customer.mapper.CustomerMapper;
import com.company.daizhang.module.customer.mapper.ServiceContractMapper;
import com.company.daizhang.module.customer.service.ContractService;
import com.company.daizhang.module.customer.vo.ContractRenewalReminderVO;
import com.company.daizhang.module.customer.vo.ContractVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务合同服务实现
 */
@Service
@RequiredArgsConstructor
public class ContractServiceImpl extends ServiceImpl<ServiceContractMapper, ServiceContract> implements ContractService {

    private final CustomerMapper customerMapper;

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

        Page<ServiceContract> result = this.page(page, wrapper);

        List<ContractVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public List<ContractVO> listContractsByCustomerId(Long customerId) {
        LambdaQueryWrapper<ServiceContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceContract::getCustomerId, customerId)
               .orderByDesc(ServiceContract::getCreateTime);

        List<ServiceContract> list = this.list(wrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public ContractVO getContractById(Long id) {
        ServiceContract contract = this.getById(id);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
        }
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

        // 检查合同编号是否已存在
        LambdaQueryWrapper<ServiceContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceContract::getContractNo, request.getContractNo());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(400, "合同编号已存在");
        }

        ServiceContract contract = new ServiceContract();
        BeanUtil.copyProperties(request, contract);
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

        BeanUtil.copyProperties(request, contract);
        this.updateById(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContract(Long id) {
        ServiceContract contract = this.getById(id);
        if (contract == null) {
            throw new BusinessException(404, "合同不存在");
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

        // 查询到期日在今天到阈值日期之间的合同（包含已过期的）
        LambdaQueryWrapper<ServiceContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(ServiceContract::getEndDate)
               .le(ServiceContract::getEndDate, thresholdDate)
               .orderByAsc(ServiceContract::getEndDate);
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

        // 填充客户名称
        if (contract.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(contract.getCustomerId());
            if (customer != null) {
                vo.setCustomerName(customer.getCustomerName());
            }
        }

        return vo;
    }
}
