package com.company.daizhang.module.bank.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.bank.dto.ApplySuggestionsRequest;
import com.company.daizhang.module.bank.dto.LearnPatternsRequest;
import com.company.daizhang.module.bank.service.SmartReconciliationService;
import com.company.daizhang.module.bank.vo.MatchHistoryPatternVO;
import com.company.daizhang.module.bank.vo.MatchResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智能对账增强控制器
 * <p>
 * 提供基于评分模型的智能匹配、批量应用建议、历史模式查询与学习等接口,
 * 与原有 /bank/match/auto 自动匹配接口并存,不替换原 autoMatch 逻辑。
 * <p>
 * 实际访问路径前缀为 /api/bank(由 server.servlet.context-path=/api 配置)。
 */
@Tag(name = "智能对账增强")
@RestController
@RequestMapping("/bank")
@RequiredArgsConstructor
public class SmartReconciliationController {

    private final SmartReconciliationService smartReconciliationService;

    @Operation(summary = "智能匹配 - 对指定账套+银行账号+年月的未匹配流水与候选凭证进行评分,返回匹配建议列表")
    @PostMapping("/smart-match")
    @RequireAccountSetAccess
    public Result<MatchResultVO> smartMatch(
            @RequestParam Long accountSetId,
            @RequestParam String bankAccount,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        MatchResultVO result = smartReconciliationService.smartMatch(accountSetId, bankAccount, year, month);
        return Result.success(result);
    }

    @Operation(summary = "批量应用建议 - 用户确认接受的建议后回写匹配关系并自动学习历史模式")
    @PostMapping("/apply-suggestions")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Integer> applySuggestions(@Valid @RequestBody ApplySuggestionsRequest request) {
        Integer count = smartReconciliationService.applySuggestions(request);
        return Result.success("成功应用 " + count + " 条匹配建议", count);
    }

    @Operation(summary = "查询历史匹配模式")
    @GetMapping("/match-history/patterns")
    @RequireAccountSetAccess
    public Result<List<MatchHistoryPatternVO>> getMatchPatterns(@RequestParam Long accountSetId) {
        List<MatchHistoryPatternVO> patterns = smartReconciliationService.getMatchPatterns(accountSetId);
        return Result.success(patterns);
    }

    @Operation(summary = "手动触发历史模式学习 - 扫描已匹配流水聚合金额范围与科目编码")
    @PostMapping("/match-history/learn")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Integer> learnPatterns(@Valid @RequestBody LearnPatternsRequest request) {
        Integer count = smartReconciliationService.learnPatterns(request.getAccountSetId());
        return Result.success("学习完成,共处理 " + count + " 条历史匹配", count);
    }
}
