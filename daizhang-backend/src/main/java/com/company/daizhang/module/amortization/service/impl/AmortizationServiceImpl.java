package com.company.daizhang.module.amortization.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
import com.company.daizhang.module.amortization.dto.AmortizationRequest;
import com.company.daizhang.module.amortization.entity.Amortization;
import com.company.daizhang.module.amortization.mapper.AmortizationMapper;
import com.company.daizhang.module.amortization.service.AmortizationService;
import com.company.daizhang.module.amortization.vo.AmortizationVO;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.entity.VoucherDetail;
import com.company.daizhang.module.voucher.entity.VoucherWord;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import com.company.daizhang.module.voucher.mapper.VoucherWordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 长期待摊费用服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmortizationServiceImpl implements AmortizationService {

    private final AmortizationMapper amortizationMapper;
    private final VoucherMapper voucherMapper;
    private final VoucherDetailMapper voucherDetailMapper;
    private final SubjectMapper subjectMapper;
    private final AccountPeriodMapper accountPeriodMapper;
    private final VoucherWordMapper voucherWordMapper;

    // 长期待摊费用对应科目编码（1801），管理费用（5602）
    // 注意：SubjectServiceImpl.initDefaultSubjects 中 1801=长期待摊费用, 1901=待处理财产损溢
    private static final String CODE_LONG_TERM_PREPAID = "1801";
    private static final String CODE_MANAGEMENT_EXPENSE = "5602";

    @Override
    public PageResult<AmortizationVO> pageAmortizations(Long accountSetId, String amortizationName, Integer status, int pageNum, int pageSize) {
        Page<Amortization> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Amortization> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(accountSetId != null, Amortization::getAccountSetId, accountSetId)
               .like(StrUtil.isNotBlank(amortizationName), Amortization::getAmortizationName, amortizationName)
               .eq(status != null, Amortization::getStatus, status)
               .orderByDesc(Amortization::getCreateTime);

        Page<Amortization> result = amortizationMapper.selectPage(page, wrapper);

        List<AmortizationVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public AmortizationVO getAmortizationById(Long id) {
        Amortization amortization = amortizationMapper.selectById(id);
        if (amortization == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "长期待摊费用不存在");
        }
        return convertToVO(amortization);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAmortization(AmortizationRequest request) {
        // 校验总月数必须大于0,防止除零异常
        if (request.getTotalMonths() == null || request.getTotalMonths() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "总月数必须大于0");
        }

        Amortization amortization = new Amortization();
        BeanUtil.copyProperties(request, amortization);

        // 计算月摊销额 = 待摊总额 / 总月数
        BigDecimal monthlyAmount = request.getTotalAmount()
                .divide(BigDecimal.valueOf(request.getTotalMonths()), 2, RoundingMode.HALF_UP);
        amortization.setMonthlyAmount(monthlyAmount);

        // 剩余待摊 = 待摊总额
        amortization.setRemainingAmount(request.getTotalAmount());

        // 已摊销额初始化为0
        amortization.setAmortizedAmount(BigDecimal.ZERO);

        // 状态：摊销中
        amortization.setStatus(0);

        amortizationMapper.insert(amortization);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAmortization(Long id, AmortizationRequest request) {
        Amortization amortization = amortizationMapper.selectById(id);
        if (amortization == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "长期待摊费用不存在");
        }

        // 校验总月数必须大于0,防止除零异常
        if (request.getTotalMonths() == null || request.getTotalMonths() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "总月数必须大于0");
        }

        BeanUtil.copyProperties(request, amortization);

        // 重新计算月摊销额
        BigDecimal monthlyAmount = request.getTotalAmount()
                .divide(BigDecimal.valueOf(request.getTotalMonths()), 2, RoundingMode.HALF_UP);
        amortization.setMonthlyAmount(monthlyAmount);

        // 重新计算剩余待摊
        BigDecimal remainingAmount = request.getTotalAmount().subtract(amortization.getAmortizedAmount());
        // 守卫:手动调整导致已摊销额超过待摊总额时,剩余待摊为负会令后续摊销凭证金额为负、报表错乱,归零
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }
        amortization.setRemainingAmount(remainingAmount);

        // 如果剩余待摊 <= 0，更新状态为已摊完(归零即视为已完成,status:0-摊销中 1-已摊完)
        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            amortization.setStatus(1);
        }

        amortizationMapper.updateById(amortization);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAmortization(Long id) {
        Amortization amortization = amortizationMapper.selectById(id);
        if (amortization == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "长期待摊费用不存在");
        }
        amortizationMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void amortize(Long id, Integer year, Integer month) {
        Amortization amortization = amortizationMapper.selectById(id);
        if (amortization == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "长期待摊费用不存在");
        }

        // 检查状态为摊销中
        if (amortization.getStatus() != null && amortization.getStatus() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该费用已摊完，无法继续摊销");
        }

        // 校验该期间是否已生成过摊销凭证,避免重复摊销
        if (existsAmortizationVoucher(id, year, month)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "该费用在" + year + "年" + month + "月已摊销,不能重复摊销");
        }
        // 防重复摊销:校验当前期间是否已摊销过(不依赖凭证,覆盖只调amortize未生成凭证的场景)
        String currentPeriod = String.format("%04d-%02d", year, month);
        if (currentPeriod.equals(amortization.getLastAmortizedPeriod())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "该费用在" + year + "年" + month + "月已摊销,不能重复摊销");
        }

        BigDecimal monthlyAmount = amortization.getMonthlyAmount();
        if (monthlyAmount == null) {
            monthlyAmount = BigDecimal.ZERO;
        }

        // 剩余待摊金额
        BigDecimal remainingAmount = amortization.getRemainingAmount();
        if (remainingAmount == null) {
            remainingAmount = BigDecimal.ZERO;
        }

        // 本次摊销额 = min(月摊销额, 剩余待摊),防止amortizedAmount超过totalAmount
        BigDecimal actualAmount = monthlyAmount.min(remainingAmount);
        if (actualAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "剩余待摊金额为零,无需摊销");
        }

        // 已摊销额 += 本次摊销额
        BigDecimal amortizedAmount = amortization.getAmortizedAmount();
        if (amortizedAmount == null) {
            amortizedAmount = BigDecimal.ZERO;
        }
        amortizedAmount = amortizedAmount.add(actualAmount);
        amortization.setAmortizedAmount(amortizedAmount);

        // 剩余待摊 -= 本次摊销额
        remainingAmount = remainingAmount.subtract(actualAmount);
        amortization.setRemainingAmount(remainingAmount);

        // 如果剩余待摊 <= 0，状态改为已摊完
        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            amortization.setStatus(1);
            // 防止剩余为负数
            amortization.setRemainingAmount(BigDecimal.ZERO);
        }

        // 标记本期间已摊销,防止重复调用
        amortization.setLastAmortizedPeriod(currentPeriod);
        amortizationMapper.updateById(amortization);

        log.info("长期待摊费用摊销成功：id={}, year={}, month={}, 摊销金额={}", id, year, month, actualAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAmortize(Long accountSetId, Integer year, Integer month) {
        LambdaQueryWrapper<Amortization> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Amortization::getAccountSetId, accountSetId)
               .eq(Amortization::getStatus, 0); // 摊销中

        List<Amortization> list = amortizationMapper.selectList(wrapper);

        for (Amortization amortization : list) {
            // 已摊完的跳过
            if (amortization.getStatus() != null && amortization.getStatus() == 1) {
                continue;
            }

            // 跳过该期间已摊销的
            if (existsAmortizationVoucher(amortization.getId(), year, month)) {
                log.warn("批量摊销跳过,已摊销: amortizationId={}, year={}, month={}",
                        amortization.getId(), year, month);
                continue;
            }
            // 防重复:校验当前期间是否已摊销过(不依赖凭证)
            String currentPeriod = String.format("%04d-%02d", year, month);
            if (currentPeriod.equals(amortization.getLastAmortizedPeriod())) {
                log.warn("批量摊销跳过,已摊销: amortizationId={}, year={}, month={}",
                        amortization.getId(), year, month);
                continue;
            }

            BigDecimal monthlyAmount = amortization.getMonthlyAmount();
            if (monthlyAmount == null) {
                monthlyAmount = BigDecimal.ZERO;
            }

            BigDecimal remainingAmount = amortization.getRemainingAmount();
            if (remainingAmount == null) {
                remainingAmount = BigDecimal.ZERO;
            }

            // 本次摊销额 = min(月摊销额, 剩余待摊),防止超额
            BigDecimal actualAmount = monthlyAmount.min(remainingAmount);
            if (actualAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal amortizedAmount = amortization.getAmortizedAmount();
            if (amortizedAmount == null) {
                amortizedAmount = BigDecimal.ZERO;
            }
            amortizedAmount = amortizedAmount.add(actualAmount);
            amortization.setAmortizedAmount(amortizedAmount);

            remainingAmount = remainingAmount.subtract(actualAmount);

            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                amortization.setStatus(1);
                amortization.setRemainingAmount(BigDecimal.ZERO);
            } else {
                amortization.setRemainingAmount(remainingAmount);
            }

            // 标记本期间已摊销,防止重复调用
            amortization.setLastAmortizedPeriod(currentPeriod);
            amortizationMapper.updateById(amortization);
        }

        log.info("批量摊销完成：accountSetId={}, year={}, month={}, 处理数量={}", accountSetId, year, month, list.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateAmortizationVoucher(Long id, Integer year, Integer month) {
        Amortization amortization = amortizationMapper.selectById(id);
        if (amortization == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "长期待摊费用不存在");
        }
        if (amortization.getStatus() != null && amortization.getStatus() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该费用已摊完，无法生成摊销凭证");
        }

        Long accountSetId = amortization.getAccountSetId();

        // 校验该期间是否已生成过摊销凭证,避免重复生成
        if (existsAmortizationVoucher(id, year, month)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "该费用在" + year + "年" + month + "月已生成摊销凭证,不能重复生成");
        }
        // 防重复摊销:若本期间已通过amortize摊销过,则不再生成凭证,避免重复计账
        String currentPeriod = String.format("%04d-%02d", year, month);
        if (currentPeriod.equals(amortization.getLastAmortizedPeriod())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "该费用在" + year + "年" + month + "月已摊销,不能重复摊销");
        }
        BigDecimal monthlyAmount = amortization.getMonthlyAmount() != null
                ? amortization.getMonthlyAmount() : BigDecimal.ZERO;
        if (monthlyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "月摊销额为零，无法生成凭证");
        }

        // 校验会计期间
        AccountPeriod period = checkPeriodExists(accountSetId, year, month);
        checkPeriodNotClosed(period);
        LocalDate voucherDate = LocalDate.of(year, month, period.getEndDate().getDayOfMonth());

        // 获取科目：贷方=长期待摊费用（取自Amortization.subjectId或按编码1801查询）
        Long creditSubjectId = amortization.getSubjectId();
        if (creditSubjectId == null) {
            creditSubjectId = getSubjectIdByCode(accountSetId, CODE_LONG_TERM_PREPAID, "长期待摊费用");
        }
        // 借方=管理费用（按编码5602查询）
        Long debitSubjectId = getSubjectIdByCode(accountSetId, CODE_MANAGEMENT_EXPENSE, "管理费用");

        String summary = "长期待摊费用摊销-" + (StrUtil.isBlank(amortization.getAmortizationName())
                ? "未命名" : amortization.getAmortizationName());

        // 创建凭证
        Voucher voucher = buildVoucher(accountSetId, voucherDate, year, month, monthlyAmount);
        voucherMapper.insert(voucher);

        // 创建凭证明细
        List<VoucherDetail> details = new ArrayList<>();
        // 借：管理费用 月摊销额
        details.add(buildDetail(voucher.getId(), 1, summary, debitSubjectId, monthlyAmount, BigDecimal.ZERO));
        // 贷：长期待摊费用 月摊销额
        details.add(buildDetail(voucher.getId(), 2, summary, creditSubjectId, BigDecimal.ZERO, monthlyAmount));
        for (VoucherDetail detail : details) {
            voucherDetailMapper.insert(detail);
        }

        // 同步更新摊销金额(此前只创建凭证不更新金额,与amortize形成断裂)
        BigDecimal amortizedAmount = amortization.getAmortizedAmount() != null
                ? amortization.getAmortizedAmount() : BigDecimal.ZERO;
        BigDecimal remainingAmount = amortization.getRemainingAmount() != null
                ? amortization.getRemainingAmount() : BigDecimal.ZERO;
        // 本次摊销额 = min(月摊销额, 剩余待摊),防止已摊销额超过待摊总额
        BigDecimal actualAmount = monthlyAmount.min(remainingAmount);
        if (actualAmount.compareTo(BigDecimal.ZERO) > 0) {
            amortization.setAmortizedAmount(amortizedAmount.add(actualAmount));
            BigDecimal newRemaining = remainingAmount.subtract(actualAmount);
            if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                amortization.setRemainingAmount(BigDecimal.ZERO);
                amortization.setStatus(1);
            } else {
                amortization.setRemainingAmount(newRemaining);
            }
            // 标记本期间已摊销,防止重复摊销
            amortization.setLastAmortizedPeriod(currentPeriod);
            amortizationMapper.updateById(amortization);
        }

        log.info("长期待摊费用摊销凭证生成成功：摊销ID={}, 凭证ID={}, 凭证号={}, 金额={}",
                id, voucher.getId(), voucher.getVoucherNo(), monthlyAmount);
        return voucher.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchGenerateAmortizationVouchers(Long accountSetId, Integer year, Integer month) {
        LambdaQueryWrapper<Amortization> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Amortization::getAccountSetId, accountSetId)
               .eq(Amortization::getStatus, 0); // 摊销中
        List<Amortization> list = amortizationMapper.selectList(wrapper);

        List<Long> voucherIds = new ArrayList<>();
        for (Amortization amortization : list) {
            try {
                Long voucherId = generateAmortizationVoucher(amortization.getId(), year, month);
                voucherIds.add(voucherId);
            } catch (BusinessException e) {
                log.warn("批量生成摊销凭证跳过，摊销ID: {}, 原因: {}", amortization.getId(), e.getMessage());
            }
        }
        log.info("批量生成摊销凭证完成：accountSetId={}, year={}月={}, 成功数量={}",
                accountSetId, year, month, voucherIds.size());
        return voucherIds;
    }

    // ==================== 摊销凭证辅助方法 ====================

    /**
     * 校验指定摊销对象在指定期间是否已生成过摊销凭证。
     * 通过查询该期间的凭证中是否存在摘要以"长期待摊费用摊销-"开头且贷方科目为1801的明细来判断。
     * 此方法不依赖额外字段,避免修改数据库schema。
     */
    private boolean existsAmortizationVoucher(Long amortizationId, Integer year, Integer month) {
        Amortization amortization = amortizationMapper.selectById(amortizationId);
        if (amortization == null) {
            return false;
        }
        Long accountSetId = amortization.getAccountSetId();
        // 查询该期间所有凭证(source=1 表示系统生成)
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getYear, year)
                .eq(Voucher::getMonth, month)
                .eq(Voucher::getSource, 1);
        List<Voucher> vouchers = voucherMapper.selectList(voucherWrapper);
        if (vouchers.isEmpty()) {
            return false;
        }
        List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());
        // 查询这些凭证的明细,判断是否存在该摊销对象的摊销凭证(精确匹配"长期待摊费用摊销-{摊销对象名称}")
        // 原like"长期待摊费用摊销-"会误判其他摊销对象为已存在,导致同期只能摊销一个对象
        String exactSummary = "长期待摊费用摊销-" + amortization.getAmortizationName();
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                .eq(VoucherDetail::getSummary, exactSummary)
                .gt(VoucherDetail::getCredit, BigDecimal.ZERO);
        Long count = voucherDetailMapper.selectCount(detailWrapper);
        return count != null && count > 0;
    }

    private AccountPeriod checkPeriodExists(Long accountSetId, Integer year, Integer month) {
        LambdaQueryWrapper<AccountPeriod> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
               .eq(AccountPeriod::getYear, year)
               .eq(AccountPeriod::getMonth, month);
        AccountPeriod period = accountPeriodMapper.selectOne(wrapper);
        if (period == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_SET_PERIOD_NOT_FOUND);
        }
        return period;
    }

    private void checkPeriodNotClosed(AccountPeriod period) {
        // PeriodStatus.CLOSED=1，已结账期间不允许生成凭证
        if (period.getStatus() != null && period.getStatus() == 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_SET_PERIOD_ALREADY_CLOSED);
        }
    }

    private Long getSubjectIdByCode(Long accountSetId, String code, String subjectName) {
        Subject subject = subjectMapper.selectOne(new LambdaQueryWrapper<Subject>()
                .eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getCode, code));
        if (subject == null) {
            // 前缀匹配容错
            String prefix = code.split("\\.")[0];
            subject = subjectMapper.selectOne(new LambdaQueryWrapper<Subject>()
                    .eq(Subject::getAccountSetId, accountSetId)
                    .eq(Subject::getCode, prefix));
            if (subject == null) {
                throw new BusinessException(ErrorCode.VOUCHER_SUBJECT_INVALID.getCode(),
                        "未查询到科目[" + subjectName + "]，编码: " + code);
            }
            log.warn("科目[{}]精确编码{}未找到，使用前缀编码{}替代", subjectName, code, prefix);
        }
        return subject.getId();
    }

    private Voucher buildVoucher(Long accountSetId, LocalDate voucherDate, int year, int month,
                                  BigDecimal totalAmount) {
        Voucher voucher = new Voucher();
        voucher.setAccountSetId(accountSetId);
        voucher.setVoucherWordId(getDefaultVoucherWordId(accountSetId));
        voucher.setVoucherNo(generateVoucherNo(accountSetId, year, month));
        voucher.setVoucherDate(voucherDate);
        voucher.setYear(year);
        voucher.setMonth(month);
        voucher.setTotalDebit(totalAmount);
        voucher.setTotalCredit(totalAmount);
        voucher.setAttachmentCount(0);
        voucher.setStatus(0);
        voucher.setSource(1);
        return voucher;
    }

    private VoucherDetail buildDetail(Long voucherId, int lineNo, String summary, Long subjectId,
                                      BigDecimal debit, BigDecimal credit) {
        VoucherDetail detail = new VoucherDetail();
        detail.setVoucherId(voucherId);
        detail.setLineNo(lineNo);
        detail.setSummary(summary);
        detail.setSubjectId(subjectId);
        Subject subject = subjectMapper.selectById(subjectId);
        if (subject != null) {
            detail.setSubjectCode(subject.getCode());
            detail.setSubjectName(subject.getName());
        }
        detail.setDebit(debit);
        detail.setCredit(credit);
        detail.setSortOrder(lineNo);
        return detail;
    }

    private Long getDefaultVoucherWordId(Long accountSetId) {
        LambdaQueryWrapper<VoucherWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoucherWord::getAccountSetId, accountSetId)
               .orderByAsc(VoucherWord::getId)
               .last("LIMIT 1");
        VoucherWord word = voucherWordMapper.selectOne(wrapper);
        return word != null ? word.getId() : null;
    }

    private String generateVoucherNo(Long accountSetId, Integer year, Integer month) {
        // 查询该期间已有凭证数+1，排除TMP-临时号，避免序号偏大
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Voucher::getAccountSetId, accountSetId)
               .eq(Voucher::getYear, year)
               .eq(Voucher::getMonth, month)
               .notLike(Voucher::getVoucherNo, "TMP-%");
        Long count = voucherMapper.selectCount(wrapper);
        return String.format("%d-%02d-%03d", year, month, count + 1);
    }

    private AmortizationVO convertToVO(Amortization amortization) {
        AmortizationVO vo = new AmortizationVO();
        BeanUtil.copyProperties(amortization, vo);

        // 状态名称
        if (amortization.getStatus() != null) {
            switch (amortization.getStatus()) {
                case 0:
                    vo.setStatusName("摊销中");
                    break;
                case 1:
                    vo.setStatusName("已摊完");
                    break;
            }
        }

        return vo;
    }
}
