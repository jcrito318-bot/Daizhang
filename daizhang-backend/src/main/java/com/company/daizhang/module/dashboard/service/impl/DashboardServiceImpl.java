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

        // 1. 查询所有账套（客户）
        LambdaQueryWrapper<AccountSet> asWrapper = new LambdaQueryWrapper<>();
        asWrapper.orderByDesc(AccountSet::getUpdateTime);
        List<AccountSet> accountSets = accountSetMapper.selectList(asWrapper);

        // 2. 查询所有待办服务任务（taskStatus < 2）
        LambdaQueryWrapper<ServiceTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.lt(ServiceTask::getTaskStatus, 2)
                .orderByDesc(ServiceTask::getCreateTime);
        List<ServiceTask> pendingTasks = serviceTaskMapper.selectList(taskWrapper);
        Map<Long, List<ServiceTask>> taskByAccountSet = pendingTasks.stream()
                .filter(t -> t.getAccountSetId() != null)
                .collect(Collectors.groupingBy(ServiceTask::getAccountSetId));

        // 已完成服务任务数（status==2）
        long completedTaskCount = serviceTaskMapper.selectCount(new LambdaQueryWrapper<ServiceTask>()
                .eq(ServiceTask::getTaskStatus, 2));

        // 3. 查询所有未审核凭证（status=0）
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getStatus, 0)
                .orderByDesc(Voucher::getCreateTime);
        List<Voucher> unauditedVouchers = voucherMapper.selectList(voucherWrapper);
        Map<Long, List<Voucher>> voucherByAccountSet = unauditedVouchers.stream()
                .filter(v -> v.getAccountSetId() != null)
                .collect(Collectors.groupingBy(Voucher::getAccountSetId));

        // 4. 查询所有未申报税务（status=0）
        LambdaQueryWrapper<TaxDeclaration> taxWrapper = new LambdaQueryWrapper<>();
        taxWrapper.eq(TaxDeclaration::getStatus, 0)
                .orderByDesc(TaxDeclaration::getCreateTime);
        List<TaxDeclaration> undeclaredTaxes = taxDeclarationMapper.selectList(taxWrapper);
        Map<Long, List<TaxDeclaration>> taxByAccountSet = undeclaredTaxes.stream()
                .filter(t -> t.getAccountSetId() != null)
                .collect(Collectors.groupingBy(TaxDeclaration::getAccountSetId));

        // 5. 构建总览统计
        vo.setSummary(buildSummary(accountSets, pendingTasks, unauditedVouchers, undeclaredTaxes, completedTaskCount));

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

    private DashboardSummary buildSummary(List<AccountSet> accountSets, List<ServiceTask> pendingTasks,
                                          List<Voucher> unauditedVouchers, List<TaxDeclaration> undeclaredTaxes,
                                          long completedTaskCount) {
        DashboardSummary s = new DashboardSummary();
        s.setTotalAccountSets(accountSets.size());
        int active = (int) accountSets.stream().filter(a -> a.getStatus() != null && a.getStatus() == 1).count();
        s.setActiveAccountSets(active);

        int general = 0;
        int small = 0;
        for (AccountSet a : accountSets) {
            String t = a.getTaxpayerType();
            if (t == null) continue;
            if ("2".equals(t) || t.contains("一般")) {
                general++;
            } else if ("1".equals(t) || t.contains("小规模")) {
                small++;
            }
        }
        s.setGeneralTaxpayerCount(general);
        s.setSmallTaxpayerCount(small);

        s.setPendingTaskCount(pendingTasks.size());
        s.setCompletedTaskCount((int) completedTaskCount);
        s.setUnauditedVoucherCount(unauditedVouchers.size());
        s.setUndeclaredTaxCount(undeclaredTaxes.size());

        // 逾期待办：服务任务创建时间超过7天且仍未完成
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        long overdue = pendingTasks.stream()
                .filter(t -> t.getCreateTime() != null && t.getCreateTime().isBefore(threshold))
                .count();
        s.setOverdueTodoCount((int) overdue);
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
