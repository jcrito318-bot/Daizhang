package com.company.daizhang.module.accountset.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.module.accountset.dto.SubjectBalanceRequest;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.entity.SubjectBalance;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.accountset.mapper.SubjectBalanceMapper;
import com.company.daizhang.module.accountset.service.SubjectBalanceService;
import com.company.daizhang.module.accountset.vo.SubjectBalanceVO;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 科目期初余额服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectBalanceServiceImpl extends ServiceImpl<SubjectBalanceMapper, SubjectBalance> implements SubjectBalanceService {

    private final AccountBalanceMapper accountBalanceMapper;
    private final SubjectMapper subjectMapper;

    /**
     * 期次：期初
     */
    private static final Integer PERIOD_BEGIN = 1;

    @Override
    public List<SubjectBalanceVO> listByAccountSetAndYear(Long accountSetId, Integer year) {
        LambdaQueryWrapper<SubjectBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubjectBalance::getAccountSetId, accountSetId)
               .eq(SubjectBalance::getYear, year)
               .eq(SubjectBalance::getPeriod, PERIOD_BEGIN)
               .orderByAsc(SubjectBalance::getSubjectCode);
        List<SubjectBalance> list = this.list(wrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(Long accountSetId, Integer year, List<SubjectBalanceRequest> requests) {
        // 先删除该账套该年度的旧数据
        LambdaQueryWrapper<SubjectBalance> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SubjectBalance::getAccountSetId, accountSetId)
                     .eq(SubjectBalance::getYear, year)
                     .eq(SubjectBalance::getPeriod, PERIOD_BEGIN);
        this.remove(deleteWrapper);

        // 批量插入新数据
        if (requests != null && !requests.isEmpty()) {
            for (SubjectBalanceRequest request : requests) {
                SubjectBalance balance = new SubjectBalance();
                BeanUtil.copyProperties(request, balance);
                balance.setAccountSetId(accountSetId);
                balance.setYear(year);
                balance.setPeriod(PERIOD_BEGIN);
                if (balance.getBeginDebit() == null) {
                    balance.setBeginDebit(BigDecimal.ZERO);
                }
                if (balance.getBeginCredit() == null) {
                    balance.setBeginCredit(BigDecimal.ZERO);
                }
                this.save(balance);

                // 同步到 AccountBalance(year, month=1):否则用户录入的期初余额不会进入
                // 月度余额表/资产负债表(这些报表读 AccountBalance),期初恒为0导致报表错乱。
                // 仅更新 beginDebit/beginCredit 并重算 endDebit/endCredit,
                // 保留已过账凭证产生的 periodDebit/periodCredit/yearDebit/yearCredit。
                syncAccountBalanceBegin(accountSetId, year, balance.getSubjectId(),
                        balance.getBeginDebit(), balance.getBeginCredit());
            }
        }

        log.info("批量保存期初余额成功，账套ID: {}, 年度: {}, 记录数: {}",
                accountSetId, year, requests == null ? 0 : requests.size());
    }

    /**
     * 同步期初余额到 AccountBalance(year, month=1)。
     * - 若记录不存在:新建,beginDebit/beginCredit 取录入值,periodDebit/Credit=0,
     *   endDebit/endCredit 按余额方向计算净额,yearDebit/yearCredit=0。
     * - 若记录已存在(1月已有凭证过账):仅更新 beginDebit/beginCredit,
     *   并按"begin + 现有 period"重算 endDebit/endCredit,保留 period/year 累计。
     */
    private void syncAccountBalanceBegin(Long accountSetId, Integer year, Long subjectId,
                                         BigDecimal beginDebit, BigDecimal beginCredit) {
        if (subjectId == null) {
            return;
        }
        BigDecimal bd = beginDebit != null ? beginDebit : BigDecimal.ZERO;
        BigDecimal bc = beginCredit != null ? beginCredit : BigDecimal.ZERO;

        LambdaQueryWrapper<AccountBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountBalance::getAccountSetId, accountSetId)
               .eq(AccountBalance::getSubjectId, subjectId)
               .eq(AccountBalance::getYear, year)
               .eq(AccountBalance::getMonth, 1);
        AccountBalance ab = accountBalanceMapper.selectOne(wrapper);

        // 余额方向:1=借方,2=贷方
        int balanceDirection = 1;
        Subject subject = subjectMapper.selectById(subjectId);
        if (subject != null && subject.getBalanceDirection() != null) {
            balanceDirection = subject.getBalanceDirection();
        }

        if (ab == null) {
            ab = new AccountBalance();
            ab.setAccountSetId(accountSetId);
            ab.setSubjectId(subjectId);
            ab.setYear(year);
            ab.setMonth(1);
            ab.setBeginDebit(bd);
            ab.setBeginCredit(bc);
            ab.setPeriodDebit(BigDecimal.ZERO);
            ab.setPeriodCredit(BigDecimal.ZERO);
            ab.setYearDebit(BigDecimal.ZERO);
            ab.setYearCredit(BigDecimal.ZERO);
            applyEndBalance(ab, balanceDirection);
            accountBalanceMapper.insert(ab);
        } else {
            ab.setBeginDebit(bd);
            ab.setBeginCredit(bc);
            applyEndBalance(ab, balanceDirection);
            accountBalanceMapper.updateById(ab);
        }
    }

    /**
     * 按 begin + period 净额计算 endDebit/endCredit。
     */
    private void applyEndBalance(AccountBalance ab, int balanceDirection) {
        BigDecimal beginD = ab.getBeginDebit() != null ? ab.getBeginDebit() : BigDecimal.ZERO;
        BigDecimal beginC = ab.getBeginCredit() != null ? ab.getBeginCredit() : BigDecimal.ZERO;
        BigDecimal periodD = ab.getPeriodDebit() != null ? ab.getPeriodDebit() : BigDecimal.ZERO;
        BigDecimal periodC = ab.getPeriodCredit() != null ? ab.getPeriodCredit() : BigDecimal.ZERO;
        if (balanceDirection == 2) {
            BigDecimal net = beginC.add(periodC).subtract(periodD);
            if (net.compareTo(BigDecimal.ZERO) >= 0) {
                ab.setEndCredit(net);
                ab.setEndDebit(BigDecimal.ZERO);
            } else {
                ab.setEndCredit(BigDecimal.ZERO);
                ab.setEndDebit(net.abs());
            }
        } else {
            BigDecimal net = beginD.add(periodD).subtract(periodC);
            if (net.compareTo(BigDecimal.ZERO) >= 0) {
                ab.setEndDebit(net);
                ab.setEndCredit(BigDecimal.ZERO);
            } else {
                ab.setEndDebit(BigDecimal.ZERO);
                ab.setEndCredit(net.abs());
            }
        }
    }

    @Override
    public Map<String, Object> trialBalance(Long accountSetId, Integer year) {
        List<SubjectBalanceVO> list = listByAccountSetAndYear(accountSetId, year);

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (SubjectBalanceVO vo : list) {
            if (vo.getBeginDebit() != null) {
                totalDebit = totalDebit.add(vo.getBeginDebit());
            }
            if (vo.getBeginCredit() != null) {
                totalCredit = totalCredit.add(vo.getBeginCredit());
            }
        }

        boolean balanced = totalDebit.compareTo(totalCredit) == 0;

        Map<String, Object> result = new HashMap<>();
        result.put("totalDebit", totalDebit);
        result.put("totalCredit", totalCredit);
        result.put("balanced", balanced);
        result.put("difference", totalDebit.subtract(totalCredit));
        result.put("list", list);

        log.info("试算平衡，账套ID: {}, 年度: {}, 借方合计: {}, 贷方合计: {}, 是否平衡: {}",
                accountSetId, year, totalDebit, totalCredit, balanced);

        return result;
    }

    /**
     * 实体转VO
     */
    private SubjectBalanceVO convertToVO(SubjectBalance balance) {
        SubjectBalanceVO vo = new SubjectBalanceVO();
        BeanUtil.copyProperties(balance, vo);
        return vo;
    }
}
