package com.company.daizhang.module.accountset.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.module.accountset.dto.SubjectBalanceRequest;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.entity.SubjectBalance;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
import com.company.daizhang.module.accountset.mapper.SubjectBalanceMapper;
import com.company.daizhang.module.accountset.service.SubjectBalanceService;
import com.company.daizhang.module.accountset.vo.SubjectBalanceVO;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private final AccountPeriodMapper accountPeriodMapper;
    private final JdbcTemplate jdbcTemplate;

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
        // 结账校验:已结账期间的期初余额为冻结快照,不允许修改
        // 否则修改beginDebit后重算endDebit/endCredit会改写已结账期间的期末余额,破坏结账快照,审计轨迹被破坏
        LambdaQueryWrapper<AccountPeriod> periodWrapper = new LambdaQueryWrapper<>();
        periodWrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
                     .eq(AccountPeriod::getYear, year)
                     .eq(AccountPeriod::getMonth, 1);
        AccountPeriod janPeriod = accountPeriodMapper.selectOne(periodWrapper);
        if (janPeriod != null && Integer.valueOf(1).equals(janPeriod.getStatus())) {
            throw new BusinessException("1月已结账，期初余额不可修改，请先反结账");
        }

        // 先删除该账套该年度的旧数据
        // 注意:SubjectBalance 继承 BaseEntity,deleted 字段标注了 @TableLogic,且
        // application.yml 全局开启逻辑删除。this.remove() 仅做逻辑删除(UPDATE deleted=1),
        // 旧记录(含此前逻辑删除的记录)仍占用唯一键 (account_set_id, subject_id, year, period),
        // 而 uk_subject_balance 未包含 deleted 字段,导致重复录入时 this.save() 插入新行
        // 触发唯一约束冲突返回 500,saveBatch 非幂等。
        // 此处改用 JdbcTemplate 执行物理删除(共享 DataSource,参与当前 @Transactional),
        // 清理活动记录与历史逻辑删除记录,保证重复录入幂等。
        jdbcTemplate.update(
                "DELETE FROM acc_subject_balance WHERE account_set_id = ? AND `year` = ? AND period = ?",
                accountSetId, year, PERIOD_BEGIN);

        // 批量插入新数据
        if (requests != null && !requests.isEmpty()) {
            // 校验科目归属:所有 subjectId 必须属于当前账套,防止为他账套科目写入期初余额
            Set<Long> subjectIds = requests.stream()
                    .map(SubjectBalanceRequest::getSubjectId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!subjectIds.isEmpty()) {
                List<Subject> subjects = subjectMapper.selectList(new LambdaQueryWrapper<Subject>()
                        .eq(Subject::getAccountSetId, accountSetId)
                        .in(Subject::getId, subjectIds));
                Set<Long> validSubjectIds = subjects.stream()
                        .map(Subject::getId)
                        .collect(Collectors.toSet());
                List<Long> invalidIds = subjectIds.stream()
                        .filter(id -> !validSubjectIds.contains(id))
                        .collect(Collectors.toList());
                if (!invalidIds.isEmpty()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                            "科目不属于当前账套: " + invalidIds);
                }
            }

            // 借贷平衡校验:期初余额借方合计必须等于贷方合计
            BigDecimal totalDebit = requests.stream()
                    .map(r -> r.getBeginDebit() != null ? r.getBeginDebit() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCredit = requests.stream()
                    .map(r -> r.getBeginCredit() != null ? r.getBeginCredit() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalDebit.compareTo(totalCredit) != 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                        "期初余额借贷不平衡(借方合计:" + totalDebit + ", 贷方合计:" + totalCredit + ")");
            }
            // 构建科目ID→科目对象映射(科目归属校验时已查询),用于回填subjectCode/subjectName
            // 前端请求可能只传subjectId,而acc_subject_balance表的subject_code列不允许NULL
            Map<Long, Subject> subjectMap = subjectMapper.selectList(new LambdaQueryWrapper<Subject>()
                    .eq(Subject::getAccountSetId, accountSetId)
                    .in(Subject::getId, subjectIds))
                    .stream().collect(Collectors.toMap(Subject::getId, s -> s));

            for (SubjectBalanceRequest request : requests) {
                SubjectBalance balance = new SubjectBalance();
                BeanUtil.copyProperties(request, balance);
                balance.setAccountSetId(accountSetId);
                balance.setYear(year);
                balance.setPeriod(PERIOD_BEGIN);
                // 回填subjectCode/subjectName(前端可能未传,但DB要求非NULL)
                Subject subj = subjectMap.get(request.getSubjectId());
                if (subj != null) {
                    if (balance.getSubjectCode() == null) {
                        balance.setSubjectCode(subj.getCode());
                    }
                    if (balance.getSubjectName() == null) {
                        balance.setSubjectName(subj.getName());
                    }
                }
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
