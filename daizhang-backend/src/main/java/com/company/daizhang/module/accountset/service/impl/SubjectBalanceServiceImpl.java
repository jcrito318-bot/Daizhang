package com.company.daizhang.module.accountset.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.module.accountset.dto.SubjectBalanceRequest;
import com.company.daizhang.module.accountset.entity.SubjectBalance;
import com.company.daizhang.module.accountset.mapper.SubjectBalanceMapper;
import com.company.daizhang.module.accountset.service.SubjectBalanceService;
import com.company.daizhang.module.accountset.vo.SubjectBalanceVO;
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
            }
        }

        log.info("批量保存期初余额成功，账套ID: {}, 年度: {}, 记录数: {}",
                accountSetId, year, requests == null ? 0 : requests.size());
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
