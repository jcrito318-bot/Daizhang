package com.company.daizhang.module.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.customer.entity.Customer;
import com.company.daizhang.module.customer.mapper.CustomerMapper;
import com.company.daizhang.module.report.service.AgingAnalysisService;
import com.company.daizhang.module.report.vo.AgeBucketVO;
import com.company.daizhang.module.report.vo.AgingAnalysisVO;
import com.company.daizhang.module.report.vo.AgingItemVO;
import com.company.daizhang.module.report.vo.AgingSummaryVO;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.entity.VoucherDetail;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 账龄分析服务实现
 * <p>
 * 计算口径(简化版):
 * <ol>
 *   <li>查询指定账套下所有客户(cst_customer 表)</li>
 *   <li>对每个客户,查询应收账款(1122)/应付账款(2202)相关的已过账凭证明细</li>
 *   <li>匹配规则:优先 auxiliary_id = customer.id;若该客户无 auxiliary_id 关联,
 *       兜底使用 summary LIKE %customerName% 匹配</li>
 *   <li>未核销余额 = 借方累计 - 贷方累计(应收);应付反之</li>
 *   <li>按凭证日期与截止日期的天数差分桶: 0-30/31-60/61-90/91-180/180+</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgingAnalysisServiceImpl implements AgingAnalysisService {

    /**
     * 应收账款科目编码前缀(1122 应收账款)
     */
    private static final String RECEIVABLE_SUBJECT_PREFIX = "1122";

    /**
     * 应付账款科目编码前缀(2202 应付账款)
     */
    private static final String PAYABLE_SUBJECT_PREFIX = "2202";

    /**
     * 已过账状态
     */
    private static final int VOUCHER_STATUS_POSTED = 2;

    private final CustomerMapper customerMapper;
    private final VoucherMapper voucherMapper;
    private final VoucherDetailMapper voucherDetailMapper;
    private final AccountSetAccessService accountSetAccessService;

    @Override
    @Transactional(readOnly = true)
    public AgingAnalysisVO agingAnalysis(Long accountSetId, LocalDate asOfDate) {
        // IDOR 越权治理:校验账套访问权(注解层已校验,这里兜底二次校验)
        accountSetAccessService.checkAccess(accountSetId);
        LocalDate baseDate = resolveAsOfDate(asOfDate);

        List<AgingItemVO> customerAging = buildAgingItems(accountSetId, baseDate, true);
        List<AgingItemVO> supplierAging = buildAgingItems(accountSetId, baseDate, false);
        AgingSummaryVO summary = buildSummary(customerAging, supplierAging);

        AgingAnalysisVO vo = new AgingAnalysisVO();
        vo.setAccountSetId(accountSetId);
        vo.setAsOfDate(baseDate);
        vo.setCustomerAging(customerAging);
        vo.setSupplierAging(supplierAging);
        vo.setSummary(summary);
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgingItemVO> receivableAging(Long accountSetId, LocalDate asOfDate) {
        accountSetAccessService.checkAccess(accountSetId);
        LocalDate baseDate = resolveAsOfDate(asOfDate);
        return buildAgingItems(accountSetId, baseDate, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgingItemVO> payableAging(Long accountSetId, LocalDate asOfDate) {
        accountSetAccessService.checkAccess(accountSetId);
        LocalDate baseDate = resolveAsOfDate(asOfDate);
        return buildAgingItems(accountSetId, baseDate, false);
    }

    @Override
    @Transactional(readOnly = true)
    public AgingSummaryVO agingSummary(Long accountSetId, LocalDate asOfDate) {
        accountSetAccessService.checkAccess(accountSetId);
        LocalDate baseDate = resolveAsOfDate(asOfDate);
        List<AgingItemVO> customerAging = buildAgingItems(accountSetId, baseDate, true);
        List<AgingItemVO> supplierAging = buildAgingItems(accountSetId, baseDate, false);
        return buildSummary(customerAging, supplierAging);
    }

    // ==================== 核心计算 ====================

    /**
     * 构建账龄分析明细列表
     *
     * @param accountSetId 账套ID
     * @param baseDate     截止日期
     * @param isReceivable true=应收(1122,余额=借-贷);false=应付(2202,余额=贷-借)
     * @return 账龄明细列表(已按总金额降序排序,仅含有未核销余额的记录)
     */
    private List<AgingItemVO> buildAgingItems(Long accountSetId, LocalDate baseDate, boolean isReceivable) {
        String subjectPrefix = isReceivable ? RECEIVABLE_SUBJECT_PREFIX : PAYABLE_SUBJECT_PREFIX;

        // 1. 查询所有客户(同一张 cst_customer 表,客户/供应商维度由是否有应收/应付余额决定)
        LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
        customerWrapper.eq(Customer::getAccountSetId, accountSetId)
                .eq(Customer::getStatus, 1);
        List<Customer> customers = customerMapper.selectList(customerWrapper);
        if (customers.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查询该账套下所有已过账凭证(用于关联 voucherDate)
        //    一次查询避免 N+1;筛掉 voucherDate 在 baseDate 之后的凭证(账龄基准日之后的不参与统计)
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getStatus, VOUCHER_STATUS_POSTED)
                .le(Voucher::getVoucherDate, baseDate);
        List<Voucher> vouchers = voucherMapper.selectList(voucherWrapper);
        if (vouchers.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Voucher> voucherMap = vouchers.stream()
                .collect(Collectors.toMap(Voucher::getId, v -> v, (a, b) -> a));
        List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());

        // 3. 查询应收/应付科目相关的凭证明细(subject_code 前缀匹配)
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                .likeRight(VoucherDetail::getSubjectCode, subjectPrefix);
        List<VoucherDetail> allDetails = voucherDetailMapper.selectList(detailWrapper);
        if (allDetails.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. 按"客户ID(辅助核算)"分组;同时为无 auxiliary_id 的明细按 summary 兜底分组
        //    优先级:auxiliary_id 直接匹配客户ID;否则按 summary LIKE %customerName% 兜底
        Map<Long, List<VoucherDetail>> byAuxId = new HashMap<>();
        List<VoucherDetail> noAuxDetails = new ArrayList<>();
        for (VoucherDetail d : allDetails) {
            if (d.getAuxiliaryId() != null) {
                byAuxId.computeIfAbsent(d.getAuxiliaryId(), k -> new ArrayList<>()).add(d);
            } else {
                noAuxDetails.add(d);
            }
        }

        List<AgingItemVO> result = new ArrayList<>();
        for (Customer customer : customers) {
            // 收集该客户相关的所有明细:auxiliary_id 命中 + summary 兜底命中
            List<VoucherDetail> customerDetails = new ArrayList<>();
            List<VoucherDetail> auxMatched = byAuxId.get(customer.getId());
            if (auxMatched != null) {
                customerDetails.addAll(auxMatched);
            }
            // 兜底:summary 模糊匹配客户名称(仅在该客户没有 auxiliary_id 关联时使用,
            // 避免与辅助核算命中重复)
            if (auxMatched == null && customer.getCustomerName() != null && !customer.getCustomerName().isEmpty()) {
                String name = customer.getCustomerName();
                for (VoucherDetail d : noAuxDetails) {
                    if (d.getSummary() != null && d.getSummary().contains(name)) {
                        customerDetails.add(d);
                    }
                }
            }

            if (customerDetails.isEmpty()) {
                continue;
            }

            AgingItemVO item = buildItem(customer, customerDetails, voucherMap, baseDate, isReceivable);
            // 过滤掉总余额为 0 的客户(已完全核销)
            if (item.getTotalAmount() != null && item.getTotalAmount().compareTo(BigDecimal.ZERO) != 0) {
                result.add(item);
            }
        }

        // 按总金额降序排序
        result.sort(Comparator.comparing(AgingItemVO::getTotalAmount).reversed());

        log.info("账龄分析({})完成: accountSetId={}, asOfDate={}, 客户/供应商数={}",
                isReceivable ? "应收" : "应付", accountSetId, baseDate, result.size());
        return result;
    }

    /**
     * 构建单个客户/供应商的账龄明细
     */
    private AgingItemVO buildItem(Customer customer, List<VoucherDetail> details,
                                  Map<Long, Voucher> voucherMap, LocalDate baseDate,
                                  boolean isReceivable) {
        BigDecimal within30 = BigDecimal.ZERO;
        BigDecimal days31To60 = BigDecimal.ZERO;
        BigDecimal days61To90 = BigDecimal.ZERO;
        BigDecimal days91To180 = BigDecimal.ZERO;
        BigDecimal over180 = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        LocalDate oldestDate = null;
        HashSet<Long> voucherIdSet = new HashSet<>();

        for (VoucherDetail d : details) {
            Voucher voucher = voucherMap.get(d.getVoucherId());
            if (voucher == null || voucher.getVoucherDate() == null) {
                continue;
            }
            BigDecimal debit = d.getDebit() != null ? d.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = d.getCredit() != null ? d.getCredit() : BigDecimal.ZERO;
            // 应收: 借方-贷方; 应付: 贷方-借方
            BigDecimal amount = isReceivable
                    ? debit.subtract(credit)
                    : credit.subtract(debit);

            // 按凭证日期与截止日期的差值分桶
            long days = ChronoUnit.DAYS.between(voucher.getVoucherDate(), baseDate);
            if (days < 0) {
                // 凭证日期在截止日期之后(理论上已被 voucherWrapper 过滤,防御性兜底)
                continue;
            }
            if (days <= 30) {
                within30 = within30.add(amount);
            } else if (days <= 60) {
                days31To60 = days31To60.add(amount);
            } else if (days <= 90) {
                days61To90 = days61To90.add(amount);
            } else if (days <= 180) {
                days91To180 = days91To180.add(amount);
            } else {
                over180 = over180.add(amount);
            }
            totalAmount = totalAmount.add(amount);
            voucherIdSet.add(d.getVoucherId());

            // 记录最早凭证日期
            if (oldestDate == null || voucher.getVoucherDate().isBefore(oldestDate)) {
                oldestDate = voucher.getVoucherDate();
            }
        }

        int voucherCount = voucherIdSet.size();

        AgeBucketVO bucket = new AgeBucketVO();
        bucket.setWithin30Days(within30);
        bucket.setDays31To60(days31To60);
        bucket.setDays61To90(days61To90);
        bucket.setDays91To180(days91To180);
        bucket.setOver180Days(over180);

        AgingItemVO item = new AgingItemVO();
        item.setCustomerId(customer.getId());
        item.setCustomerName(customer.getCustomerName());
        item.setTotalAmount(totalAmount);
        item.setAgeBuckets(bucket);
        item.setOldestDate(oldestDate);
        item.setOldestDays(oldestDate != null
                ? (int) ChronoUnit.DAYS.between(oldestDate, baseDate)
                : null);
        item.setVoucherCount(voucherCount);
        return item;
    }

    /**
     * 构建汇总信息
     */
    private AgingSummaryVO buildSummary(List<AgingItemVO> customerAging, List<AgingItemVO> supplierAging) {
        BigDecimal totalReceivable = BigDecimal.ZERO;
        BigDecimal totalPayable = BigDecimal.ZERO;
        BigDecimal overdueReceivable = BigDecimal.ZERO;
        BigDecimal overduePayable = BigDecimal.ZERO;

        for (AgingItemVO item : customerAging) {
            if (item.getTotalAmount() == null) {
                continue;
            }
            totalReceivable = totalReceivable.add(item.getTotalAmount());
            AgeBucketVO b = item.getAgeBuckets();
            if (b != null) {
                // 逾期应收 = 31 天以上分桶之和(0-30 天为正常)
                overdueReceivable = overdueReceivable.add(safeAdd(b.getDays31To60(), b.getDays61To90(),
                        b.getDays91To180(), b.getOver180Days()));
            }
        }
        for (AgingItemVO item : supplierAging) {
            if (item.getTotalAmount() == null) {
                continue;
            }
            totalPayable = totalPayable.add(item.getTotalAmount());
            AgeBucketVO b = item.getAgeBuckets();
            if (b != null) {
                overduePayable = overduePayable.add(safeAdd(b.getDays31To60(), b.getDays61To90(),
                        b.getDays91To180(), b.getOver180Days()));
            }
        }

        AgingSummaryVO summary = new AgingSummaryVO();
        summary.setTotalReceivable(totalReceivable);
        summary.setTotalPayable(totalPayable);
        summary.setOverdueReceivable(overdueReceivable);
        summary.setOverduePayable(overduePayable);
        summary.setCustomerCount(customerAging.size());
        summary.setSupplierCount(supplierAging.size());
        return summary;
    }

    /**
     * 安全累加(忽略 null)
     */
    private BigDecimal safeAdd(BigDecimal... values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            if (v != null) {
                sum = sum.add(v);
            }
        }
        return sum;
    }

    /**
     * 解析截止日期:为 null 时默认取本月最后一天
     */
    private LocalDate resolveAsOfDate(LocalDate asOfDate) {
        if (asOfDate != null) {
            return asOfDate;
        }
        // 默认本月最后一天
        YearMonth currentMonth = YearMonth.now();
        return currentMonth.atEndOfMonth();
    }
}
