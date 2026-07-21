package com.company.daizhang.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.ai.dto.AccountingRuleRequest;
import com.company.daizhang.module.ai.entity.AiAccountingRule;
import com.company.daizhang.module.ai.mapper.AiAccountingRuleMapper;
import com.company.daizhang.module.ai.service.AccountingRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 记账规则服务实现
 * <p>
 * IDOR 治理要点:
 * 1. 账套级规则(accountSetId != 0):CRUD 须校验当前用户对该账套的访问权
 * 2. 全局规则(accountSetId == 0):对所有用户只读,仅 ADMIN 可写
 * 3. matchRule 内部查询全局规则不需要权限校验(只读 SQL,且仅返回借贷科目编码/名称,不暴露其他账套数据)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountingRuleServiceImpl extends ServiceImpl<AiAccountingRuleMapper, AiAccountingRule>
        implements AccountingRuleService {

    /** 全局规则的 accountSetId 取值 */
    private static final Long GLOBAL_ACCOUNT_SET_ID = 0L;

    private final AccountSetAccessService accountSetAccessService;

    @Override
    public AiAccountingRule matchRule(Long accountSetId, String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        String desc = description.trim();

        // 1. 优先匹配账套级规则
        AiAccountingRule matched = doMatch(accountSetId, desc);
        // 2. 账套级未命中,回退到全局规则(accountSetId=0)
        if (matched == null && !GLOBAL_ACCOUNT_SET_ID.equals(accountSetId)) {
            matched = doMatch(GLOBAL_ACCOUNT_SET_ID, desc);
        }
        // 3. 命中后异步自增 hit_count(同步执行,SQL 简单且不影响主流程性能)
        if (matched != null) {
            try {
                incrementHitCount(matched.getId());
            } catch (Exception e) {
                // 命中计数失败不影响业务返回,仅记录日志
                log.warn("规则命中计数自增失败,ruleId={}", matched.getId(), e);
            }
        }
        return matched;
    }

    /**
     * 在指定 accountSetId 下做关键词模糊匹配
     * 排序: priority 升序(数字越小越优先) -> hit_count 降序(高频优先) -> id 升序(稳定排序)
     */
    private AiAccountingRule doMatch(Long accountSetId, String description) {
        LambdaQueryWrapper<AiAccountingRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAccountingRule::getAccountSetId, accountSetId)
                .eq(AiAccountingRule::getEnabled, 1)
                .like(AiAccountingRule::getKeyword, description)
                .orderByAsc(AiAccountingRule::getPriority)
                .orderByDesc(AiAccountingRule::getHitCount)
                .orderByAsc(AiAccountingRule::getId)
                .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementHitCount(Long ruleId) {
        if (ruleId == null) {
            return;
        }
        // 使用 LambdaUpdateWrapper 拼接 hit_count = hit_count + 1,避免先查后写的并发问题
        LambdaUpdateWrapper<AiAccountingRule> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AiAccountingRule::getId, ruleId)
                .setSql("hit_count = hit_count + 1");
        this.update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRule(AccountingRuleRequest request) {
        Long accountSetId = request.getAccountSetId();
        checkWriteAccess(accountSetId);
        // 同账套下 keyword 唯一性校验(避免重复规则)
        if (existsByKeywordAndAccountSetId(accountSetId, request.getKeyword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "关键词[" + request.getKeyword() + "]在该账套下已存在规则");
        }
        AiAccountingRule rule = new AiAccountingRule();
        rule.setAccountSetId(accountSetId);
        rule.setKeyword(request.getKeyword());
        rule.setDebitSubjectCode(request.getDebitSubjectCode());
        rule.setDebitSubjectName(request.getDebitSubjectName());
        rule.setCreditSubjectCode(request.getCreditSubjectCode());
        rule.setCreditSubjectName(request.getCreditSubjectName());
        rule.setVoucherSummary(request.getVoucherSummary());
        rule.setPriority(request.getPriority() == null ? 100 : request.getPriority());
        rule.setHitCount(0);
        rule.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
        this.save(rule);
        log.info("AI记账规则创建成功,id={},accountSetId={},keyword={}",
                rule.getId(), accountSetId, rule.getKeyword());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRule(Long id, AccountingRuleRequest request) {
        AiAccountingRule existing = this.getById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "规则不存在");
        }
        // 同时校验:原规则的 accountSetId 与新请求的 accountSetId 都必须有权限
        checkWriteAccess(existing.getAccountSetId());
        checkWriteAccess(request.getAccountSetId());
        // keyword 变更时检查唯一性
        if (!existing.getKeyword().equals(request.getKeyword())
                || !existing.getAccountSetId().equals(request.getAccountSetId())) {
            if (existsByKeywordAndAccountSetId(request.getAccountSetId(), request.getKeyword())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                        "关键词[" + request.getKeyword() + "]在该账套下已存在规则");
            }
        }
        existing.setAccountSetId(request.getAccountSetId());
        existing.setKeyword(request.getKeyword());
        existing.setDebitSubjectCode(request.getDebitSubjectCode());
        existing.setDebitSubjectName(request.getDebitSubjectName());
        existing.setCreditSubjectCode(request.getCreditSubjectCode());
        existing.setCreditSubjectName(request.getCreditSubjectName());
        existing.setVoucherSummary(request.getVoucherSummary());
        if (request.getPriority() != null) {
            existing.setPriority(request.getPriority());
        }
        if (request.getEnabled() != null) {
            existing.setEnabled(request.getEnabled());
        }
        this.updateById(existing);
        log.info("AI记账规则更新成功,id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        AiAccountingRule existing = this.getById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "规则不存在");
        }
        checkWriteAccess(existing.getAccountSetId());
        this.removeById(id);
        log.info("AI记账规则删除成功,id={}", id);
    }

    @Override
    public List<AiAccountingRule> listRules(Long accountSetId, String keyword, Integer enabled) {
        // 查询权限:全局规则(accountSetId=0)对所有用户只读开放;账套级规则需访问权
        // accountSetId 为 null 时只返回全局规则,避免跨账套数据泄露
        Long queryAccountSetId = (accountSetId == null) ? GLOBAL_ACCOUNT_SET_ID : accountSetId;
        if (!GLOBAL_ACCOUNT_SET_ID.equals(queryAccountSetId)) {
            accountSetAccessService.checkAccess(queryAccountSetId);
        }
        LambdaQueryWrapper<AiAccountingRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAccountingRule::getAccountSetId, queryAccountSetId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(AiAccountingRule::getKeyword, keyword.trim());
        }
        if (enabled != null) {
            wrapper.eq(AiAccountingRule::getEnabled, enabled);
        }
        wrapper.orderByAsc(AiAccountingRule::getAccountSetId)
                .orderByAsc(AiAccountingRule::getPriority)
                .orderByDesc(AiAccountingRule::getHitCount);
        return this.list(wrapper);
    }

    @Override
    public PageResult<AiAccountingRule> pageRules(Long accountSetId, String keyword, Integer enabled,
                                                   int pageNum, int pageSize) {
        // 查询权限同 listRules;accountSetId 为 null 时只返回全局规则,避免跨账套数据泄露
        Long queryAccountSetId = (accountSetId == null) ? GLOBAL_ACCOUNT_SET_ID : accountSetId;
        if (!GLOBAL_ACCOUNT_SET_ID.equals(queryAccountSetId)) {
            accountSetAccessService.checkAccess(queryAccountSetId);
        }
        LambdaQueryWrapper<AiAccountingRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAccountingRule::getAccountSetId, queryAccountSetId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(AiAccountingRule::getKeyword, keyword.trim());
        }
        if (enabled != null) {
            wrapper.eq(AiAccountingRule::getEnabled, enabled);
        }
        wrapper.orderByAsc(AiAccountingRule::getAccountSetId)
                .orderByAsc(AiAccountingRule::getPriority)
                .orderByDesc(AiAccountingRule::getHitCount);
        Page<AiAccountingRule> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public boolean existsByKeywordAndAccountSetId(Long accountSetId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }
        long count = this.count(new LambdaQueryWrapper<AiAccountingRule>()
                .eq(AiAccountingRule::getAccountSetId, accountSetId)
                .eq(AiAccountingRule::getKeyword, keyword.trim()));
        return count > 0;
    }

    /**
     * 校验当前用户对指定账套的写权限
     * - 全局规则(accountSetId=0):仅 ADMIN 可写
     * - 账套级规则:需对该账套有访问权
     */
    private void checkWriteAccess(Long accountSetId) {
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }
        if (GLOBAL_ACCOUNT_SET_ID.equals(accountSetId)) {
            if (!isSuperAdmin()) {
                log.warn("IDOR越权拦截: 非管理员用户尝试操作全局AI规则");
                throw new BusinessException(ErrorCode.FORBIDDEN, "全局规则仅管理员可操作");
            }
            return;
        }
        accountSetAccessService.checkAccess(accountSetId);
    }

    /**
     * 判断当前登录用户是否为超级管理员(ROLE_ADMIN)
     * 与 AccountSetAccessServiceImpl 中判定逻辑保持一致
     */
    private boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }
}
