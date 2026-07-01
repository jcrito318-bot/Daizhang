package com.company.daizhang.module.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.mapper.AccountSetMapper;
import com.company.daizhang.module.biz.entity.ServiceTask;
import com.company.daizhang.module.biz.mapper.ServiceTaskMapper;
import com.company.daizhang.module.dashboard.service.DashboardService;
import com.company.daizhang.module.dashboard.vo.CustomerSummaryVO;
import com.company.daizhang.module.dashboard.vo.DashboardSummary;
import com.company.daizhang.module.dashboard.vo.DashboardVO;
import com.company.daizhang.module.dashboard.vo.TodoItemVO;
import com.company.daizhang.module.tax.entity.TaxDeclaration;
import com.company.daizhang.module.tax.mapper.TaxDeclarationMapper;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 代账公司运营看板服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AccountSetMapper accountSetMapper;
    private final ServiceTaskMapper serviceTaskMapper;
    private final VoucherMapper voucherMapper;
    private final TaxDeclarationMapper taxDeclarationMapper;

    private static final String TODO_SERVICE_TASK = "SERVICE_TASK";
    private static final String TODO_TAX_DECLARATION = "TAX_DECLARATION";
    private static final String TODO_VOUCHER_AUDIT = "VOUCHER_AUDIT";

    @Override
    @Transactional(readOnly = true)
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();

        // 1. 查询账套（限量500防止全量加载OOM）
        LambdaQueryWrapper<AccountSet> asWrapper = new LambdaQueryWrapper<>();
        asWrapper.orderByDesc(AccountSet::getUpdateTime)
                 .last("LIMIT 500");
        List<AccountSet> accountSets = accountSetMapper.selectList(asWrapper);

        // 2. 查询待办服务任务（taskStatus < 2,限量500防止OOM）
        LambdaQueryWrapper<ServiceTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.lt(ServiceTask::getTaskStatus, 2)
                .orderByDesc(ServiceTask::getCreateTime)
                .last("LIMIT 500");
        List<ServiceTask> pendingTasks = serviceTaskMapper.selectList(taskWrapper);
        Map<Long, List<ServiceTask>> taskByAccountSet = pendingTasks.stream()
                .filter(t -> t.getAccountSetId() != null)
                .collect(Collectors.groupingBy(ServiceTask::getAccountSetId));

        // 已完成服务任务数（status==2）用count统计,不加载明细
        long completedTaskCount = serviceTaskMapper.selectCount(new LambdaQueryWrapper<ServiceTask>()
                .eq(ServiceTask::getTaskStatus, 2));

        // 3. 查询未审核凭证（status=0,限量500防止OOM）
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getStatus, 0)
                .orderByDesc(Voucher::getCreateTime)
                .last("LIMIT 500");
        List<Voucher> unauditedVouchers = voucherMapper.selectList(voucherWrapper);
        Map<Long, List<Voucher>> voucherByAccountSet = unauditedVouchers.stream()
                .filter(v -> v.getAccountSetId() != null)
                .collect(Collectors.groupingBy(Voucher::getAccountSetId));

        // 4. 查询未申报税务（status=0,限量500防止OOM）
        LambdaQueryWrapper<TaxDeclaration> taxWrapper = new LambdaQueryWrapper<>();
        taxWrapper.eq(TaxDeclaration::getStatus, 0)
                .orderByDesc(TaxDeclaration::getCreateTime)
                .last("LIMIT 500");
        List<TaxDeclaration> undeclaredTaxes = taxDeclarationMapper.selectList(taxWrapper);
        Map<Long, List<TaxDeclaration>> taxByAccountSet = undeclaredTaxes.stream()
                .filter(t -> t.getAccountSetId() != null)
                .collect(Collectors.groupingBy(TaxDeclaration::getAccountSetId));

        // 5. 构建总览统计
        // 注意:列表查询带LIMIT 500仅用于明细展示,统计数必须用独立的selectCount(不加limit),否则超过500时计数失真
        // 账套总数(不限制)
        long totalAccountSets = accountSetMapper.selectCount(new LambdaQueryWrapper<>());
        // 启用账套数(status=1)
        long activeAccountSets = accountSetMapper.selectCount(new LambdaQueryWrapper<AccountSet>()
                .eq(AccountSet::getStatus, 1));
        // 一般纳税人:taxpayerType="2"或包含"一般"
        long generalTaxpayerCount = accountSetMapper.selectCount(new LambdaQueryWrapper<AccountSet>()
                .and(w -> w.eq(AccountSet::getTaxpayerType, "2")
                        .or().like(AccountSet::getTaxpayerType, "一般")));
        // 小规模纳税人:taxpayerType="1"或包含"小规模"
        long smallTaxpayerCount = accountSetMapper.selectCount(new LambdaQueryWrapper<AccountSet>()
                .and(w -> w.eq(AccountSet::getTaxpayerType, "1")
                        .or().like(AccountSet::getTaxpayerType, "小规模")));
        // 待办任务数(taskStatus<2,不限制)
        long pendingTaskCount = serviceTaskMapper.selectCount(new LambdaQueryWrapper<ServiceTask>()
                .lt(ServiceTask::getTaskStatus, 2));
        // 逾期待办:状态未完成且创建时间超过7天
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        long overdueTodoCount = serviceTaskMapper.selectCount(new LambdaQueryWrapper<ServiceTask>()
                .lt(ServiceTask::getTaskStatus, 2)
                .lt(ServiceTask::getCreateTime, threshold));
        // 未审核凭证数(status=0,不限制)
        long unauditedVoucherCount = voucherMapper.selectCount(new LambdaQueryWrapper<Voucher>()
                .eq(Voucher::getStatus, 0));
        // 未申报税务数(status=0,不限制)
        long undeclaredTaxCount = taxDeclarationMapper.selectCount(new LambdaQueryWrapper<TaxDeclaration>()
                .eq(TaxDeclaration::getStatus, 0));

        vo.setSummary(buildSummary(totalAccountSets, activeAccountSets, generalTaxpayerCount, smallTaxpayerCount,
                pendingTaskCount, completedTaskCount, unauditedVoucherCount, undeclaredTaxCount, overdueTodoCount));

        // 6. 构建客户列表摘要
        vo.setCustomers(buildCustomerSummaries(accountSets, taskByAccountSet, voucherByAccountSet, taxByAccountSet));

        // 7. 构建待办看板（合并各类型，按创建时间倒序，限制100条）
        List<TodoItemVO> todoItems = new ArrayList<>();
        todoItems.addAll(buildTaskTodos(pendingTasks, accountSets));
        todoItems.addAll(buildVoucherTodos(unauditedVouchers, accountSets));
        todoItems.addAll(buildTaxTodos(undeclaredTaxes, accountSets));
        todoItems.sort(Comparator.comparing(TodoItemVO::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        if (todoItems.size() > 100) {
            todoItems = todoItems.subList(0, 100);
        }
        vo.setTodoItems(todoItems);

        return vo;
    }

    private DashboardSummary buildSummary(long totalAccountSets, long activeAccountSets,
                                          long generalTaxpayerCount, long smallTaxpayerCount,
                                          long pendingTaskCount, long completedTaskCount,
                                          long unauditedVoucherCount, long undeclaredTaxCount,
                                          long overdueTodoCount) {
        DashboardSummary s = new DashboardSummary();
        // 统计数均由独立的selectCount查询得出(不加LIMIT),避免超过500条时计数失真
        s.setTotalAccountSets((int) totalAccountSets);
        s.setActiveAccountSets((int) activeAccountSets);
        s.setGeneralTaxpayerCount((int) generalTaxpayerCount);
        s.setSmallTaxpayerCount((int) smallTaxpayerCount);
        s.setPendingTaskCount((int) pendingTaskCount);
        s.setCompletedTaskCount((int) completedTaskCount);
        s.setUnauditedVoucherCount((int) unauditedVoucherCount);
        s.setUndeclaredTaxCount((int) undeclaredTaxCount);
        s.setOverdueTodoCount((int) overdueTodoCount);
        return s;
    }

    private List<CustomerSummaryVO> buildCustomerSummaries(List<AccountSet> accountSets,
                                                           Map<Long, List<ServiceTask>> taskByAccountSet,
                                                           Map<Long, List<Voucher>> voucherByAccountSet,
                                                           Map<Long, List<TaxDeclaration>> taxByAccountSet) {
        List<CustomerSummaryVO> list = new ArrayList<>();
        for (AccountSet a : accountSets) {
            CustomerSummaryVO c = new CustomerSummaryVO();
            c.setAccountSetId(a.getId());
            c.setAccountSetCode(a.getCode());
            c.setAccountSetName(a.getName());
            c.setCompanyName(a.getCompanyName());
            c.setTaxpayerType(a.getTaxpayerType());
            c.setTaxpayerTypeDesc(convertTaxpayerTypeDesc(a.getTaxpayerType()));
            c.setIndustryType(a.getIndustryType());
            c.setStatus(a.getStatus());
            c.setStatusDesc(a.getStatus() != null && a.getStatus() == 1 ? "启用" : "禁用");
            c.setContactPerson(a.getContactPerson());
            c.setContactPhone(a.getContactPhone());

            List<ServiceTask> tasks = taskByAccountSet.get(a.getId());
            c.setPendingTaskCount(tasks != null ? tasks.size() : 0);

            List<Voucher> vouchers = voucherByAccountSet.get(a.getId());
            c.setUnauditedVoucherCount(vouchers != null ? vouchers.size() : 0);

            List<TaxDeclaration> taxes = taxByAccountSet.get(a.getId());
            c.setUndeclaredTaxCount(taxes != null ? taxes.size() : 0);

            c.setLastUpdateTime(a.getUpdateTime());
            list.add(c);
        }
        return list;
    }

    private List<TodoItemVO> buildTaskTodos(List<ServiceTask> tasks, List<AccountSet> accountSets) {
        Map<Long, AccountSet> asMap = accountSets.stream()
                .collect(Collectors.toMap(AccountSet::getId, a -> a, (x, y) -> x));
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        List<TodoItemVO> list = new ArrayList<>();
        for (ServiceTask t : tasks) {
            TodoItemVO todo = new TodoItemVO();
            todo.setTodoType(TODO_SERVICE_TASK);
            todo.setTodoTypeDesc("服务任务");
            todo.setRefId(t.getId());
            todo.setAccountSetId(t.getAccountSetId());
            AccountSet as = asMap.get(t.getAccountSetId());
            todo.setCompanyName(as != null ? as.getCompanyName() : null);
            todo.setTitle(t.getNodeName());
            todo.setYear(t.getYear());
            todo.setMonth(t.getMonth());
            todo.setAssigneeName(t.getAssigneeName());
            todo.setStatus(t.getTaskStatus());
            todo.setStatusDesc(convertTaskStatusDesc(t.getTaskStatus()));
            todo.setCreateTime(t.getCreateTime());
            todo.setOverdue(t.getCreateTime() != null && t.getCreateTime().isBefore(threshold));
            list.add(todo);
        }
        return list;
    }

    private List<TodoItemVO> buildVoucherTodos(List<Voucher> vouchers, List<AccountSet> accountSets) {
        Map<Long, AccountSet> asMap = accountSets.stream()
                .collect(Collectors.toMap(AccountSet::getId, a -> a, (x, y) -> x));
        List<TodoItemVO> list = new ArrayList<>();
        for (Voucher v : vouchers) {
            TodoItemVO todo = new TodoItemVO();
            todo.setTodoType(TODO_VOUCHER_AUDIT);
            todo.setTodoTypeDesc("凭证审核");
            todo.setRefId(v.getId());
            todo.setAccountSetId(v.getAccountSetId());
            AccountSet as = asMap.get(v.getAccountSetId());
            todo.setCompanyName(as != null ? as.getCompanyName() : null);
            todo.setTitle("凭证 " + (v.getVoucherNo() != null ? v.getVoucherNo() : ""));
            todo.setYear(v.getYear());
            todo.setMonth(v.getMonth());
            todo.setStatus(v.getStatus());
            todo.setStatusDesc("未审核");
            todo.setCreateTime(v.getCreateTime());
            todo.setOverdue(false);
            list.add(todo);
        }
        return list;
    }

    private List<TodoItemVO> buildTaxTodos(List<TaxDeclaration> taxes, List<AccountSet> accountSets) {
        Map<Long, AccountSet> asMap = accountSets.stream()
                .collect(Collectors.toMap(AccountSet::getId, a -> a, (x, y) -> x));
        List<TodoItemVO> list = new ArrayList<>();
        for (TaxDeclaration t : taxes) {
            TodoItemVO todo = new TodoItemVO();
            todo.setTodoType(TODO_TAX_DECLARATION);
            todo.setTodoTypeDesc("税务申报");
            todo.setRefId(t.getId());
            todo.setAccountSetId(t.getAccountSetId());
            AccountSet as = asMap.get(t.getAccountSetId());
            todo.setCompanyName(as != null ? as.getCompanyName() : null);
            todo.setTitle(t.getTaxType() != null ? t.getTaxType() + "申报" : "税务申报");
            todo.setYear(t.getYear());
            todo.setMonth(t.getMonth());
            todo.setStatus(t.getStatus());
            todo.setStatusDesc("未申报");
            todo.setCreateTime(t.getCreateTime());
            todo.setOverdue(false);
            list.add(todo);
        }
        return list;
    }

    private String convertTaxpayerTypeDesc(String type) {
        if (type == null) return "";
        if ("2".equals(type) || type.contains("一般")) return "一般纳税人";
        if ("1".equals(type) || type.contains("小规模")) return "小规模纳税人";
        return type;
    }

    private String convertTaskStatusDesc(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "待处理";
            case 1: return "进行中";
            case 2: return "已完成";
            default: return String.valueOf(status);
        }
    }
}
