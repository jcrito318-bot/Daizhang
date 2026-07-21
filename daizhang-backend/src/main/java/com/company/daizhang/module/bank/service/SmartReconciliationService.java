package com.company.daizhang.module.bank.service;

import com.company.daizhang.module.bank.dto.ApplySuggestionsRequest;
import com.company.daizhang.module.bank.vo.MatchHistoryPatternVO;
import com.company.daizhang.module.bank.vo.MatchResultVO;

import java.util.List;

/**
 * 智能对账增强服务
 * <p>
 * 在现有 {@link BankService#autoMatch} 基础上做能力增强,不替换原 autoMatch 方法。
 * <p>
 * 提供基于评分模型的模糊匹配(金额/日期/摘要/对方/历史模式多维度加权)、
 * 历史模式学习与查询、用户确认后的批量应用建议。
 */
public interface SmartReconciliationService {

    /**
     * 自动智能匹配 — 对指定账套+银行账号+年月的未匹配流水与候选凭证进行评分,
     * 返回匹配建议列表(含分数、原因、匹配类型)。
     *
     * @param accountSetId 账套ID
     * @param bankAccount  银行账号
     * @param year         年度
     * @param month        月份
     * @return 匹配结果(已匹配建议按分数倒序,未匹配流水/凭证列表,统计计数)
     */
    MatchResultVO smartMatch(Long accountSetId, String bankAccount, Integer year, Integer month);

    /**
     * 批量应用建议 — 用户在前端确认接受的建议后,回写匹配关系并自动学习历史模式。
     * <p>
     * 应用前再次校验:流水未匹配、凭证已过账、归属同一账套。
     *
     * @param request 已接受建议列表
     * @return 实际应用成功的数量
     */
    Integer applySuggestions(ApplySuggestionsRequest request);

    /**
     * 查询某账套下的历史匹配模式
     *
     * @param accountSetId 账套ID
     * @return 历史模式列表(按匹配次数倒序)
     */
    List<MatchHistoryPatternVO> getMatchPatterns(Long accountSetId);

    /**
     * 手动触发历史模式学习 — 扫描该账套所有已匹配银行流水,
     * 按 counterparty 聚合金额范围与科目编码,upsert 到 bank_match_history 表。
     *
     * @param accountSetId 账套ID
     * @return 学习(更新或插入)的模式条数
     */
    Integer learnPatterns(Long accountSetId);

    /**
     * 单条匹配后学习 — 以本次流水金额扩展历史金额范围,并累加匹配次数。
     * <p>
     * 供 {@link BankService#manualMatch} 在匹配完成后调用,
     * 实现"每次手动匹配后自动更新或插入历史模式"。
     *
     * @param transactionId 银行流水ID
     * @param voucherId     凭证ID
     */
    void learnFromMatch(Long transactionId, Long voucherId);
}
