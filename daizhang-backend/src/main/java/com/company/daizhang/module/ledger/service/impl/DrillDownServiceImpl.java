package com.company.daizhang.module.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.mapper.AccountSetMapper;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.ledger.service.DrillDownService;
import com.company.daizhang.module.ledger.vo.DrillDownResultVO;
import com.company.daizhang.module.ledger.vo.DrillDownResultVO.DrillDownVoucher;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.entity.VoucherDetail;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表钻取服务实现
 * <p>
 * 实现思路:
 * 1. 校验账套访问权限与参数合法性;
 * 2. 查询指定账套/年度/月份的已过账凭证(status=2);
 * 3. JOIN voucher_detail,按 subject_code LIKE 'xx%' 过滤科目范围;
 * 4. 按方向(debit/credit)与金额精确匹配或 ±0.01 容差模糊匹配;
 * 5. 按凭证维度聚合,返回命中的凭证分录列表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DrillDownServiceImpl implements DrillDownService {

    /**
     * 模糊匹配容差:±0.01 元(避免浮点累计误差导致漏匹配)
     */
    private static final BigDecimal FUZZY_TOLERANCE = new BigDecimal("0.01");

    private final VoucherMapper voucherMapper;
    private final VoucherDetailMapper voucherDetailMapper;
    private final AccountSetMapper accountSetMapper;
    private final AccountSetAccessService accountSetAccessService;

    @Override
    @Transactional(readOnly = true)
    public DrillDownResultVO drillDown(Long accountSetId, String subjectCode, Integer year, Integer month,
                                       BigDecimal amount, String direction, Boolean fuzzy) {
        // 业务校验:账套ID不能为空
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }

        // IDOR越权校验:账套读权限
        accountSetAccessService.checkAccess(accountSetId);

        // 业务校验:科目编码不能为空
        if (subjectCode == null || subjectCode.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "科目编码不能为空");
        }

        // 业务校验:年度不能为空且范围合理
        if (year == null || year < 1900 || year > 2099) {
            throw new BusinessException(ErrorCode.LEDGER_YEAR_INVALID);
        }

        // 业务校验:月份必须在1-12之间
        if (month == null || month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.LEDGER_MONTH_INVALID);
        }

        // 业务校验:金额不能为空且不能为负
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "金额不能为空且不能为负数");
        }

        // 业务校验:方向只能为 debit 或 credit
        if (!"debit".equalsIgnoreCase(direction) && !"credit".equalsIgnoreCase(direction)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "方向只能为 debit 或 credit");
        }

        // 业务校验:账套必须存在
        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }

        boolean useFuzzy = Boolean.TRUE.equals(fuzzy);
        boolean byDebit = "debit".equalsIgnoreCase(direction);

        log.info("报表钻取查询: accountSetId={}, subjectCode={}, year={}, month={}, amount={}, direction={}, fuzzy={}",
                accountSetId, subjectCode, year, month, amount, direction, useFuzzy);

        // 步骤1:查询已过账的凭证(status=2)
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getStatus, 2)
                .eq(Voucher::getYear, year)
                .eq(Voucher::getMonth, month)
                .orderByAsc(Voucher::getVoucherDate)
                .orderByAsc(Voucher::getVoucherNo);
        List<Voucher> vouchers = voucherMapper.selectList(voucherWrapper);

        if (vouchers.isEmpty()) {
            log.info("未找到已过账的凭证");
            return buildEmptyResult(subjectCode, amount, direction, useFuzzy);
        }

        List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());

        // 步骤2:查询凭证明细,按科目编码前缀过滤
        // 注:VoucherDetail.subjectCode 存储的是末级科目编码,
        // 通过 LIKE 'xx%' 可命中传入科目及其所有下级科目(例如 "1001" 命中 "1001"、"100101"、"100102" 等)
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                .likeRight(VoucherDetail::getSubjectCode, subjectCode.trim())
                .orderByAsc(VoucherDetail::getSortOrder);
        List<VoucherDetail> details = voucherDetailMapper.selectList(detailWrapper);

        if (details.isEmpty()) {
            log.info("未找到科目范围内的凭证明细");
            return buildEmptyResult(subjectCode, amount, direction, useFuzzy);
        }

        // 步骤3:按方向+金额过滤(精确匹配或 ±0.01 容差模糊匹配)
        List<VoucherDetail> matchedDetails = details.stream()
                .filter(d -> amountMatch(d, amount, byDebit, useFuzzy))
                .collect(Collectors.toList());

        if (matchedDetails.isEmpty()) {
            log.info("未找到金额匹配的凭证分录");
            return buildEmptyResult(subjectCode, amount, direction, useFuzzy);
        }

        // 步骤4:按凭证维度聚合
        Map<Long, Voucher> voucherMap = vouchers.stream()
                .collect(Collectors.toMap(Voucher::getId, v -> v, (a, b) -> a));

        // 使用 LinkedHashMap 保持凭证出现顺序(已按日期、凭证号排序)
        Map<Long, List<VoucherDetail>> groupedByVoucher = new LinkedHashMap<>();
        for (VoucherDetail d : matchedDetails) {
            groupedByVoucher.computeIfAbsent(d.getVoucherId(), k -> new ArrayList<>()).add(d);
        }

        List<DrillDownVoucher> resultVouchers = new ArrayList<>(groupedByVoucher.size());
        for (Map.Entry<Long, List<VoucherDetail>> entry : groupedByVoucher.entrySet()) {
            Voucher voucher = voucherMap.get(entry.getKey());
            if (voucher == null) {
                continue;
            }
            List<VoucherDetail> hits = entry.getValue();

            DrillDownVoucher vo = new DrillDownVoucher();
            vo.setVoucherId(voucher.getId());
            vo.setVoucherNo(voucher.getVoucherNo());
            vo.setVoucherDate(voucher.getVoucherDate());

            // summary:取首条命中分录的摘要作为代表
            VoucherDetail first = hits.get(0);
            vo.setSummary(first.getSummary());

            // debitAmount/creditAmount:命中分录借贷方金额合计(同一凭证可能命中多行)
            BigDecimal debitSum = hits.stream()
                    .map(VoucherDetail::getDebit)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal creditSum = hits.stream()
                    .map(VoucherDetail::getCredit)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            vo.setDebitAmount(debitSum);
            vo.setCreditAmount(creditSum);

            // abstracts:命中分录的摘要列表(去重保序)
            List<String> abstracts = hits.stream()
                    .map(VoucherDetail::getSummary)
                    .filter(s -> s != null && !s.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            vo.setAbstracts(abstracts);

            resultVouchers.add(vo);
        }

        // 二次排序:按凭证日期、凭证号(保险起见,与 voucherWrapper 排序一致)
        resultVouchers.sort(Comparator
                .comparing(DrillDownVoucher::getVoucherDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(DrillDownVoucher::getVoucherNo, Comparator.nullsLast(Comparator.naturalOrder())));

        log.info("报表钻取命中 {} 张凭证", resultVouchers.size());

        DrillDownResultVO result = new DrillDownResultVO();
        result.setVouchers(resultVouchers);
        result.setSubjectCode(subjectCode);
        result.setAmount(amount);
        result.setDirection(direction);
        result.setFuzzy(useFuzzy);
        return result;
    }

    /**
     * 判断单条凭证明细金额是否命中目标金额
     *
     * @param detail   凭证明细
     * @param amount   目标金额
     * @param byDebit  true=按借方匹配,false=按贷方匹配
     * @param useFuzzy 是否模糊匹配
     */
    private boolean amountMatch(VoucherDetail detail, BigDecimal amount, boolean byDebit, boolean useFuzzy) {
        BigDecimal target = byDebit ? detail.getDebit() : detail.getCredit();
        if (target == null || target.compareTo(BigDecimal.ZERO) == 0) {
            // 反方向为零不参与匹配,避免借方查询时把贷方为 0 的行误命中
            return false;
        }
        if (useFuzzy) {
            BigDecimal diff = target.subtract(amount).abs();
            return diff.compareTo(FUZZY_TOLERANCE) <= 0;
        }
        return target.compareTo(amount) == 0;
    }

    /**
     * 构造空结果
     */
    private DrillDownResultVO buildEmptyResult(String subjectCode, BigDecimal amount, String direction, boolean fuzzy) {
        DrillDownResultVO vo = new DrillDownResultVO();
        vo.setVouchers(Collections.emptyList());
        vo.setSubjectCode(subjectCode);
        vo.setAmount(amount);
        vo.setDirection(direction);
        vo.setFuzzy(fuzzy);
        return vo;
    }
}
