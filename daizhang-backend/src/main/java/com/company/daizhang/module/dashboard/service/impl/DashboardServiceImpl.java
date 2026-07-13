package com.company.daizhang.module.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.mapper.AccountSetMapper;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
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

    private final AccountSetAccessService accountSetAccessService;
    private final AccountSetMapper accountSetMapper;
    private final ServiceTaskMapper serviceTaskMapper;
    private final VoucherMapper voucherMapper;
    private final TaxDeclarationMapper taxDeclarationMapper;

    private static final String TODO_SERVICE_TASK = "SERVICE_TASK";
    private static final String TODO_TAX_DECLARATION = "TAX_DECLARATION";
    private static final String TODO_VOUCHER_AUDIT = "VOUCHER_AUDIT";
    private static final String QUERY_LIMIT = "LIMIT 500";

    @Override
    @Transactional(readOnly = true)
    public DashboardVO getDashboard(Long accountSetId) {
        DashboardVO vo = new DashboardVO();

        // 数据级隔离:仅查询当前用户可访问账套及其关联数据(超级管理员返回null表示不限制)
        Set<Long> accessibleIds = accountSetAccessService.listAccessibleAccountSetIds();
        // 可选账套过滤:若指定了 accountSetId,则在可访问范围内进一步限定到该账套,
        // 避免前端传入的 accountSetId 被静默丢弃。无权访问该账套时返回空结果集。
        if (accountSetId != null && accountSetId > 0) {
            if (accessibleIds == null || accessibleIds.contains(accountSetId)) {
                accessibleIds = Collections.singleton(accountSetId);
            } else {
                accessibleIds = Collections.emptySet();
            }
        }

        // 1. 查询账套（限量500防止全量加载OOM）
        LambdaQueryWrapper<AccountSet> asWrapper = new LambdaQueryWrapper<>();
        applyAccessFilter(asWrapper, AccountSet::getId, accessibleIds);
        asWrapper.orderByDesc(AccountSet::getUpdateTime)
                 .last(QUERY_LIMIT);
        List<AccountSet> accountSets = accountSetMapper.selectList(asWrapper);

        // 2. 查询待办服务任务（taskStatus < 2,限量500防止OOM）
        LambdaQueryWrapper<ServiceTask> taskWrapper = new LambdaQueryWrapper<>();
        applyAccessFilter(taskWrapper, ServiceTask::getAccountSetId, accessibleIds);
        taskWrapper.lt(ServiceTask::getTaskStatus, 2)
                .orderByDesc(ServiceTask::getCreateTime)
                .last(QUERY_LIMIT);
        List<ServiceTask> pendingTasks = serviceTaskMapper.selectList(taskWrapper);
        Map<Long, List<ServiceTask>> taskByAccountSet = pendingTasks.stream()
                .filter(t -> t.getAccountSetId() != null)
                .collect(Collectors.groupingBy(ServiceTask::getAccountSetId));

        // 已完成服务任务数（status==2）用count统计,不加载明细
        LambdaQueryWrapper<ServiceTask> completedTaskWrapper = new LambdaQueryWrapper<ServiceTask>()
                .eq(ServiceTask::getTaskStatus, 2);
        applyAccessFilter(completedTaskWrapper, ServiceTask::getAccountSetId, accessibleIds);
        long completedTaskCount = serviceTaskMapper.selectCount(completedTaskWrapper);

        // 3. 查询未审核凭证（status=0,限量500防止OOM）
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        applyAccessFilter(voucherWrapper, Voucher::getAccountSetId, accessibleIds);
        voucherWrapper.eq(Voucher::getStatus, 0)
                .orderByDesc(Voucher::getCreateTime)
                .last(QUERY_LIMIT);
        List<Voucher> unauditedVouchers = voucherMapper.selectList(voucherWrapper);
        Map<Long, List<Voucher>> voucherByAccountSet = unauditedVouchers.stream()
                .filter(v -> v.getAccountSetId() != null)
                .collect(Collectors.groupingBy(Voucher::getAccountSetId));

        // 4. 查询未申报税务（status=0,限量500防止OOM）
        LambdaQueryWrapper<TaxDeclaration> taxWrapper = new LambdaQueryWrapper<>();
        applyAccessFilter(taxWrapper, TaxDeclaration::getAccountSetId, accessibleIds);
        taxWrapper.eq(TaxDeclaration::getStatus, 0)
                .orderByDesc(TaxDeclaration::getCreateTime)
                .last(QUERY_LIMIT);
        List<TaxDeclaration> undeclaredTaxes = taxDeclarationMapper.selectList(taxWrapper);
        Map<Long, List<TaxDeclaration>> taxByAccountSet = undeclaredTaxes.stream()
                .filter(t -> t.getAccountSetId() != null)
                .collect(Collectors.groupingBy(TaxDeclaration::getAccountSetId));

        // 5. 构建总览统计
        // 注意:列表查询带LIMIT 500仅用于明细展示,统计数必须用独立的selectCount(不加limit),否则超过500时计数失真
        // 账套总数(不限制)
        LambdaQueryWrapper<AccountSet> totalAsWrapper = new LambdaQueryWrapper<>();
        applyAccessFilter(totalAsWrapper, AccountSet::getId, accessibleIds);
        long totalAccountSets = accountSetMapper.selectCount(totalAsWrapper);
        // 启用账套数(status=1)
        LambdaQueryWrapper<AccountSet> activeAsWrapper = new LambdaQueryWrapper<AccountSet>()
                .eq(AccountSet::getStatus, 1);
        applyAccessFilter(activeAsWrapper, AccountSet::getId, accessibleIds);
        long activeAccountSets = accountSetMapper.selectCount(activeAsWrapper);
        // 一般纳税人:taxpayerType="2"或包含"一般"
        LambdaQueryWrapper<AccountSet> generalWrapper = new LambdaQueryWrapper<AccountSet>()
                .and(w -> w.eq(AccountSet::getTaxpayerType, "2")
                        .or().like(AccountSet::getTaxpayerType, "一般"));
        applyAccessFilter(generalWrapper, AccountSet::getId, accessibleIds);
        long generalTaxpayerCount = accountSetMapper.selectCount(generalWrapper);
        // 小规模纳税人:taxpayerType="1"或包含"小规模"
        LambdaQueryWrapper<AccountSet> smallWrapper = new LambdaQueryWrapper<AccountSet>()
                .and(w -> w.eq(AccountSet::getTaxpayerType, "1")
                        .or().like(AccountSet::getTaxpayerType, "小规模"));
        applyAccessFilter(smallWrapper, AccountSet::getId, accessibleIds);
        long smallTaxpayerCount = accountSetMapper.selectCount(smallWrapper);
        // 待办任务数(taskStatus<2,不限制)
        LambdaQueryWrapper<ServiceTask> pendingTaskCountWrapper = new LambdaQueryWrapper<ServiceTask>()
                .lt(ServiceTask::getTaskStatus, 2);
        applyAccessFilter(pendingTaskCountWrapper, ServiceTask::getAccountSetId, accessibleIds);
        long pendingTaskCount = serviceTaskMapper.selectCount(pendingTaskCountWrapper);
        // 逾期待办:状态未完成且创建时间超过7天
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        LambdaQueryWrapper<ServiceTask> overdueWrapper = new LambdaQueryWrapper<ServiceTask>()
                .lt(ServiceTask::getTaskStatus, 2)
                .lt(ServiceTask::getCreateTime, threshold);
        applyAccessFilter(overdueWrapper, ServiceTask::getAccountSetId, accessibleIds);
        long overdueTodoCount = serviceTaskMapper.selectCount(overdueWrapper);
        // 未审核凭证数(status=0,不限制)
        LambdaQueryWrapper<Voucher> unauditedCountWrapper = new LambdaQueryWrapper<Voucher>()
                .eq(Voucher::getStatus, 0);
        applyAccessFilter(unauditedCountWrapper, Voucher::getAccountSetId, accessibleIds);
        long unauditedVoucherCount = voucherMapper.selectCount(unauditedCountWrapper);
        // 本月凭证总数(按当前年月统计,排除作废凭证status=3,避免首页"本月凭证"虚高)
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        LambdaQueryWrapper<Voucher> monthVoucherWrapper = new LambdaQueryWrapper<Voucher>()
                .eq(Voucher::getYear, currentYear)
                .eq(Voucher::getMonth, currentMonth)
                .ne(Voucher::getStatus, 3);
        applyAccessFilter(monthVoucherWrapper, Voucher::getAccountSetId, accessibleIds);
        long monthVoucherCount = voucherMapper.selectCount(monthVoucherWrapper);
        // 未申报税务数(status=0,不限制)
        LambdaQueryWrapper<TaxDeclaration> undeclaredCountWrapper = new LambdaQueryWrapper<TaxDeclaration>()
                .eq(TaxDeclaration::getStatus, 0);
        applyAccessFilter(undeclaredCountWrapper, TaxDeclaration::getAccountSetId, accessibleIds);
        long undeclaredTaxCount = taxDeclarationMapper.selectCount(undeclaredCountWrapper);

        vo.setSummary(buildSummary(totalAccountSets, activeAccountSets, generalTaxpayerCount, smallTaxpayerCount,
                pendingTaskCount, completedTaskCount, unauditedVoucherCount, undeclaredTaxCount, overdueTodoCount,
                monthVoucherCount));

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

    @Override
    @Transactional(readOnly = true)
    public PageResult<TodoItemVO> pageTodoItems(int page, int size) {
        // 参数兜底，防止分页越界或除零
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }
        List<TodoItemVO> all = loadAllTodoItems();
        // total 改用各待办表 selectCount 之和: loadAllTodoItems 每表 LIMIT 500,
        // 合并列表上限1500条,超过时列表大小不再反映真实总数,导致分页总数失真。
        // 注: 数据查询仍保留 LIMIT 500 防OOM,故超出1500条的深分页可能返回空页(已知限制)。
        long total = countAllTodoItems();
        int listSize = all.size();
        int fromIndex = Math.min((page - 1) * size, listSize);
        int toIndex = Math.min(fromIndex + size, listSize);
        List<TodoItemVO> pageList = fromIndex < toIndex
                ? new ArrayList<>(all.subList(fromIndex, toIndex))
                : new ArrayList<>();
        return new PageResult<>(pageList, total, page, size);
    }

    /**
     * 加载全部待办项（合并服务任务/凭证审核/税务申报，按创建时间倒序）。
     * 复用 getDashboard 中的待办查询逻辑，供独立分页查询使用。
     * 待办来自三张不同表，无法单SQL分页，故先按各自上限加载再在内存合并分页。
     */
    private List<TodoItemVO> loadAllTodoItems() {
        // 数据级隔离:仅查询当前用户可访问账套(超级管理员返回null表示不限制)
        Set<Long> accessibleIds = accountSetAccessService.listAccessibleAccountSetIds();

        // 查询账套（用于填充公司名称，限量500防止OOM）
        LambdaQueryWrapper<AccountSet> asWrapper = new LambdaQueryWrapper<>();
        applyAccessFilter(asWrapper, AccountSet::getId, accessibleIds);
        asWrapper.orderByDesc(AccountSet::getUpdateTime).last(QUERY_LIMIT);
        List<AccountSet> accountSets = accountSetMapper.selectList(asWrapper);

        // 待办服务任务（taskStatus<2，限量500防止OOM）
        LambdaQueryWrapper<ServiceTask> taskWrapper = new LambdaQueryWrapper<>();
        applyAccessFilter(taskWrapper, ServiceTask::getAccountSetId, accessibleIds);
        taskWrapper.lt(ServiceTask::getTaskStatus, 2)
                .orderByDesc(ServiceTask::getCreateTime)
                .last(QUERY_LIMIT);
        List<ServiceTask> pendingTasks = serviceTaskMapper.selectList(taskWrapper);

        // 未审核凭证（status=0，限量500防止OOM）
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        applyAccessFilter(voucherWrapper, Voucher::getAccountSetId, accessibleIds);
        voucherWrapper.eq(Voucher::getStatus, 0)
                .orderByDesc(Voucher::getCreateTime)
                .last(QUERY_LIMIT);
        List<Voucher> unauditedVouchers = voucherMapper.selectList(voucherWrapper);

        // 未申报税务（status=0，限量500防止OOM）
        LambdaQueryWrapper<TaxDeclaration> taxWrapper = new LambdaQueryWrapper<>();
        applyAccessFilter(taxWrapper, TaxDeclaration::getAccountSetId, accessibleIds);
        taxWrapper.eq(TaxDeclaration::getStatus, 0)
                .orderByDesc(TaxDeclaration::getCreateTime)
                .last(QUERY_LIMIT);
        List<TaxDeclaration> undeclaredTaxes = taxDeclarationMapper.selectList(taxWrapper);

        // 合并各类型，按创建时间倒序
        List<TodoItemVO> todoItems = new ArrayList<>();
        todoItems.addAll(buildTaskTodos(pendingTasks, accountSets));
        todoItems.addAll(buildVoucherTodos(unauditedVouchers, accountSets));
        todoItems.addAll(buildTaxTodos(undeclaredTaxes, accountSets));
        todoItems.sort(Comparator.comparing(TodoItemVO::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return todoItems;
    }

    /**
     * 统计全部待办的真实总数(各待办表 selectCount 之和)。
     * loadAllTodoItems 出于防OOM对每表 LIMIT 500,合并列表上限1500,
     * 超过时无法反映真实总数,故分页 total 改用各表 count 求和(条件与查询一致,不含LIMIT)。
     */
    private long countAllTodoItems() {
        Set<Long> accessibleIds = accountSetAccessService.listAccessibleAccountSetIds();

        LambdaQueryWrapper<ServiceTask> taskWrapper = new LambdaQueryWrapper<>();
        applyAccessFilter(taskWrapper, ServiceTask::getAccountSetId, accessibleIds);
        taskWrapper.lt(ServiceTask::getTaskStatus, 2);

        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        applyAccessFilter(voucherWrapper, Voucher::getAccountSetId, accessibleIds);
        voucherWrapper.eq(Voucher::getStatus, 0);

        LambdaQueryWrapper<TaxDeclaration> taxWrapper = new LambdaQueryWrapper<>();
        applyAccessFilter(taxWrapper, TaxDeclaration::getAccountSetId, accessibleIds);
        taxWrapper.eq(TaxDeclaration::getStatus, 0);

        return serviceTaskMapper.selectCount(taskWrapper)
                + voucherMapper.selectCount(voucherWrapper)
                + taxDeclarationMapper.selectCount(taxWrapper);
    }

    private DashboardSummary buildSummary(long totalAccountSets, long activeAccountSets,
                                          long generalTaxpayerCount, long smallTaxpayerCount,
                                          long pendingTaskCount, long completedTaskCount,
                                          long unauditedVoucherCount, long undeclaredTaxCount,
                                          long overdueTodoCount, long monthVoucherCount) {
        DashboardSummary s = new DashboardSummary();
        // 统计数均由独立的selectCount查询得出(不加LIMIT),避免超过500条时计数失真
        s.setTotalAccountSets((int) totalAccountSets);
        s.setActiveAccountSets((int) activeAccountSets);
        s.setGeneralTaxpayerCount((int) generalTaxpayerCount);
        s.setSmallTaxpayerCount((int) smallTaxpayerCount);
        s.setPendingTaskCount((int) pendingTaskCount);
        s.setCompletedTaskCount((int) completedTaskCount);
        s.setUnauditedVoucherCount((int) unauditedVoucherCount);
        s.setMonthVoucherCount((int) monthVoucherCount);
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

    /**
     * Dashboard 数据级隔离过滤(IDOR治理):
     * - accessibleIds 为 null: 超级管理员,不限制
     * - accessibleIds 为空: 无任何可访问账套,注入永不命中条件避免空集合in被跳过导致越权
     * - 其他: 按可访问账套集合in过滤
     */
    private <T> void applyAccessFilter(LambdaQueryWrapper<T> wrapper,
                                       SFunction<T, Long> accountSetIdColumn,
                                       Set<Long> accessibleIds) {
        if (accessibleIds == null) {
            return;
        }
        if (accessibleIds.isEmpty()) {
            wrapper.eq(accountSetIdColumn, -1L);
            return;
        }
        wrapper.in(accountSetIdColumn, accessibleIds);
    }
}
