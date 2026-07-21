package com.company.daizhang.module.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.ai.dto.RecognitionFeedbackRequest;
import com.company.daizhang.module.ai.entity.AiAccountingRule;
import com.company.daizhang.module.ai.entity.AiRecognitionFeedback;
import com.company.daizhang.module.ai.mapper.AiRecognitionFeedbackMapper;
import com.company.daizhang.module.ai.service.AccountingRuleService;
import com.company.daizhang.module.ai.service.RecognitionFeedbackService;
import com.company.daizhang.module.ai.vo.AutoLearnResultVO;
import com.company.daizhang.module.ai.vo.FeedbackStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * AI 识别反馈服务实现
 * <p>
 * 反馈闭环核心:
 * 1. saveFeedback: 后端基于 aiSuggested* 与 actual* 自动计算 accepted,不信任前端传入值
 * 2. autoLearnFromFeedback: 按 original_description 聚合,≥3 次且一致率 ≥80% 自动生成规则
 * 3. few-shot 注入由 GlmAiService 调用 getRecentFewShotExamples 完成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecognitionFeedbackServiceImpl
        extends ServiceImpl<AiRecognitionFeedbackMapper, AiRecognitionFeedback>
        implements RecognitionFeedbackService {

    /** 自动学习:扫描最近多少天的反馈 */
    private static final int AUTO_LEARN_LOOKBACK_DAYS = 30;
    /** 自动学习:同一描述最少出现次数阈值 */
    private static final int AUTO_LEARN_MIN_OCCURRENCE = 3;
    /** 自动学习:借贷科目一致率阈值(0-1) */
    private static final double AUTO_LEARN_MIN_CONSISTENCY = 0.80;
    /** 自动学习生成的规则优先级(低于手动创建的 100) */
    private static final int AUTO_LEARN_RULE_PRIORITY = 200;
    /** 自动学习:keyword 最大长度(从 description 截取) */
    private static final int AUTO_LEARN_KEYWORD_MAX_LEN = 20;
    /** few-shot 示例默认拉取条数 */
    private static final int FEW_SHOT_DEFAULT_LIMIT = 5;

    private final AccountSetAccessService accountSetAccessService;
    private final AccountingRuleService accountingRuleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFeedback(RecognitionFeedbackRequest request) {
        Long accountSetId = request.getAccountSetId();
        if (accountSetId == null || accountSetId == 0L) {
            // 反馈必须归属具体账套,不允许全局反馈
            throw new BusinessException(ErrorCode.PARAM_ERROR, "反馈必须指定具体账套");
        }
        accountSetAccessService.checkAccess(accountSetId);

        AiRecognitionFeedback feedback = new AiRecognitionFeedback();
        feedback.setAccountSetId(accountSetId);
        feedback.setOriginalDescription(request.getOriginalDescription());
        feedback.setAiSuggestedDebitCode(request.getAiSuggestedDebitCode());
        feedback.setAiSuggestedCreditCode(request.getAiSuggestedCreditCode());
        feedback.setActualDebitCode(request.getActualDebitCode());
        feedback.setActualCreditCode(request.getActualCreditCode());
        feedback.setActualSummary(request.getActualSummary());
        // 后端基于实际 vs 建议自动计算 accepted,不信任前端传入值
        feedback.setAccepted(computeAccepted(request));
        this.save(feedback);
        log.info("AI反馈保存成功,id={},accountSetId={},accepted={}",
                feedback.getId(), accountSetId, feedback.getAccepted());
    }

    @Override
    public List<AiRecognitionFeedback> getRecentFewShotExamples(Long accountSetId, int limit) {
        if (accountSetId == null) {
            return new ArrayList<>();
        }
        int safeLimit = limit <= 0 ? FEW_SHOT_DEFAULT_LIMIT : limit;
        // 优先取完全采纳(accepted=2)的反馈作为正面 few-shot
        // 同一描述只取一条,避免重复示例
        LambdaQueryWrapper<AiRecognitionFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiRecognitionFeedback::getAccountSetId, accountSetId)
                .eq(AiRecognitionFeedback::getAccepted, 2)
                .orderByDesc(AiRecognitionFeedback::getCreateTime)
                .last("LIMIT " + (safeLimit * 2)); // 多取一些用于去重
        List<AiRecognitionFeedback> all = this.list(wrapper);
        // 按 originalDescription 去重,保留最新的一条
        return all.stream()
                .collect(Collectors.groupingBy(AiRecognitionFeedback::getOriginalDescription))
                .values()
                .stream()
                .map(list -> list.stream()
                        .max(Comparator.comparing(AiRecognitionFeedback::getCreateTime))
                        .orElse(null))
                .filter(Objects::nonNull)
                .limit(safeLimit)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AutoLearnResultVO autoLearnFromFeedback() {
        LocalDateTime startTime = LocalDate.now().minusDays(AUTO_LEARN_LOOKBACK_DAYS).atStartOfDay();
        // 扫描最近 30 天的所有反馈(不区分账套,逐账套聚合)
        List<AiRecognitionFeedback> feedbacks = this.list(new LambdaQueryWrapper<AiRecognitionFeedback>()
                .ge(AiRecognitionFeedback::getCreateTime, startTime));
        AutoLearnResultVO result = new AutoLearnResultVO();
        result.setScannedFeedbackCount((long) feedbacks.size());
        if (feedbacks.isEmpty()) {
            result.setCandidateCount(0);
            result.setGeneratedCount(0);
            result.setSkippedDuplicateCount(0);
            return result;
        }

        // 按 accountSetId + originalDescription 聚合
        Map<String, List<AiRecognitionFeedback>> grouped = feedbacks.stream()
                .collect(Collectors.groupingBy(this::groupKey));
        int candidateCount = 0;
        int generatedCount = 0;
        int skippedCount = 0;
        for (Map.Entry<String, List<AiRecognitionFeedback>> entry : grouped.entrySet()) {
            List<AiRecognitionFeedback> group = entry.getValue();
            if (group.size() < AUTO_LEARN_MIN_OCCURRENCE) {
                continue;
            }
            candidateCount++;
            AiRecognitionFeedback first = group.get(0);
            Long accountSetId = first.getAccountSetId();
            String description = first.getOriginalDescription();
            // 统计 actual_debit_code/credit_code 一致率
            String dominantDebit = dominantValue(group, AiRecognitionFeedback::getActualDebitCode);
            String dominantCredit = dominantValue(group, AiRecognitionFeedback::getActualCreditCode);
            if (dominantDebit == null || dominantCredit == null) {
                continue;
            }
            long debitMatch = group.stream()
                    .filter(f -> dominantDebit.equals(f.getActualDebitCode())).count();
            long creditMatch = group.stream()
                    .filter(f -> dominantCredit.equals(f.getActualCreditCode())).count();
            double consistency = ((double) debitMatch / group.size() + (double) creditMatch / group.size()) / 2.0;
            if (consistency < AUTO_LEARN_MIN_CONSISTENCY) {
                continue;
            }
            // 生成 keyword:取 description 最长公共子串,否则取前 20 字符
            String keyword = extractKeyword(description);
            if (StrUtil.isBlank(keyword)) {
                continue;
            }
            // 去重:keyword + accountSetId 已存在则跳过
            if (accountingRuleService.existsByKeywordAndAccountSetId(accountSetId, keyword)) {
                skippedCount++;
                continue;
            }
            AiAccountingRule rule = new AiAccountingRule();
            rule.setAccountSetId(accountSetId);
            rule.setKeyword(keyword);
            rule.setDebitSubjectCode(dominantDebit);
            rule.setDebitSubjectName(findSubjectName(group, dominantDebit,
                    AiRecognitionFeedback::getActualDebitCode));
            rule.setCreditSubjectCode(dominantCredit);
            rule.setCreditSubjectName(findSubjectName(group, dominantCredit,
                    AiRecognitionFeedback::getActualCreditCode));
            rule.setVoucherSummary(first.getActualSummary());
            rule.setPriority(AUTO_LEARN_RULE_PRIORITY);
            rule.setHitCount(0);
            rule.setEnabled(1);
            try {
                accountingRuleService.save(rule);
                generatedCount++;
                log.info("自动学习生成规则:accountSetId={},keyword={},debit={},credit={}",
                        accountSetId, keyword, dominantDebit, dominantCredit);
            } catch (Exception e) {
                // 单条生成失败不影响其他规则
                log.warn("自动学习生成规则失败:accountSetId={},keyword={}", accountSetId, keyword, e);
            }
        }
        result.setCandidateCount(candidateCount);
        result.setGeneratedCount(generatedCount);
        result.setSkippedDuplicateCount(skippedCount);
        log.info("自动学习完成:扫描{}条反馈,候选{}组,生成{}条规则,跳过重复{}条",
                result.getScannedFeedbackCount(), candidateCount, generatedCount, skippedCount);
        return result;
    }

    @Override
    public FeedbackStatsVO getFeedbackStats(Long accountSetId, LocalDate startDate, LocalDate endDate) {
        if (accountSetId == null || accountSetId == 0L) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "必须指定具体账套");
        }
        accountSetAccessService.checkAccess(accountSetId);
        // 默认统计最近 30 天
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        LambdaQueryWrapper<AiRecognitionFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiRecognitionFeedback::getAccountSetId, accountSetId)
                .ge(AiRecognitionFeedback::getCreateTime, start.atStartOfDay())
                .le(AiRecognitionFeedback::getCreateTime, end.plusDays(1).atStartOfDay());
        List<AiRecognitionFeedback> feedbacks = this.list(wrapper);

        FeedbackStatsVO vo = new FeedbackStatsVO();
        vo.setAccountSetId(accountSetId);
        long total = feedbacks.size();
        long fully = feedbacks.stream().filter(f -> Integer.valueOf(2).equals(f.getAccepted())).count();
        long partial = feedbacks.stream().filter(f -> Integer.valueOf(1).equals(f.getAccepted())).count();
        long rejected = feedbacks.stream().filter(f -> Integer.valueOf(0).equals(f.getAccepted())).count();
        vo.setTotalCount(total);
        vo.setFullyAccepted(fully);
        vo.setPartiallyAccepted(partial);
        vo.setRejected(rejected);
        // 综合采纳率 = (完全采纳 + 部分采纳 * 0.5) / 总数
        if (total > 0) {
            double rate = (fully + partial * 0.5) / total;
            vo.setAcceptanceRate(Math.round(rate * 10000) / 10000.0);
        } else {
            vo.setAcceptanceRate(0.0);
        }
        return vo;
    }

    /**
     * 定时自动学习:每天凌晨 3 点执行,扫描最近 30 天反馈自动生成规则
     * <p>
     * 触发方式:
     * - 自动:每天 03:00(cron 表达式 "0 0 3 * * ?")
     * - 手动:ADMIN 调用 POST /api/ai/accounting-rules/auto-learn
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledAutoLearn() {
        try {
            log.info("定时自动学习任务启动");
            AutoLearnResultVO result = autoLearnFromFeedback();
            log.info("定时自动学习任务完成:扫描{}条,生成{}条规则",
                    result.getScannedFeedbackCount(), result.getGeneratedCount());
        } catch (Exception e) {
            // 定时任务异常不应中断调度,仅记录日志
            log.error("定时自动学习任务异常", e);
        }
    }

    /**
     * 计算采纳状态:0-未采纳 1-部分采纳 2-完全采纳
     * 不信任前端传入的 accepted,基于 aiSuggested* 与 actual* 自动计算
     */
    private Integer computeAccepted(RecognitionFeedbackRequest request) {
        String aiDebit = request.getAiSuggestedDebitCode();
        String aiCredit = request.getAiSuggestedCreditCode();
        // AI 建议缺失时无法采纳,记为 0
        if (StrUtil.isBlank(aiDebit) && StrUtil.isBlank(aiCredit)) {
            return 0;
        }
        boolean debitMatch = StrUtil.isNotBlank(aiDebit) && aiDebit.equals(request.getActualDebitCode());
        boolean creditMatch = StrUtil.isNotBlank(aiCredit) && aiCredit.equals(request.getActualCreditCode());
        if (debitMatch && creditMatch) {
            return 2;
        }
        if (debitMatch || creditMatch) {
            return 1;
        }
        return 0;
    }

    /**
     * 聚合 key:accountSetId + originalDescription
     */
    private String groupKey(AiRecognitionFeedback f) {
        return f.getAccountSetId() + "::" + (f.getOriginalDescription() == null ? "" : f.getOriginalDescription());
    }

    /**
     * 取列表中某字段出现频次最高的值(众数)
     */
    private String dominantValue(List<AiRecognitionFeedback> group,
                                 java.util.function.Function<AiRecognitionFeedback, String> extractor) {
        Map<String, Long> counts = new HashMap<>();
        for (AiRecognitionFeedback f : group) {
            String v = extractor.apply(f);
            if (StrUtil.isNotBlank(v)) {
                counts.merge(v, 1L, Long::sum);
            }
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * 在反馈组中查找指定借/贷科目编码对应的科目名称
     * 注:反馈表中没有完整的科目名称,这里返回 null,由 GlmAiService 调用方在规则匹配时
     * 用账套科目体系补全名称。本方法保留扩展空间,后续可在反馈表中冗余科目名称字段。
     */
    private String findSubjectName(List<AiRecognitionFeedback> group, String code,
                                   java.util.function.Function<AiRecognitionFeedback, String> extractor) {
        // 当前反馈表未存储科目名称,返回 null 由调用方补全
        return null;
    }

    /**
     * 从描述中提取关键词:
     * 1. 单条描述:取前 20 字符
     * 2. 多条描述(聚合场景):取最长公共子串,长度 ≥2 才采用,否则回退到首条前 20 字符
     */
    private String extractKeyword(String description) {
        if (StrUtil.isBlank(description)) {
            return null;
        }
        String trimmed = description.trim();
        if (trimmed.length() <= AUTO_LEARN_KEYWORD_MAX_LEN) {
            return trimmed;
        }
        return trimmed.substring(0, AUTO_LEARN_KEYWORD_MAX_LEN);
    }
}
