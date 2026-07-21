package com.company.daizhang.module.ai.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.ai.dto.AccountingRuleRequest;
import com.company.daizhang.module.ai.dto.AccountingSuggestionRequest;
import com.company.daizhang.module.ai.dto.AccountingSuggestionResponse;
import com.company.daizhang.module.ai.dto.RecognitionFeedbackRequest;
import com.company.daizhang.module.ai.entity.AiAccountingRule;
import com.company.daizhang.module.ai.service.AccountingRuleService;
import com.company.daizhang.module.ai.service.GlmAiService;
import com.company.daizhang.module.ai.service.RecognitionFeedbackService;
import com.company.daizhang.module.ai.vo.AutoLearnResultVO;
import com.company.daizhang.module.ai.vo.FeedbackStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * AI 智能凭证识别控制器
 * <p>
 * 三大能力:
 * 1. 智能建议(规则优先 + AI 兜底):POST /api/ai/accounting-suggest
 * 2. 反馈闭环(用户反馈 + 自动学习):POST /api/ai/accounting-feedback, POST /api/ai/accounting-rules/auto-learn
 * 3. 规则 CRUD:GET/POST/PUT/DELETE /api/ai/accounting-rules
 * <p>
 * IDOR 治理:
 * - accounting-suggest/accounting-feedback:通过 @RequireAccountSetAccess 自动校验账套访问权
 * - accounting-rules 列表/分页:accountSetId=0(全局)对所有用户只读;accountSetId!=0 由 Service 校验访问权
 * - accounting-rules 写操作:accountSetId=0 仅 ADMIN;accountSetId!=0 由 Service 校验访问权
 * - auto-learn:@PreAuthorize("hasRole('ADMIN')") 仅管理员可手动触发
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI智能凭证识别", description = "科目映射规则库 + few-shot prompt + 用户反馈闭环")
public class AccountingSuggestionController {

    private final GlmAiService glmAiService;
    private final AccountingRuleService accountingRuleService;
    private final RecognitionFeedbackService recognitionFeedbackService;

    /**
     * 智能记账建议(先规则后AI)
     * <p>
     * 流程:规则库匹配 → 命中直接返回 → 未命中调用 GLM(注入科目体系 + few-shot)
     */
    @PostMapping("/accounting-suggest")
    @Operation(summary = "智能记账建议", description = "规则库优先匹配,未命中调用GLM增强版(注入科目体系+few-shot)")
    @RequireAccountSetAccess
    public Result<AccountingSuggestionResponse> suggest(@Valid @RequestBody AccountingSuggestionRequest request) {
        // 限制输入长度,防止超长文本消耗 GLM token 配额/费用
        if (request.getDescription().length() > 500) {
            return Result.error(400, "业务描述过长(>500字),请精简后重试");
        }
        AccountingSuggestionResponse response = glmAiService.suggestAccountingWithContext(
                request.getDescription(),
                request.getAmount(),
                request.getAccountSetId(),
                null);
        return Result.success(response);
    }

    /**
     * 提交用户反馈(用户实际选择 vs AI建议)
     */
    @PostMapping("/accounting-feedback")
    @Operation(summary = "提交用户反馈", description = "用户实际选择借贷科目后反馈,用于few-shot注入与自动学习")
    @RequireAccountSetAccess
    public Result<Void> submitFeedback(@Valid @RequestBody RecognitionFeedbackRequest request) {
        recognitionFeedbackService.saveFeedback(request);
        return Result.success();
    }

    /**
     * 查询规则列表
     * <p>
     * accountSetId=0:查询全局规则(对所有用户只读)
     * accountSetId!=0:查询指定账套规则(需访问权)
     */
    @GetMapping("/accounting-rules")
    @Operation(summary = "查询规则列表", description = "支持按账套ID/关键词/启用状态过滤;accountSetId=0查全局规则")
    public Result<List<AiAccountingRule>> listRules(
            @RequestParam(required = false) Long accountSetId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer enabled) {
        List<AiAccountingRule> rules = accountingRuleService.listRules(accountSetId, keyword, enabled);
        return Result.success(rules);
    }

    /**
     * 分页查询规则列表
     */
    @GetMapping("/accounting-rules/page")
    @Operation(summary = "分页查询规则列表", description = "支持按账套ID/关键词/启用状态过滤")
    public Result<PageResult<AiAccountingRule>> pageRules(
            @RequestParam(required = false) Long accountSetId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<AiAccountingRule> page = accountingRuleService.pageRules(
                accountSetId, keyword, enabled, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 新增规则
     * <p>
     * accountSetId=0:仅 ADMIN 可创建全局规则(Service 层校验)
     * accountSetId!=0:需对该账套有访问权(Service 层校验)
     */
    @PostMapping("/accounting-rules")
    @Operation(summary = "新增规则", description = "全局规则(accountSetId=0)仅ADMIN可创建;账套级规则需访问权")
    public Result<Void> createRule(@Valid @RequestBody AccountingRuleRequest request) {
        accountingRuleService.saveRule(request);
        return Result.success();
    }

    /**
     * 更新规则
     */
    @PutMapping("/accounting-rules/{id}")
    @Operation(summary = "更新规则", description = "全局规则仅ADMIN可更新;账套级规则需访问权")
    public Result<Void> updateRule(@PathVariable Long id, @Valid @RequestBody AccountingRuleRequest request) {
        accountingRuleService.updateRule(id, request);
        return Result.success();
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/accounting-rules/{id}")
    @Operation(summary = "删除规则", description = "全局规则仅ADMIN可删除;账套级规则需访问权")
    public Result<Void> deleteRule(@PathVariable Long id) {
        accountingRuleService.deleteRule(id);
        return Result.success();
    }

    /**
     * 触发自动学习(从反馈生成规则,ADMIN only)
     * <p>
     * 扫描最近 30 天反馈,按 description 聚合,≥3 次且一致率 ≥80% 自动生成规则
     */
    @PostMapping("/accounting-rules/auto-learn")
    @Operation(summary = "触发自动学习", description = "从用户反馈自动生成规则(ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<AutoLearnResultVO> autoLearn() {
        AutoLearnResultVO result = recognitionFeedbackService.autoLearnFromFeedback();
        return Result.success(result);
    }

    /**
     * 反馈统计(采纳率)
     * <p>
     * 默认统计最近 30 天,可通过 startDate/endDate 自定义区间
     */
    @GetMapping("/accounting-feedback/stats")
    @Operation(summary = "反馈统计", description = "统计指定账套在区间的AI建议采纳率")
    @RequireAccountSetAccess
    public Result<FeedbackStatsVO> feedbackStats(
            @RequestParam Long accountSetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        FeedbackStatsVO stats = recognitionFeedbackService.getFeedbackStats(accountSetId, startDate, endDate);
        return Result.success(stats);
    }
}
