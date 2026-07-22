package com.company.daizhang.module.period.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.enums.PeriodStatus;
import com.company.daizhang.common.enums.VoucherStatus;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
import com.company.daizhang.module.period.dto.PeriodCloseWizardRequest;
import com.company.daizhang.module.period.service.PeriodCloseWizardService;
import com.company.daizhang.module.period.service.PeriodService;
import com.company.daizhang.module.period.vo.ClosePeriodResultVO;
import com.company.daizhang.module.period.vo.PeriodCloseWizardVO;
import com.company.daizhang.module.period.vo.WizardStepResult;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.entity.VoucherDetail;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 期末结账向导编排服务实现
 * <p>
 * 一键完成"结转损益 + 结账 + 下月开启"的月末结账流程。
 * <p>
 * 事务策略:各步骤独立执行,内层 {@link PeriodService} 方法(@Transactional)各自独立事务,
 * 失败步骤仅回滚该步骤,后续步骤继续执行(保持向导设计意图),最终汇总 VO 正常返回。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PeriodCloseWizardServiceImpl implements PeriodCloseWizardService {

    private final PeriodService periodService;
    private final AccountPeriodMapper accountPeriodMapper;
    private final VoucherMapper voucherMapper;
    private final VoucherDetailMapper voucherDetailMapper;

    /**
     * 步骤 3"结转损益"识别用摘要。
     * 现有 {@link PeriodService#carryForward} 生成的凭证明细摘要为"结转本年利润",
     * 任务要求统一为"结转本期损益"(未来增强),此处两种摘要均识别,保证向后兼容。
     */
    private static final String CARRY_FORWARD_SUMMARY_LEGACY = "结转本年利润";
    private static final String CARRY_FORWARD_SUMMARY_NEW = "结转本期损益";

    /** 步骤序号 -> 步骤名称(用于已结账场景的批量跳过) */
    private static final String[] STEP_NAMES = {
            "数据完整性检查",  // 1
            "期末调汇",        // 2
            "结转损益",        // 3
            "结转成本",        // 4
            "计提折旧",        // 5
            "结账",            // 6
            "下月开启"         // 7
    };

    @Override
    public PeriodCloseWizardVO executeCloseWizard(Long accountSetId, int year, int month,
                                                   PeriodCloseWizardRequest request) {
        // 请求体缺省值均为 true:未传或传 null 时按 true 处理
        boolean skipOptional = request == null
                || request.getSkipOptionalSteps() == null
                || request.getSkipOptionalSteps();
        boolean autoClose = request == null
                || request.getAutoCloseIfNoErrors() == null
                || request.getAutoCloseIfNoErrors();

        log.info("期末结账向导开始, 账套ID={}, 期间={}-{}, skipOptional={}, autoClose={}",
                accountSetId, year, month, skipOptional, autoClose);

        // 各步骤独立事务执行(内层 @Transactional 方法各自独立事务),
        // 失败步骤仅回滚该步骤,后续步骤继续执行(保持向导设计意图),最终汇总 VO 正常返回
        PeriodCloseWizardVO vo = new PeriodCloseWizardVO();
        List<WizardStepResult> steps = new ArrayList<>(7);
        vo.setSteps(steps);
        boolean aborted = false;

        // 前置校验:本期已结账则全部跳过,不执行任何写操作
        AccountPeriod period = findPeriod(accountSetId, year, month);
        if (period != null && PeriodStatus.CLOSED.getCode().equals(period.getStatus())) {
            for (int i = 1; i <= 7; i++) {
                steps.add(WizardStepResult.skipped(i, STEP_NAMES[i - 1],
                        "本期已结账,无需重复执行结账向导"));
            }
            vo.setNextPeriodOpened(false);
            applySummary(vo, false);
            log.info("期末结账向导完成, 账套ID={}, 期间={}-{}, 整体状态={}, 成功={}, 失败={}, 跳过={}, 下月已开={}",
                    accountSetId, year, month, vo.getOverallStatus(),
                    vo.getSuccessCount(), vo.getFailedCount(), vo.getSkippedCount(), vo.isNextPeriodOpened());
            return vo;
        }

        // ============ 步骤 1: 数据完整性检查 ============
        WizardStepResult step1 = executeDataIntegrityCheck(accountSetId, year, month);
        steps.add(step1);
        if (WizardStepResult.STATUS_FAILED.equals(step1.getStatus()) && autoClose) {
            // autoClose=true: 数据完整性检查失败,中止后续步骤
            aborted = true;
        }

        // ============ 步骤 2: 期末调汇(预留,暂未实现) ============
        steps.add(buildSkippedStep(2, aborted, "期末调汇",
                "本期暂未实现期末调汇,已跳过"));

        // ============ 步骤 3: 结转损益 ============
        WizardStepResult step3 = aborted
                ? WizardStepResult.skipped(3, "结转损益", "前序步骤失败,本步骤已跳过")
                : executeCarryForward(accountSetId, year, month);
        steps.add(step3);
        if (WizardStepResult.STATUS_FAILED.equals(step3.getStatus())) {
            aborted = true;
        }

        // ============ 步骤 4: 结转成本(可选,商业账套跳过) ============
        steps.add(buildSkippedStep(4, aborted, "结转成本",
                skipOptional ? "可选步骤,已跳过" : "暂未实现,已跳过"));

        // ============ 步骤 5: 计提折旧(可选,依赖固定资产模块) ============
        steps.add(buildSkippedStep(5, aborted, "计提折旧",
                skipOptional ? "可选步骤,已跳过" : "暂未实现,已跳过"));

        // ============ 步骤 6: 结账 ============
        WizardStepResult step6 = aborted
                ? WizardStepResult.skipped(6, "结账", "前序步骤失败,本步骤已跳过")
                : executeClosePeriod(accountSetId, year, month);
        steps.add(step6);
        if (WizardStepResult.STATUS_FAILED.equals(step6.getStatus())) {
            aborted = true;
        }

        // ============ 步骤 7: 下月开启 ============
        WizardStepResult step7 = aborted
                ? WizardStepResult.skipped(7, "下月开启", "前序步骤失败,本步骤已跳过")
                : executeOpenNextPeriod(accountSetId, year, month);
        steps.add(step7);
        if (WizardStepResult.STATUS_FAILED.equals(step7.getStatus())) {
            aborted = true;
        }
        vo.setNextPeriodOpened(WizardStepResult.STATUS_SUCCESS.equals(step7.getStatus()));

        // 汇总各步骤状态
        applySummary(vo, aborted);

        log.info("期末结账向导完成, 账套ID={}, 期间={}-{}, 整体状态={}, 成功={}, 失败={}, 跳过={}, 下月已开={}",
                accountSetId, year, month, vo.getOverallStatus(),
                vo.getSuccessCount(), vo.getFailedCount(), vo.getSkippedCount(), vo.isNextPeriodOpened());
        return vo;
    }

    // ==================== 各步骤执行方法 ====================

    /**
     * 步骤 1: 数据完整性检查。
     * 检查本期是否存在未审核凭证(提示用户先审核)和借贷不平凭证。
     */
    private WizardStepResult executeDataIntegrityCheck(Long accountSetId, int year, int month) {
        try {
            // 1. 未审核凭证数(状态=UNAUDITED)
            LambdaQueryWrapper<Voucher> unauditWrapper = new LambdaQueryWrapper<>();
            unauditWrapper.eq(Voucher::getAccountSetId, accountSetId)
                    .eq(Voucher::getYear, year)
                    .eq(Voucher::getMonth, month)
                    .eq(Voucher::getStatus, VoucherStatus.UNAUDITED.getCode());
            long unauditedCount = voucherMapper.selectCount(unauditWrapper);

            // 2. 借贷不平凭证数(理论上凭证录入时已校验平衡,此处为安全网)
            LambdaQueryWrapper<Voucher> unbalancedWrapper = new LambdaQueryWrapper<>();
            unbalancedWrapper.eq(Voucher::getAccountSetId, accountSetId)
                    .eq(Voucher::getYear, year)
                    .eq(Voucher::getMonth, month)
                    .ne(Voucher::getStatus, 3) // 排除已作废凭证
                    .apply("total_debit != total_credit");
            long unbalancedCount = voucherMapper.selectCount(unbalancedWrapper);

            if (unauditedCount > 0 || unbalancedCount > 0) {
                List<String> issues = new ArrayList<>();
                if (unauditedCount > 0) {
                    issues.add("存在 " + unauditedCount + " 张未审核凭证,请先审核");
                }
                if (unbalancedCount > 0) {
                    issues.add("存在 " + unbalancedCount + " 张借贷不平凭证");
                }
                String message = String.join(";", issues);
                return WizardStepResult.failed(1, "数据完整性检查", message, message);
            }
            return WizardStepResult.success(1, "数据完整性检查", "数据完整性检查通过");
        } catch (Exception e) {
            log.warn("数据完整性检查异常, 账套ID={}: {}", accountSetId, e.getMessage(), e);
            return WizardStepResult.failed(1, "数据完整性检查",
                    "数据完整性检查异常: " + e.getMessage(), e.getMessage());
        }
    }

    /**
     * 步骤 3: 结转损益。
     * 复用 {@link PeriodService#carryForward} 生成结转损益凭证。
     * 若本期已存在结转损益凭证(通过 source=1 + 摘要识别),跳过该步并提示。
     */
    private WizardStepResult executeCarryForward(Long accountSetId, int year, int month) {
        try {
            // 幂等校验:本期已存在结转损益凭证则跳过
            if (hasExistingCarryForwardVoucher(accountSetId, year, month)) {
                return WizardStepResult.skipped(3, "结转损益", "已存在结转损益凭证,无需重复结转");
            }
            // 执行结转损益(复用 PeriodService.carryForward)
            periodService.carryForward(accountSetId, year, month);
            // 查询刚刚生成的结转损益凭证(source=1),回填 voucherId 供前端跳转
            Voucher carryVoucher = getLatestCarryForwardVoucher(accountSetId, year, month);
            if (carryVoucher != null) {
                return WizardStepResult.success(3, "结转损益",
                        "结转损益凭证已生成,凭证号: " + carryVoucher.getVoucherNo(),
                        carryVoucher.getId());
            }
            // carryForward 在本期无损益发生时静默返回(不生成凭证)
            return WizardStepResult.success(3, "结转损益", "本期无损益发生,无需结转");
        } catch (BusinessException e) {
            log.warn("结转损益失败(BusinessException), 账套ID={}: {}", accountSetId, e.getMessage());
            return WizardStepResult.failed(3, "结转损益",
                    "结转损益失败: " + e.getMessage(), e.getMessage());
        } catch (Exception e) {
            log.warn("结转损益失败(异常), 账套ID={}", accountSetId, e);
            return WizardStepResult.failed(3, "结转损益",
                    "结转损益失败: " + e.getMessage(), e.getMessage());
        }
    }

    /**
     * 步骤 6: 结账。
     * 调用 {@link PeriodService#closePeriod} 完成结账。
     * closePeriod 在存在未审核/未过账凭证时返回 success=false(不抛异常),
     * 此处将其转为 FAILED 状态并触发回滚。
     */
    private WizardStepResult executeClosePeriod(Long accountSetId, int year, int month) {
        try {
            ClosePeriodResultVO closeResult = periodService.closePeriod(accountSetId, year, month);
            if (closeResult != null && closeResult.isSuccess()) {
                return WizardStepResult.success(6, "结账", "结账成功");
            }
            String msg = closeResult != null && closeResult.getMessage() != null
                    ? closeResult.getMessage() : "结账失败";
            return WizardStepResult.failed(6, "结账", msg, msg);
        } catch (BusinessException e) {
            log.warn("结账失败(BusinessException), 账套ID={}: {}", accountSetId, e.getMessage());
            return WizardStepResult.failed(6, "结账",
                    "结账失败: " + e.getMessage(), e.getMessage());
        } catch (Exception e) {
            log.warn("结账失败(异常), 账套ID={}", accountSetId, e);
            return WizardStepResult.failed(6, "结账",
                    "结账失败: " + e.getMessage(), e.getMessage());
        }
    }

    /**
     * 步骤 7: 下月开启。
     * 自动创建下月会计期间(如果不存在),状态为"开"。已存在则跳过。
     * 跨年场景(12月)自动滚到次年1月。
     * <p>
     * P5.2.2 跨年结转向导化:当当前结账月份为12月时,自动调用
     * {@link PeriodService#carryForwardYear} 执行年度结转(创建次年12个期间 + 3103→3104结转 + 余额结转)。
     * carryForwardYear 会创建次年1月期间,故其成功后下方"创建下月期间"逻辑会因已存在而跳过,避免重复创建。
     * 年度结转失败不影响主流程(下月期间创建),仅在结果中标记"年度结转失败,请手动执行"。
     */
    private WizardStepResult executeOpenNextPeriod(Long accountSetId, int year, int month) {
        try {
            int nextYear = (month == 12) ? year + 1 : year;
            int nextMonth = (month == 12) ? 1 : month + 1;

            // P5.2.2 跨年结转:12月结账时自动触发年度结转
            boolean yearCarryForwardDone = false;
            boolean yearCarryForwardFailed = false;
            boolean nextYearAlreadyExists = false;
            if (month == 12) {
                // 先检查次年期间是否已存在:已存在则跳过年度结转
                // (避免 carryForwardYear 抛"下一年度会计期间已存在"及重复创建)
                LambdaQueryWrapper<AccountPeriod> nextYearWrapper = new LambdaQueryWrapper<>();
                nextYearWrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
                               .eq(AccountPeriod::getYear, nextYear);
                Long nextYearCount = accountPeriodMapper.selectCount(nextYearWrapper);
                if (nextYearCount != null && nextYearCount > 0) {
                    nextYearAlreadyExists = true;
                    log.info("次年{}会计期间已存在,跳过年度结转, 账套ID={}", nextYear, accountSetId);
                } else {
                    log.info("12月结账触发年度结转, 账套ID={}, 来源年度={}", accountSetId, year);
                    try {
                        periodService.carryForwardYear(accountSetId, year);
                        yearCarryForwardDone = true;
                        log.info("年度结转成功, 账套ID={}, 来源年度={}, 目标年度={}", accountSetId, year, nextYear);
                    } catch (Exception e) {
                        // 年度结转失败不影响主流程(下月期间创建),仅记录告警并标记
                        yearCarryForwardFailed = true;
                        log.warn("年度结转失败,请手动执行, 账套ID={}, 年度={}: {}", accountSetId, year, e.getMessage(), e);
                    }
                }
            }

            // 检查下月期间是否已存在(年度结转成功后次年1月已由 carryForwardYear 创建,此处会跳过创建)
            LambdaQueryWrapper<AccountPeriod> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
                    .eq(AccountPeriod::getYear, nextYear)
                    .eq(AccountPeriod::getMonth, nextMonth);
            Long existCount = accountPeriodMapper.selectCount(wrapper);

            boolean nextPeriodCreated = false;
            String periodMessage;
            if (existCount != null && existCount > 0) {
                periodMessage = nextYear + "年" + nextMonth + "月会计期间已存在,无需重复创建";
            } else {
                // 创建下月会计期间,状态为"开"
                AccountPeriod nextPeriod = new AccountPeriod();
                nextPeriod.setAccountSetId(accountSetId);
                nextPeriod.setYear(nextYear);
                nextPeriod.setMonth(nextMonth);
                nextPeriod.setStartDate(LocalDate.of(nextYear, nextMonth, 1));
                nextPeriod.setEndDate(LocalDate.of(nextYear, nextMonth, 1).plusMonths(1).minusDays(1));
                nextPeriod.setStatus(PeriodStatus.OPEN.getCode());
                accountPeriodMapper.insert(nextPeriod);
                nextPeriodCreated = true;
                periodMessage = nextYear + "年" + nextMonth + "月会计期间已创建";
            }

            // 拼接年度结转结果到步骤消息
            StringBuilder message = new StringBuilder(periodMessage);
            if (yearCarryForwardDone) {
                message.append(";年度结转已执行(次年12个期间+余额结转)");
            } else if (yearCarryForwardFailed) {
                message.append(";年度结转失败,请手动执行");
            } else if (nextYearAlreadyExists) {
                message.append(";次年期间已存在,跳过年度结转");
            }

            // 步骤状态:执行了年度结转或新建了下月期间视为成功;均已存在则为跳过
            if (yearCarryForwardDone || nextPeriodCreated) {
                return WizardStepResult.success(7, "下月开启", message.toString());
            }
            return WizardStepResult.skipped(7, "下月开启", message.toString());
        } catch (Exception e) {
            log.warn("下月开启失败, 账套ID={}", accountSetId, e);
            return WizardStepResult.failed(7, "下月开启",
                    "下月开启失败: " + e.getMessage(), e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 构造可选步骤结果:已中止时显示"前序步骤失败",否则按传入的成功/跳过消息。
     */
    private WizardStepResult buildSkippedStep(int stepNo, boolean aborted, String stepName, String normalMessage) {
        if (aborted) {
            return WizardStepResult.skipped(stepNo, stepName, "前序步骤失败,本步骤已跳过");
        }
        return WizardStepResult.skipped(stepNo, stepName, normalMessage);
    }

    /**
     * 检查本期是否已存在结转损益凭证。
     * 识别规则:source=1(系统生成)且凭证明细摘要为"结转本年利润"或"结转本期损益"。
     */
    private boolean hasExistingCarryForwardVoucher(Long accountSetId, int year, int month) {
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getYear, year)
                .eq(Voucher::getMonth, month)
                .eq(Voucher::getSource, 1);
        List<Voucher> systemVouchers = voucherMapper.selectList(voucherWrapper);
        if (systemVouchers.isEmpty()) {
            return false;
        }
        List<Long> voucherIds = systemVouchers.stream()
                .map(Voucher::getId).collect(Collectors.toList());
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                .in(VoucherDetail::getSummary, CARRY_FORWARD_SUMMARY_LEGACY, CARRY_FORWARD_SUMMARY_NEW);
        Long carryCount = voucherDetailMapper.selectCount(detailWrapper);
        return carryCount != null && carryCount > 0;
    }

    /**
     * 查询本期最新的结转损益凭证(source=1)。
     * 用于步骤 3 执行后回填 voucherId。
     */
    private Voucher getLatestCarryForwardVoucher(Long accountSetId, int year, int month) {
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getYear, year)
                .eq(Voucher::getMonth, month)
                .eq(Voucher::getSource, 1)
                .orderByDesc(Voucher::getId)
                .last("LIMIT 1");
        return voucherMapper.selectOne(wrapper);
    }

    /**
     * 查询本期会计期间(不存在时返回 null,不抛异常)。
     */
    private AccountPeriod findPeriod(Long accountSetId, int year, int month) {
        LambdaQueryWrapper<AccountPeriod> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
                .eq(AccountPeriod::getYear, year)
                .eq(AccountPeriod::getMonth, month);
        return accountPeriodMapper.selectOne(wrapper);
    }

    /**
     * 汇总各步骤状态并设置 overallStatus / 各计数。
     * hasFailure=true 时整体状态为 failed。
     */
    private void applySummary(PeriodCloseWizardVO vo, boolean hasFailure) {
        List<WizardStepResult> steps = vo.getSteps();
        int success = 0, failed = 0, skipped = 0;
        for (WizardStepResult s : steps) {
            if (WizardStepResult.STATUS_SUCCESS.equals(s.getStatus())) {
                success++;
            } else if (WizardStepResult.STATUS_FAILED.equals(s.getStatus())) {
                failed++;
            } else if (WizardStepResult.STATUS_SKIPPED.equals(s.getStatus())) {
                skipped++;
            }
        }
        vo.setSuccessCount(success);
        vo.setFailedCount(failed);
        vo.setSkippedCount(skipped);

        if (hasFailure || failed > 0) {
            vo.setOverallStatus(PeriodCloseWizardVO.OVERALL_FAILED);
        } else if (skipped > 0) {
            vo.setOverallStatus(PeriodCloseWizardVO.OVERALL_PARTIAL);
        } else {
            vo.setOverallStatus(PeriodCloseWizardVO.OVERALL_SUCCESS);
        }
    }
}
