package com.company.daizhang.module.bank.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.bank.dto.ApplySuggestionsRequest;
import com.company.daizhang.module.bank.entity.BankMatchHistory;
import com.company.daizhang.module.bank.entity.BankTransaction;
import com.company.daizhang.module.bank.mapper.BankMatchHistoryMapper;
import com.company.daizhang.module.bank.mapper.BankTransactionMapper;
import com.company.daizhang.module.bank.service.SmartReconciliationService;
import com.company.daizhang.module.bank.vo.MatchHistoryPatternVO;
import com.company.daizhang.module.bank.vo.MatchResultVO;
import com.company.daizhang.module.bank.vo.MatchSuggestionVO;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 智能对账增强服务实现
 * <p>
 * 评分模型核心思路:对每条未匹配流水,遍历所有候选凭证(已过账,且日期在 ±3 天窗口内),
 * 计算多维评分,选取得分最高的凭证作为匹配建议。评分维度见 {@link MatchSuggestionVO#getScore()}。
 * <p>
 * 历史模式:每次 applySuggestions 时自动 upsert bank_match_history(同账套+同对方),
 * 后续 smartMatch 时若同对方+相似金额命中历史模式则额外加 25 分。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmartReconciliationServiceImpl implements SmartReconciliationService {

    /**
     * 银行存款科目编码 — 仅匹配该科目的借贷方金额,与 autoMatch 保持口径一致
     */
    private static final String BANK_SUBJECT_CODE = "1002";

    /**
     * 金额模糊匹配容差(±0.01)
     */
    private static final BigDecimal AMOUNT_FUZZY_TOLERANCE = new BigDecimal("0.01");

    /**
     * 日期接近阈值(±3 天)
     */
    private static final long DATE_NEAR_DAYS = 3;

    /**
     * 摘要相似度阈值
     */
    private static final double SUMMARY_SIMILARITY_HIGH = 0.8;
    private static final double SUMMARY_SIMILARITY_MID = 0.5;

    /**
     * 分数阈值(决定 matchType)
     */
    private static final int SCORE_STRONG = 80;
    private static final int SCORE_SUGGEST = 60;

    /**
     * 评分上限
     */
    private static final int SCORE_MAX = 100;

    private final BankTransactionMapper bankTransactionMapper;
    private final VoucherMapper voucherMapper;
    private final VoucherDetailMapper voucherDetailMapper;
    private final BankMatchHistoryMapper bankMatchHistoryMapper;
    private final AccountSetAccessService accountSetAccessService;

    @Override
    public MatchResultVO smartMatch(Long accountSetId, String bankAccount, Integer year, Integer month) {
        // IDOR治理:校验当前用户对该账套的访问权(智能匹配属只读建议,ACCESS 级即可)
        accountSetAccessService.checkAccess(accountSetId);
        if (StrUtil.isBlank(bankAccount)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "银行账号不能为空");
        }
        if (year == null || month == null || month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "年度/月份参数不正确");
        }

        // 1. 查询该月未匹配的银行流水
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        LambdaQueryWrapper<BankTransaction> txWrapper = new LambdaQueryWrapper<>();
        txWrapper.eq(BankTransaction::getAccountSetId, accountSetId)
                 .eq(BankTransaction::getBankAccount, bankAccount)
                 .eq(BankTransaction::getMatchedStatus, 0)
                 .ge(BankTransaction::getTransactionDate, startDate)
                 .le(BankTransaction::getTransactionDate, endDate);
        List<BankTransaction> unmatchedTransactions = bankTransactionMapper.selectList(txWrapper);

        // 2. 查询候选凭证:为覆盖 ±3 天日期窗口,查询该月及前后各 1 个月已过账凭证(status=2)
        LocalDate voucherStart = startDate.minusMonths(1);
        LocalDate voucherEnd = endDate.plusMonths(1);
        LambdaQueryWrapper<Voucher> vWrapper = new LambdaQueryWrapper<>();
        vWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getStatus, 2)
                .ge(Voucher::getVoucherDate, voucherStart)
                .le(Voucher::getVoucherDate, voucherEnd);
        List<Voucher> candidateVouchers = voucherMapper.selectList(vWrapper);

        MatchResultVO result = new MatchResultVO();
        if (unmatchedTransactions.isEmpty() || candidateVouchers.isEmpty()) {
            result.setMatched(new ArrayList<>());
            result.setUnmatchedTransactions(unmatchedTransactions.stream()
                    .map(BankTransaction::getId).collect(Collectors.toList()));
            result.setUnmatchedVouchers(candidateVouchers.stream()
                    .map(Voucher::getId).collect(Collectors.toList()));
            result.setTotalMatched(0);
            result.setTotalUnmatched(unmatchedTransactions.size() + candidateVouchers.size());
            return result;
        }

        // 3. 查询凭证明细(仅银行存款科目),按凭证ID分组
        List<Long> voucherIds = candidateVouchers.stream().map(Voucher::getId).collect(Collectors.toList());
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                     .eq(VoucherDetail::getSubjectCode, BANK_SUBJECT_CODE);
        List<VoucherDetail> bankDetails = voucherDetailMapper.selectList(detailWrapper);
        Map<Long, List<VoucherDetail>> detailsByVoucherId = bankDetails.stream()
                .collect(Collectors.groupingBy(VoucherDetail::getVoucherId));

        // 4. 预加载历史模式(避免每条流水逐条查询数据库)
        Map<String, BankMatchHistory> historyMap = loadHistoryMap(accountSetId);

        // 5. 评分:对每条流水,遍历所有候选凭证,选取得分最高的作为建议
        List<MatchSuggestionVO> suggestions = new ArrayList<>();
        // 已被建议匹配的凭证ID集合(一个凭证至多建议匹配一条流水,避免一对多歧义)
        Set<Long> suggestedVoucherIds = new HashSet<>();

        // 按流水日期升序处理,先入先匹配,提高匹配质量
        unmatchedTransactions.sort(Comparator.comparing(BankTransaction::getTransactionDate,
                Comparator.nullsLast(Comparator.naturalOrder())));

        for (BankTransaction transaction : unmatchedTransactions) {
            Integer txType = transaction.getTransactionType();
            // transactionType 为空无法判断借贷方向,跳过(与原 autoMatch 保持一致)
            if (txType == null) {
                continue;
            }
            MatchSuggestionVO best = null;
            for (Voucher voucher : candidateVouchers) {
                if (suggestedVoucherIds.contains(voucher.getId())) {
                    continue;
                }
                List<VoucherDetail> details = detailsByVoucherId.get(voucher.getId());
                if (details == null || details.isEmpty()) {
                    continue;
                }
                MatchSuggestionVO suggestion = scorePair(transaction, voucher, details, txType, historyMap);
                if (suggestion == null) {
                    continue;
                }
                if (best == null || suggestion.getScore() > best.getScore()) {
                    best = suggestion;
                }
            }
            if (best != null && best.getScore() > 0) {
                suggestions.add(best);
                suggestedVoucherIds.add(best.getVoucherId());
            }
        }

        // 按分数倒序
        suggestions.sort(Comparator.comparing(MatchSuggestionVO::getScore, Comparator.reverseOrder()));

        // 6. 构造返回结果(matched 直接复用完整建议列表,前端一次渲染全部信息)
        Set<Long> matchedTxIds = suggestions.stream()
                .map(MatchSuggestionVO::getTransactionId).collect(Collectors.toSet());
        List<Long> unmatchedTxIds = unmatchedTransactions.stream()
                .map(BankTransaction::getId)
                .filter(id -> !matchedTxIds.contains(id))
                .collect(Collectors.toList());
        List<Long> unmatchedVoucherIds = candidateVouchers.stream()
                .map(Voucher::getId)
                .filter(id -> !suggestedVoucherIds.contains(id))
                .collect(Collectors.toList());

        result.setMatched(suggestions);
        result.setUnmatchedTransactions(unmatchedTxIds);
        result.setUnmatchedVouchers(unmatchedVoucherIds);
        result.setTotalMatched(suggestions.size());
        result.setTotalUnmatched(unmatchedTxIds.size() + unmatchedVoucherIds.size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer applySuggestions(ApplySuggestionsRequest request) {
        Long accountSetId = request.getAccountSetId();
        // IDOR治理:应用建议属写操作,需 OWNER 级权限,防止跨账套回写匹配关系
        accountSetAccessService.checkOwner(accountSetId);
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return 0;
        }

        // 收集所有涉及的流水ID与凭证ID
        List<Long> txIds = request.getItems().stream()
                .map(ApplySuggestionsRequest.ApplyItem::getTransactionId)
                .distinct().collect(Collectors.toList());
        List<Long> voucherIds = request.getItems().stream()
                .map(ApplySuggestionsRequest.ApplyItem::getVoucherId)
                .distinct().collect(Collectors.toList());

        // 批量查询流水与凭证,转 Map 便于校验
        List<BankTransaction> transactions = bankTransactionMapper.selectBatchIds(txIds);
        Map<Long, BankTransaction> txMap = transactions.stream()
                .collect(Collectors.toMap(BankTransaction::getId, t -> t));
        List<Voucher> vouchers = voucherMapper.selectBatchIds(voucherIds);
        Map<Long, Voucher> voucherMap = vouchers.stream()
                .collect(Collectors.toMap(Voucher::getId, v -> v));

        int applied = 0;
        for (ApplySuggestionsRequest.ApplyItem item : request.getItems()) {
            BankTransaction transaction = txMap.get(item.getTransactionId());
            if (transaction == null) {
                throw new BusinessException("银行流水不存在: " + item.getTransactionId());
            }
            // 校验流水归属当前账套(IDOR 兜底)
            if (!accountSetId.equals(transaction.getAccountSetId())) {
                throw new BusinessException("银行流水不属于当前账套: " + item.getTransactionId());
            }
            // 已匹配流水不可重复勾对,跳过而非报错(允许部分成功的批量应用)
            if (transaction.getMatchedStatus() != null && transaction.getMatchedStatus() == 1) {
                log.warn("流水 {} 已匹配,跳过", item.getTransactionId());
                continue;
            }

            Voucher voucher = voucherMap.get(item.getVoucherId());
            if (voucher == null) {
                throw new BusinessException("凭证不存在: " + item.getVoucherId());
            }
            // 凭证必须已过账才能参与对账,与 autoMatch/manualMatch 保持一致
            if (voucher.getStatus() == null || voucher.getStatus() != 2) {
                throw new BusinessException("凭证未过账,不可对账: " + item.getVoucherId());
            }
            if (!accountSetId.equals(voucher.getAccountSetId())) {
                throw new BusinessException("凭证不属于当前账套: " + item.getVoucherId());
            }

            // 回写匹配关系
            transaction.setMatchedStatus(1);
            transaction.setVoucherId(voucher.getId());
            bankTransactionMapper.updateById(transaction);
            applied++;

            // 自动学习历史模式(每次匹配后 upsert)
            learnFromMatch(transaction.getId(), voucher.getId());
        }
        log.info("应用智能匹配建议完成,accountSetId={},成功={}", accountSetId, applied);
        return applied;
    }

    @Override
    public List<MatchHistoryPatternVO> getMatchPatterns(Long accountSetId) {
        // IDOR治理:查询模式属只读,ACCESS 级即可
        accountSetAccessService.checkAccess(accountSetId);
        LambdaQueryWrapper<BankMatchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankMatchHistory::getAccountSetId, accountSetId)
               .orderByDesc(BankMatchHistory::getMatchCount);
        List<BankMatchHistory> list = bankMatchHistoryMapper.selectList(wrapper);
        return list.stream().map(this::toPatternVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer learnPatterns(Long accountSetId) {
        // IDOR治理:学习属写操作(会 upsert bank_match_history),需 OWNER 级权限
        accountSetAccessService.checkOwner(accountSetId);

        // 扫描该账套所有已匹配银行流水(matched_status=1 且 voucher_id 非空)
        LambdaQueryWrapper<BankTransaction> txWrapper = new LambdaQueryWrapper<>();
        txWrapper.eq(BankTransaction::getAccountSetId, accountSetId)
                 .eq(BankTransaction::getMatchedStatus, 1)
                 .isNotNull(BankTransaction::getVoucherId);
        List<BankTransaction> matchedTxList = bankTransactionMapper.selectList(txWrapper);
        if (matchedTxList.isEmpty()) {
            log.info("账套 {} 暂无可学习的已匹配流水", accountSetId);
            return 0;
        }

        // 批量查询关联凭证,用于读取凭证日期与对应科目编码
        List<Long> voucherIds = matchedTxList.stream()
                .map(BankTransaction::getVoucherId).distinct().collect(Collectors.toList());
        List<Voucher> vouchers = voucherMapper.selectBatchIds(voucherIds);
        Map<Long, Voucher> voucherMap = vouchers.stream()
                .collect(Collectors.toMap(Voucher::getId, v -> v));

        // 查询这些凭证的银行存款明细,获取对应科目编码
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                     .eq(VoucherDetail::getSubjectCode, BANK_SUBJECT_CODE);
        List<VoucherDetail> bankDetails = voucherDetailMapper.selectList(detailWrapper);
        Map<Long, String> voucherSubjectMap = bankDetails.stream()
                .collect(Collectors.toMap(VoucherDetail::getVoucherId,
                        VoucherDetail::getSubjectCode, (a, b) -> a));

        int count = 0;
        for (BankTransaction tx : matchedTxList) {
            Voucher voucher = voucherMap.get(tx.getVoucherId());
            if (voucher == null) {
                continue;
            }
            String subjectCode = voucherSubjectMap.get(tx.getVoucherId());
            if (subjectCode == null) {
                subjectCode = BANK_SUBJECT_CODE;
            }
            learnFromMatchInternal(accountSetId, tx, voucher, subjectCode);
            count++;
        }
        log.info("历史模式学习完成,accountSetId={},处理流水数={}", accountSetId, count);
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void learnFromMatch(Long transactionId, Long voucherId) {
        BankTransaction transaction = bankTransactionMapper.selectById(transactionId);
        if (transaction == null) {
            log.warn("learnFromMatch: 银行流水不存在,跳过学习,transactionId={}", transactionId);
            return;
        }
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            log.warn("learnFromMatch: 凭证不存在,跳过学习,voucherId={}", voucherId);
            return;
        }
        // 查询该凭证的银行存款明细,获取实际科目编码(可能是 1002 的下级明细)
        LambdaQueryWrapper<VoucherDetail> dw = new LambdaQueryWrapper<>();
        dw.eq(VoucherDetail::getVoucherId, voucherId)
          .eq(VoucherDetail::getSubjectCode, BANK_SUBJECT_CODE);
        List<VoucherDetail> details = voucherDetailMapper.selectList(dw);
        String subjectCode = details.isEmpty() ? BANK_SUBJECT_CODE : details.get(0).getSubjectCode();
        learnFromMatchInternal(transaction.getAccountSetId(), transaction, voucher, subjectCode);
    }

    // ==================== 评分核心 ====================

    /**
     * 对一条流水与一张凭证(含其银行存款明细列表)进行评分,
     * 取明细中得分最高的一项作为该凭证的最终得分。
     *
     * @return 评分建议对象;若金额维度都不匹配且其他维度也无命中,可能返回 score=0 的建议
     */
    private MatchSuggestionVO scorePair(BankTransaction transaction, Voucher voucher,
                                         List<VoucherDetail> bankDetails, Integer txType,
                                         Map<String, BankMatchHistory> historyMap) {
        BigDecimal txAmount = transaction.getAmount();
        LocalDate txDate = transaction.getTransactionDate();
        String txSummary = StrUtil.isNotBlank(transaction.getSummary()) ? transaction.getSummary() : "";
        String txCounterparty = transaction.getCounterparty();

        VoucherDetail bestDetail = null;
        int bestScore = -1;
        List<String> bestReasons = null;
        BigDecimal bestDetailAmount = null;

        for (VoucherDetail detail : bankDetails) {
            // 根据流水类型取对应借贷方金额:收入=借方,支出=贷方
            BigDecimal detailAmount = txType == 1 ? detail.getDebit() : detail.getCredit();
            if (detailAmount == null) {
                continue;
            }

            int score = 0;
            List<String> reasons = new ArrayList<>(6);

            // 1. 金额精确匹配 50 / 模糊匹配 40 (互斥,优先精确)
            int cmp = detailAmount.compareTo(txAmount);
            if (cmp == 0) {
                score += 50;
                reasons.add("金额精确匹配 +50");
            } else if (detailAmount.subtract(txAmount).abs().compareTo(AMOUNT_FUZZY_TOLERANCE) <= 0) {
                score += 40;
                reasons.add("金额模糊匹配 +40");
            } else {
                // 金额差距过大,该明细不参与匹配(与 autoMatch 行为一致,避免明显错误建议)
                continue;
            }

            // 2. 日期同日 20 / 接近 15 (互斥)
            if (txDate != null && voucher.getVoucherDate() != null) {
                long daysDiff = Math.abs(ChronoUnit.DAYS.between(txDate, voucher.getVoucherDate()));
                if (daysDiff == 0) {
                    score += 20;
                    reasons.add("日期同日 +20");
                } else if (daysDiff <= DATE_NEAR_DAYS) {
                    score += 15;
                    reasons.add("日期接近 ±" + daysDiff + "天 +15");
                }
            }

            // 3. 摘要相似度 ≥0.8 30 / ≥0.5 20 (互斥)
            String detailSummary = StrUtil.isNotBlank(detail.getSummary()) ? detail.getSummary() : "";
            double similarity = summarySimilarity(txSummary, detailSummary);
            if (similarity >= SUMMARY_SIMILARITY_HIGH) {
                score += 30;
                reasons.add("摘要相似度 " + String.format("%.2f", similarity) + " +30");
            } else if (similarity >= SUMMARY_SIMILARITY_MID) {
                score += 20;
                reasons.add("摘要相似度 " + String.format("%.2f", similarity) + " +20");
            }

            // 4. 对方单位匹配 15
            if (StrUtil.isNotBlank(txCounterparty) && StrUtil.isNotBlank(detailSummary)) {
                if (detailSummary.contains(txCounterparty) || txCounterparty.contains(detailSummary)) {
                    score += 15;
                    reasons.add("对方单位匹配 +15");
                }
            }

            // 5. 历史模式匹配 25
            if (StrUtil.isNotBlank(txCounterparty)) {
                BankMatchHistory history = historyMap.get(txCounterparty);
                if (history != null && matchHistoryRange(history, txAmount)) {
                    score += 25;
                    reasons.add("历史模式匹配 +25");
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestDetail = detail;
                bestReasons = reasons;
                bestDetailAmount = detailAmount;
            }
        }

        if (bestDetail == null || bestScore <= 0) {
            return null;
        }

        // cap 到 100
        int finalScore = Math.min(SCORE_MAX, bestScore);

        MatchSuggestionVO vo = new MatchSuggestionVO();
        vo.setTransactionId(transaction.getId());
        vo.setVoucherId(voucher.getId());
        vo.setScore(finalScore);
        vo.setReasons(bestReasons);
        vo.setMatchType(determineMatchType(finalScore));
        vo.setMatchTypeName(determineMatchTypeName(finalScore));
        vo.setTransactionDate(txDate);
        vo.setTransactionAmount(txAmount);
        vo.setTransactionType(transaction.getTransactionType());
        vo.setCounterparty(txCounterparty);
        vo.setTransactionSummary(transaction.getSummary());
        vo.setVoucherNo(voucher.getVoucherNo());
        vo.setVoucherDate(voucher.getVoucherDate());
        vo.setVoucherSummary(bestDetail.getSummary());
        vo.setVoucherAmount(bestDetailAmount);
        return vo;
    }

    /**
     * 根据分数段确定 matchType
     */
    private String determineMatchType(int score) {
        if (score >= SCORE_STRONG) {
            return "exact";
        }
        if (score >= SCORE_SUGGEST) {
            return "fuzzy";
        }
        return "suggested";
    }

    private String determineMatchTypeName(int score) {
        if (score >= SCORE_STRONG) {
            return "强烈建议";
        }
        if (score >= SCORE_SUGGEST) {
            return "建议匹配";
        }
        return "不推荐";
    }

    /**
     * 判断流水金额是否落在历史模式的金额范围内
     */
    private boolean matchHistoryRange(BankMatchHistory history, BigDecimal amount) {
        BigDecimal min = history.getAmountRangeMin();
        BigDecimal max = history.getAmountRangeMax();
        if (min == null || max == null) {
            return false;
        }
        // compareTo 避免 1.0 vs 1.00 之类的精度问题
        return amount.compareTo(min) >= 0 && amount.compareTo(max) <= 0;
    }

    // ==================== 历史模式学习 ====================

    /**
     * 学习内部实现:upsert bank_match_history,扩展金额范围[min,max]并累加匹配次数。
     */
    private void learnFromMatchInternal(Long accountSetId, BankTransaction transaction,
                                         Voucher voucher, String subjectCode) {
        String counterparty = transaction.getCounterparty();
        // 无交易对方无法形成模式,跳过
        if (StrUtil.isBlank(counterparty)) {
            return;
        }
        BigDecimal amount = transaction.getAmount();
        if (amount == null) {
            return;
        }

        LambdaQueryWrapper<BankMatchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankMatchHistory::getAccountSetId, accountSetId)
               .eq(BankMatchHistory::getCounterparty, counterparty);
        BankMatchHistory existing = bankMatchHistoryMapper.selectOne(wrapper);

        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            BankMatchHistory history = new BankMatchHistory();
            history.setAccountSetId(accountSetId);
            history.setCounterparty(counterparty);
            history.setAmountRangeMin(amount);
            history.setAmountRangeMax(amount);
            history.setVoucherSubjectCode(subjectCode);
            history.setMatchCount(1);
            history.setLastMatchedAt(now);
            bankMatchHistoryMapper.insert(history);
        } else {
            // 扩展金额范围:min 取较小,max 取较大
            BigDecimal newMin = existing.getAmountRangeMin() == null
                    ? amount : existing.getAmountRangeMin().min(amount);
            BigDecimal newMax = existing.getAmountRangeMax() == null
                    ? amount : existing.getAmountRangeMax().max(amount);
            existing.setAmountRangeMin(newMin);
            existing.setAmountRangeMax(newMax);
            existing.setVoucherSubjectCode(subjectCode);
            existing.setMatchCount((existing.getMatchCount() == null ? 0 : existing.getMatchCount()) + 1);
            existing.setLastMatchedAt(now);
            bankMatchHistoryMapper.updateById(existing);
        }
    }

    /**
     * 预加载该账套下所有历史模式,以 counterparty 为 key 便于 O(1) 查询
     */
    private Map<String, BankMatchHistory> loadHistoryMap(Long accountSetId) {
        LambdaQueryWrapper<BankMatchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankMatchHistory::getAccountSetId, accountSetId);
        List<BankMatchHistory> list = bankMatchHistoryMapper.selectList(wrapper);
        Map<String, BankMatchHistory> map = new HashMap<>(list.size());
        for (BankMatchHistory h : list) {
            map.put(h.getCounterparty(), h);
        }
        return map;
    }

    private MatchHistoryPatternVO toPatternVO(BankMatchHistory history) {
        MatchHistoryPatternVO vo = new MatchHistoryPatternVO();
        vo.setId(history.getId());
        vo.setAccountSetId(history.getAccountSetId());
        vo.setCounterparty(history.getCounterparty());
        vo.setAmountRangeMin(history.getAmountRangeMin());
        vo.setAmountRangeMax(history.getAmountRangeMax());
        vo.setVoucherSubjectCode(history.getVoucherSubjectCode());
        vo.setMatchCount(history.getMatchCount());
        vo.setLastMatchedAt(history.getLastMatchedAt());
        vo.setCreateTime(history.getCreateTime());
        return vo;
    }

    // ==================== 文本相似度算法(JDK 自实现) ====================
    // 注:Apache Commons Text 未引入,这里用 JDK 实现简单版 Levenshtein 距离与 Jaccard 相似度。

    /**
     * 摘要相似度:取 Levenshtein 相似度与 Jaccard 相似度的最大值,
     * 兼顾字符级编辑距离(短文本)与字符 bigram 重叠(长文本/中文)。
     */
    private double summarySimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.isEmpty() && s2.isEmpty()) {
            return 1.0;
        }
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }
        double levSim = levenshteinSimilarity(s1, s2);
        double jacSim = jaccardSimilarity(s1, s2);
        return Math.max(levSim, jacSim);
    }

    /**
     * Levenshtein 相似度 = 1 - editDistance / maxLen
     */
    private double levenshteinSimilarity(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) {
            return 1.0;
        }
        int dist = levenshteinDistance(s1, s2);
        return 1.0 - (double) dist / maxLen;
    }

    /**
     * Levenshtein 编辑距离(经典动态规划实现)
     */
    private int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        // 滚动数组优化空间到 O(min(m,n))
        if (m < n) {
            return levenshteinDistance(s2, s1);
        }
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        for (int j = 0; j <= n; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= m; i++) {
            curr[0] = i;
            for (int j = 1; j <= n; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[n];
    }

    /**
     * Jaccard 相似度(基于字符 bigram 集合): |A∩B| / |A∪B|
     * 对中文摘要比 Levenshtein 更鲁棒(长文本编辑距离易失真)。
     */
    private double jaccardSimilarity(String s1, String s2) {
        Set<String> set1 = bigrams(s1);
        Set<String> set2 = bigrams(s2);
        if (set1.isEmpty() && set2.isEmpty()) {
            return 1.0;
        }
        if (set1.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        return (double) intersection.size() / union.size();
    }

    /**
     * 提取字符串的字符 bigram 集合(相邻两字符)
     */
    private Set<String> bigrams(String s) {
        Set<String> set = new HashSet<>(s.length());
        for (int i = 0; i < s.length() - 1; i++) {
            set.add(s.substring(i, i + 2));
        }
        return set;
    }
}
