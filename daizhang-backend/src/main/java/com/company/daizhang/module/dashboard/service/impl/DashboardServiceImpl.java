package com.company.daizhang.module.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
import com.company.daizhang.module.accountset.mapper.AccountSetMapper;
import com.company.daizhang.module.dashboard.service.DashboardService;
import com.company.daizhang.module.dashboard.vo.DashboardStatsVO;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 仪表盘服务实现
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    
    private final AccountSetMapper accountSetMapper;
    private final VoucherMapper voucherMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final AccountPeriodMapper accountPeriodMapper;
    private final SubjectMapper subjectMapper;
    
    @Override
    public DashboardStatsVO getStats(Long accountSetId) {
        DashboardStatsVO stats = new DashboardStatsVO();
        
        // 账套数量
        stats.setAccountSetCount(accountSetMapper.selectCount(null));
        
        // 本月凭证数量
        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                     .eq(Voucher::getYear, currentYear)
                     .eq(Voucher::getMonth, currentMonth);
        stats.setMonthVoucherCount(voucherMapper.selectCount(voucherWrapper));
        
        // 待审核凭证数量
        LambdaQueryWrapper<Voucher> pendingAuditWrapper = new LambdaQueryWrapper<>();
        pendingAuditWrapper.eq(Voucher::getAccountSetId, accountSetId)
                          .eq(Voucher::getStatus, 0); // 未审核
        stats.setPendingAuditCount(voucherMapper.selectCount(pendingAuditWrapper));
        
        // 待报税数量（当前月份未结账的期间数）
        LambdaQueryWrapper<AccountPeriod> periodWrapper = new LambdaQueryWrapper<>();
        periodWrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
                    .eq(AccountPeriod::getYear, currentYear)
                    .eq(AccountPeriod::getMonth, currentMonth)
                    .eq(AccountPeriod::getStatus, 0); // 未结账
        stats.setPendingTaxCount(accountPeriodMapper.selectCount(periodWrapper));
        
        // 查询科目余额
        LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
        balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                     .eq(AccountBalance::getYear, currentYear)
                     .eq(AccountBalance::getMonth, currentMonth);
        List<AccountBalance> balances = accountBalanceMapper.selectList(balanceWrapper);
        
        // 查询科目信息
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId);
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);
        
        Map<Long, Subject> subjectMap = subjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));
        
        // 计算各项财务指标
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal cashBalance = BigDecimal.ZERO;
        
        for (AccountBalance balance : balances) {
            Subject subject = subjectMap.get(balance.getSubjectId());
            if (subject == null) {
                continue;
            }
            
            String category = subject.getCategory();
            BigDecimal endDebit = balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO;
            BigDecimal endCredit = balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO;
            
            // 资产类科目
            if ("asset".equals(category)) {
                totalAssets = totalAssets.add(endDebit).subtract(endCredit);
                
                // 现金类科目
                if (subject.getIsCash() != null && subject.getIsCash() == 1) {
                    cashBalance = cashBalance.add(endDebit).subtract(endCredit);
                }
            }
            
            // 收入类科目
            if ("revenue".equals(category)) {
                totalRevenue = totalRevenue.add(endCredit).subtract(endDebit);
            }
            
            // 费用类科目
            if ("expense".equals(category)) {
                totalExpense = totalExpense.add(endDebit).subtract(endCredit);
            }
        }
        
        stats.setTotalAssets(totalAssets);
        stats.setTotalRevenue(totalRevenue);
        stats.setTotalProfit(totalRevenue.subtract(totalExpense));
        stats.setCashBalance(cashBalance);
        
        return stats;
    }
}
