package com.company.daizhang.module.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.ai.dto.AccountingRuleRequest;
import com.company.daizhang.module.ai.entity.AiAccountingRule;

import java.util.List;

/**
 * AI 记账规则服务接口
 * <p>
 * 规则库优先于 AI 调用:当业务描述命中关键词时直接返回规则,不调用 GLM。
 * IDOR 治理:账套级规则(accountSetId != 0)的 CRUD 须校验当前用户对该账套的访问权;
 * 全局规则(accountSetId = 0)对所有用户只读,仅 ADMIN 可写。
 */
public interface AccountingRuleService extends IService<AiAccountingRule> {

    /**
     * 关键词模糊匹配规则,优先匹配账套级规则,回退到全局规则(accountSetId=0)。
     * <p>
     * 匹配策略:
     * 1. 先查账套级规则(accountSetId = 入参),按 priority 升序、hit_count 降序取首条
     * 2. 未命中再查全局规则(accountSetId = 0)
     * 3. 命中后异步自增 hit_count(不影响主流程)
     *
     * @param accountSetId 账套ID
     * @param description  业务描述
     * @return 命中的规则,未命中返回 null
     */
    AiAccountingRule matchRule(Long accountSetId, String description);

    /**
     * 命中计数自增(用于排序优化,数字越大越优先匹配)
     *
     * @param ruleId 规则ID
     */
    void incrementHitCount(Long ruleId);

    /**
     * 保存规则(账套级需校验访问权,全局需 ADMIN)
     */
    void saveRule(AccountingRuleRequest request);

    /**
     * 更新规则(账套级需校验访问权,全局需 ADMIN)
     */
    void updateRule(Long id, AccountingRuleRequest request);

    /**
     * 删除规则(账套级需校验访问权,全局需 ADMIN)
     */
    void deleteRule(Long id);

    /**
     * 查询规则列表(按账套ID;admin 可传 accountSetId=0 查全局规则)
     */
    List<AiAccountingRule> listRules(Long accountSetId, String keyword, Integer enabled);

    /**
     * 分页查询规则
     */
    PageResult<AiAccountingRule> pageRules(Long accountSetId, String keyword, Integer enabled,
                                           int pageNum, int pageSize);

    /**
     * 检查 keyword + accountSetId 是否已存在(用于自动学习去重)
     *
     * @return true 表示已存在
     */
    boolean existsByKeywordAndAccountSetId(Long accountSetId, String keyword);
}
