package com.company.daizhang.module.customer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.biz.entity.ServiceTask;
import com.company.daizhang.module.biz.mapper.ServiceTaskMapper;
import com.company.daizhang.module.customer.dto.CustomerCreateRequest;
import com.company.daizhang.module.customer.dto.CustomerQueryRequest;
import com.company.daizhang.module.customer.dto.CustomerUpdateRequest;
import com.company.daizhang.module.customer.entity.Customer;
import com.company.daizhang.module.customer.entity.PaymentRecord;
import com.company.daizhang.module.customer.entity.ServiceContract;
import com.company.daizhang.module.customer.mapper.CustomerMapper;
import com.company.daizhang.module.customer.mapper.PaymentRecordMapper;
import com.company.daizhang.module.customer.mapper.ServiceContractMapper;
import com.company.daizhang.module.customer.service.CustomerService;
import com.company.daizhang.module.customer.vo.ArrearsDetailVO;
import com.company.daizhang.module.customer.vo.ArrearsVO;
import com.company.daizhang.module.customer.vo.CustomerProfileVO;
import com.company.daizhang.module.customer.vo.CustomerVO;
import com.company.daizhang.module.document.entity.InputInvoice;
import com.company.daizhang.module.document.entity.OutputInvoice;
import com.company.daizhang.module.document.mapper.InputInvoiceMapper;
import com.company.daizhang.module.document.mapper.OutputInvoiceMapper;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    private final ServiceContractMapper serviceContractMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final VoucherMapper voucherMapper;
    private final InputInvoiceMapper inputInvoiceMapper;
    private final OutputInvoiceMapper outputInvoiceMapper;
    private final ServiceTaskMapper serviceTaskMapper;

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
        if (customer.getCustomerStatus() == null) {
            customer.setCustomerStatus(1);
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

    @Override
    public PageResult<CustomerVO> pageCustomersByLevel(String customerLevel, Integer customerStatus,
                                                       String industryType, int pageNum, int pageSize) {
        Page<Customer> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(customerLevel), Customer::getCustomerLevel, customerLevel)
               .eq(customerStatus != null, Customer::getCustomerStatus, customerStatus)
               .eq(StrUtil.isNotBlank(industryType), Customer::getIndustryType, industryType)
               .orderByDesc(Customer::getCreateTime);

        Page<Customer> result = this.page(page, wrapper);

        List<CustomerVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public CustomerProfileVO getCustomerProfile(Long customerId) {
        Customer customer = this.getById(customerId);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }

        CustomerProfileVO profile = new CustomerProfileVO();
        profile.setCustomer(convertToVO(customer));

        // 查询合同信息
        LambdaQueryWrapper<ServiceContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.eq(ServiceContract::getCustomerId, customerId);
        List<ServiceContract> contracts = serviceContractMapper.selectList(contractWrapper);

        int activeCount = 0;
        int expiringCount = 0;
        BigDecimal totalContractAmount = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        LocalDate expiringThreshold = today.plusDays(30);
        for (ServiceContract contract : contracts) {
            if (contract.getStatus() != null && contract.getStatus() == 1) {
                activeCount++;
                // 合同金额仅统计执行中的合同(排除草稿/已完成/已终止)
                if (contract.getAmount() != null) {
                    totalContractAmount = totalContractAmount.add(contract.getAmount());
                }
            }
            // 即将到期合同(30天内,仅执行中)
            if (contract.getStatus() != null && contract.getStatus() == 1
                    && contract.getEndDate() != null
                    && !contract.getEndDate().isBefore(today)
                    && !contract.getEndDate().isAfter(expiringThreshold)) {
                expiringCount++;
            }
        }
        profile.setContractCount(contracts.size());
        profile.setActiveContractCount(activeCount);
        profile.setTotalContractAmount(totalContractAmount);
        profile.setExpiringContractCount(expiringCount);

        // 查询收款记录，计算已收款和未收款
        LambdaQueryWrapper<PaymentRecord> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.eq(PaymentRecord::getCustomerId, customerId);
        List<PaymentRecord> payments = paymentRecordMapper.selectList(paymentWrapper);
        BigDecimal totalPaid = BigDecimal.ZERO;
        for (PaymentRecord payment : payments) {
            if (payment.getAmount() != null) {
                totalPaid = totalPaid.add(payment.getAmount());
            }
        }
        profile.setTotalPaidAmount(totalPaid);
        profile.setTotalUnpaidAmount(totalContractAmount.subtract(totalPaid));

        // 查询本月凭证数
        int voucherCount = 0;
        if (customer.getAccountSetId() != null) {
            int year = LocalDate.now().getYear();
            int month = LocalDate.now().getMonthValue();
            LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
            voucherWrapper.eq(Voucher::getAccountSetId, customer.getAccountSetId())
                          .eq(Voucher::getYear, year)
                          .eq(Voucher::getMonth, month);
            voucherCount = Math.toIntExact(voucherMapper.selectCount(voucherWrapper));
        }
        profile.setVoucherCount(voucherCount);

        // 查询本月发票数
        int invoiceCount = 0;
        if (customer.getAccountSetId() != null) {
            int year = LocalDate.now().getYear();
            int month = LocalDate.now().getMonthValue();
            LocalDate monthStart = LocalDate.of(year, month, 1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            LambdaQueryWrapper<InputInvoice> inputWrapper = new LambdaQueryWrapper<>();
            inputWrapper.eq(InputInvoice::getAccountSetId, customer.getAccountSetId())
                       .between(InputInvoice::getInvoiceDate, monthStart, monthEnd);
            invoiceCount += Math.toIntExact(inputInvoiceMapper.selectCount(inputWrapper));

            LambdaQueryWrapper<OutputInvoice> outputWrapper = new LambdaQueryWrapper<>();
            outputWrapper.eq(OutputInvoice::getAccountSetId, customer.getAccountSetId())
                        .between(OutputInvoice::getInvoiceDate, monthStart, monthEnd);
            invoiceCount += Math.toIntExact(outputInvoiceMapper.selectCount(outputWrapper));
        }
        profile.setInvoiceCount(invoiceCount);

        // 查询近期5条收款记录
        LambdaQueryWrapper<PaymentRecord> recentPaymentWrapper = new LambdaQueryWrapper<>();
        recentPaymentWrapper.eq(PaymentRecord::getCustomerId, customerId)
                            .orderByDesc(PaymentRecord::getCreateTime)
                            .last("LIMIT 5");
        List<PaymentRecord> recentPayments = paymentRecordMapper.selectList(recentPaymentWrapper);
        profile.setRecentPayments(recentPayments.stream()
                .map(p -> BeanUtil.beanToMap(p))
                .collect(Collectors.toList()));

        // 查询近期5条合同
        LambdaQueryWrapper<ServiceContract> recentContractWrapper = new LambdaQueryWrapper<>();
        recentContractWrapper.eq(ServiceContract::getCustomerId, customerId)
                             .orderByDesc(ServiceContract::getCreateTime)
                             .last("LIMIT 5");
        List<ServiceContract> recentContracts = serviceContractMapper.selectList(recentContractWrapper);
        profile.setRecentContracts(recentContracts.stream()
                .map(c -> BeanUtil.beanToMap(c))
                .collect(Collectors.toList()));

        // 服务信息(服务流程进度)
        profile.setServiceProgress(buildServiceProgress(customer));

        // 风险信息(账龄、欠款风险)
        profile.setRiskInfo(buildRiskInfo(customer, totalContractAmount, totalPaid));

        return profile;
    }

    /**
     * 构建服务流程进度
     */
    private CustomerProfileVO.ServiceProgressVO buildServiceProgress(Customer customer) {
        CustomerProfileVO.ServiceProgressVO progress = new CustomerProfileVO.ServiceProgressVO();
        int year = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();
        progress.setCurrentPeriod(year + "-" + String.format("%02d", month));

        LambdaQueryWrapper<ServiceTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(ServiceTask::getCustomerId, customer.getId())
                   .eq(ServiceTask::getYear, year)
                   .eq(ServiceTask::getMonth, month);
        List<ServiceTask> tasks = serviceTaskMapper.selectList(taskWrapper);

        int total = tasks.size();
        int completed = 0;
        int inProgress = 0;
        int pending = 0;
        String currentNodeName = null;
        for (ServiceTask task : tasks) {
            if (task.getTaskStatus() != null) {
                if (task.getTaskStatus() == 2) {
                    completed++;
                } else if (task.getTaskStatus() == 1) {
                    inProgress++;
                    if (currentNodeName == null) {
                        currentNodeName = task.getNodeName();
                    }
                } else {
                    pending++;
                    if (currentNodeName == null) {
                        currentNodeName = task.getNodeName();
                    }
                }
            } else {
                pending++;
            }
        }
        progress.setTotalTaskCount(total);
        progress.setCompletedTaskCount(completed);
        progress.setInProgressTaskCount(inProgress);
        progress.setPendingTaskCount(pending);
        progress.setCurrentNodeName(currentNodeName);
        if (total > 0) {
            progress.setProgressPercent(BigDecimal.valueOf(completed)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        } else {
            progress.setProgressPercent(BigDecimal.ZERO);
        }
        return progress;
    }

    /**
     * 构建风险信息
     */
    private CustomerProfileVO.RiskInfoVO buildRiskInfo(Customer customer, BigDecimal totalContractAmount,
                                                        BigDecimal totalPaid) {
        CustomerProfileVO.RiskInfoVO riskInfo = new CustomerProfileVO.RiskInfoVO();
        BigDecimal arrearsAmount = totalContractAmount.subtract(totalPaid);
        if (arrearsAmount.compareTo(BigDecimal.ZERO) < 0) {
            arrearsAmount = BigDecimal.ZERO;
        }
        riskInfo.setArrearsAmount(arrearsAmount);

        // 计算逾期月数（取所有合同中最大逾期月数）
        LambdaQueryWrapper<ServiceContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.eq(ServiceContract::getCustomerId, customer.getId());
        List<ServiceContract> contracts = serviceContractMapper.selectList(contractWrapper);
        int maxOverdueMonths = 0;
        LocalDate earliestEndDate = null;
        for (ServiceContract contract : contracts) {
            int overdue = calcOverdueMonths(contract.getEndDate());
            if (overdue > maxOverdueMonths) {
                maxOverdueMonths = overdue;
            }
            if (contract.getEndDate() != null) {
                if (earliestEndDate == null || contract.getEndDate().isBefore(earliestEndDate)) {
                    earliestEndDate = contract.getEndDate();
                }
            }
        }
        riskInfo.setOverdueMonths(maxOverdueMonths);
        riskInfo.setRiskLevel(calcRiskLevel(maxOverdueMonths));

        // 账龄(天) - 从最早合同到期日到现在
        if (earliestEndDate != null && earliestEndDate.isBefore(LocalDate.now())) {
            riskInfo.setAccountAgeDays((int) java.time.temporal.ChronoUnit.DAYS.between(earliestEndDate, LocalDate.now()));
        } else {
            riskInfo.setAccountAgeDays(0);
        }

        // 风险描述
        riskInfo.setRiskDescription(buildRiskDescription(arrearsAmount, maxOverdueMonths));
        return riskInfo;
    }

    /**
     * 构建风险描述
     */
    private String buildRiskDescription(BigDecimal arrearsAmount, int overdueMonths) {
        if (arrearsAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "无欠款";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("欠款金额: ").append(arrearsAmount).append("元");
        if (overdueMonths > 6) {
            sb.append("，逾期超过6个月，存在较高坏账风险");
        } else if (overdueMonths > 3) {
            sb.append("，逾期超过3个月，建议加强催收");
        } else if (overdueMonths > 0) {
            sb.append("，已逾期").append(overdueMonths).append("个月");
        } else {
            sb.append("，暂未逾期");
        }
        return sb.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomerStatus(Long customerId, Integer status) {
        Customer customer = this.getById(customerId);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        customer.setCustomerStatus(status);
        this.updateById(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomerLevel(Long customerId, String level) {
        Customer customer = this.getById(customerId);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        customer.setCustomerLevel(level);
        this.updateById(customer);
    }

    @Override
    public List<Map<String, Object>> getCustomerStatistics() {
        List<Map<String, Object>> result = new ArrayList<>();

        // 按客户等级分组
        QueryWrapper<Customer> levelWrapper = new QueryWrapper<>();
        levelWrapper.select("IFNULL(customer_level,'未分类') as groupKey, count(*) as total")
                   .groupBy("customer_level");
        List<Map<String, Object>> levelStats = this.listMaps(levelWrapper);
        for (Map<String, Object> map : levelStats) {
            map.put("groupType", "level");
            result.add(map);
        }

        // 按客户状态分组
        QueryWrapper<Customer> statusWrapper = new QueryWrapper<>();
        statusWrapper.select("IFNULL(customer_status,-1) as groupKey, count(*) as total")
                    .groupBy("customer_status");
        List<Map<String, Object>> statusStats = this.listMaps(statusWrapper);
        for (Map<String, Object> map : statusStats) {
            map.put("groupType", "status");
            result.add(map);
        }

        // 按行业类型分组
        QueryWrapper<Customer> industryWrapper = new QueryWrapper<>();
        industryWrapper.select("IFNULL(industry_type,'未分类') as groupKey, count(*) as total")
                     .groupBy("industry_type");
        List<Map<String, Object>> industryStats = this.listMaps(industryWrapper);
        for (Map<String, Object> map : industryStats) {
            map.put("groupType", "industry");
            result.add(map);
        }

        return result;
    }

    @Override
    public PageResult<ArrearsVO> pageArrears(String customerName, int pageNum, int pageSize) {
        // 查询客户
        LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
        customerWrapper.like(StrUtil.isNotBlank(customerName), Customer::getCustomerName, customerName)
                       .orderByDesc(Customer::getCreateTime);
        List<Customer> customers = this.list(customerWrapper);

        List<ArrearsVO> allArrears = new ArrayList<>();
        for (Customer customer : customers) {
            ArrearsVO arrears = buildArrearsVO(customer);
            // 仅包含有欠款的客户
            if (arrears.getTotalArrearsAmount() != null
                    && arrears.getTotalArrearsAmount().compareTo(BigDecimal.ZERO) > 0) {
                allArrears.add(arrears);
            }
        }

        // 按欠款金额降序排序
        allArrears.sort((a, b) -> b.getTotalArrearsAmount().compareTo(a.getTotalArrearsAmount()));

        // 手动分页
        int total = allArrears.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<ArrearsVO> pageList = fromIndex < total
                ? allArrears.subList(fromIndex, toIndex)
                : new ArrayList<>();

        return new PageResult<>(pageList, (long) total, pageNum, pageSize);
    }

    @Override
    public ArrearsVO getArrearsDetail(Long customerId) {
        Customer customer = this.getById(customerId);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        return buildArrearsVO(customer);
    }

    @Override
    public List<Map<String, Object>> getCollectionReminders() {
        List<Map<String, Object>> result = new ArrayList<>();

        // 查询所有客户
        List<Customer> customers = this.list();
        LocalDate today = LocalDate.now();

        for (Customer customer : customers) {
            ArrearsVO arrears = buildArrearsVO(customer);
            if (arrears.getTotalArrearsAmount() == null
                    || arrears.getTotalArrearsAmount().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Map<String, Object> reminder = new HashMap<>();
            reminder.put("customerId", customer.getId());
            reminder.put("customerName", customer.getCustomerName());
            reminder.put("contactPerson", customer.getContactPerson());
            reminder.put("contactPhone", customer.getContactPhone());
            reminder.put("totalArrearsAmount", arrears.getTotalArrearsAmount());
            reminder.put("overdueMonths", arrears.getOverdueMonths());
            reminder.put("riskLevel", arrears.getRiskLevel());
            reminder.put("reminderDate", today);
            // 催收建议
            reminder.put("suggestion", buildCollectionSuggestion(arrears));
            result.add(reminder);
        }

        // 按风险等级和欠款金额排序
        result.sort((a, b) -> {
            String riskA = (String) a.get("riskLevel");
            String riskB = (String) b.get("riskLevel");
            int riskOrderA = getRiskOrder(riskA);
            int riskOrderB = getRiskOrder(riskB);
            if (riskOrderA != riskOrderB) {
                return Integer.compare(riskOrderB, riskOrderA);
            }
            BigDecimal amountA = (BigDecimal) a.get("totalArrearsAmount");
            BigDecimal amountB = (BigDecimal) b.get("totalArrearsAmount");
            return amountB.compareTo(amountA);
        });

        return result;
    }

    /**
     * 构建欠款VO
     */
    private ArrearsVO buildArrearsVO(Customer customer) {
        ArrearsVO vo = new ArrearsVO();
        vo.setCustomerId(customer.getId());
        vo.setCustomerName(customer.getCustomerName());

        // 查询客户合同(仅执行中,排除草稿0/已完成2/已终止3,避免虚增欠款)
        LambdaQueryWrapper<ServiceContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.eq(ServiceContract::getCustomerId, customer.getId())
                       .eq(ServiceContract::getStatus, 1);
        List<ServiceContract> contracts = serviceContractMapper.selectList(contractWrapper);

        BigDecimal totalContractAmount = BigDecimal.ZERO;
        BigDecimal totalPaidAmount = BigDecimal.ZERO;
        int maxOverdueMonths = 0;
        List<ArrearsDetailVO> details = new ArrayList<>();

        // 查询客户所有收款记录
        LambdaQueryWrapper<PaymentRecord> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.eq(PaymentRecord::getCustomerId, customer.getId());
        List<PaymentRecord> allPayments = paymentRecordMapper.selectList(paymentWrapper);
        // 按合同ID分组收款
        Map<Long, BigDecimal> paidByContract = new HashMap<>();
        for (PaymentRecord payment : allPayments) {
            BigDecimal amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
            paidByContract.merge(payment.getContractId(), amount, BigDecimal::add);
            totalPaidAmount = totalPaidAmount.add(amount);
        }

        for (ServiceContract contract : contracts) {
            BigDecimal contractAmount = contract.getAmount() != null ? contract.getAmount() : BigDecimal.ZERO;
            totalContractAmount = totalContractAmount.add(contractAmount);

            BigDecimal paidAmount = paidByContract.getOrDefault(contract.getId(), BigDecimal.ZERO);
            BigDecimal arrearsAmount = contractAmount.subtract(paidAmount);

            // 仅当有欠款时加入明细
            if (arrearsAmount.compareTo(BigDecimal.ZERO) > 0) {
                ArrearsDetailVO detail = new ArrearsDetailVO();
                detail.setContractId(contract.getId());
                detail.setContractNo(contract.getContractNo());
                detail.setContractName(contract.getContractName());
                detail.setContractAmount(contractAmount);
                detail.setPaidAmount(paidAmount);
                detail.setArrearsAmount(arrearsAmount);
                detail.setEndDate(contract.getEndDate());
                detail.setStatus(contract.getStatus());

                int overdueMonths = calcOverdueMonths(contract.getEndDate());
                detail.setOverdueMonths(overdueMonths);
                if (overdueMonths > maxOverdueMonths) {
                    maxOverdueMonths = overdueMonths;
                }
                details.add(detail);
            }
        }

        vo.setTotalContractAmount(totalContractAmount);
        vo.setTotalPaidAmount(totalPaidAmount);
        vo.setTotalArrearsAmount(totalContractAmount.subtract(totalPaidAmount));
        vo.setOverdueMonths(maxOverdueMonths);
        vo.setRiskLevel(calcRiskLevel(maxOverdueMonths));
        vo.setDetails(details);
        return vo;
    }

    /**
     * 计算逾期月数（按合同到期日计算）
     */
    private int calcOverdueMonths(LocalDate endDate) {
        if (endDate == null) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(endDate) || today.isEqual(endDate)) {
            return 0;
        }
        return (today.getYear() - endDate.getYear()) * 12 + (today.getMonthValue() - endDate.getMonthValue());
    }

    /**
     * 计算风险等级
     * 逾期>6月为高风险, >3月为中风险, 否则低风险
     */
    private String calcRiskLevel(int overdueMonths) {
        if (overdueMonths > 6) {
            return "高风险";
        } else if (overdueMonths > 3) {
            return "中风险";
        }
        return "低风险";
    }

    /**
     * 获取风险等级排序值
     */
    private int getRiskOrder(String riskLevel) {
        if ("高风险".equals(riskLevel)) {
            return 3;
        } else if ("中风险".equals(riskLevel)) {
            return 2;
        }
        return 1;
    }

    /**
     * 构建催收建议
     */
    private String buildCollectionSuggestion(ArrearsVO arrears) {
        int overdueMonths = arrears.getOverdueMonths() != null ? arrears.getOverdueMonths() : 0;
        if (overdueMonths > 6) {
            return "逾期超过6个月，建议立即电话催收并发出正式催款函，必要时启动法律程序";
        } else if (overdueMonths > 3) {
            return "逾期超过3个月，建议电话联系客户确认付款计划，并发送催款邮件";
        } else if (overdueMonths > 0) {
            return "已逾期，建议发送催款短信提醒客户尽快付款";
        }
        return "存在欠款，建议定期跟进收款进度";
    }

    private CustomerVO convertToVO(Customer customer) {
        CustomerVO vo = new CustomerVO();
        BeanUtil.copyProperties(customer, vo);
        return vo;
    }
}
