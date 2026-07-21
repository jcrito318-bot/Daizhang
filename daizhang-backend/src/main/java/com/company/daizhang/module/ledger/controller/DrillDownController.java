package com.company.daizhang.module.ledger.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.ledger.service.DrillDownService;
import com.company.daizhang.module.ledger.vo.DrillDownResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 报表钻取控制器
 * <p>
 * 提供"在报表金额上双击 → 反查相关凭证"的钻取查询能力。
 * 仅查询,不修改任何数据。
 */
@Slf4j
@Tag(name = "报表钻取")
@RestController
@RequestMapping("/ledger")
@RequiredArgsConstructor
public class DrillDownController {

    private final DrillDownService drillDownService;

    /**
     * 报表钻取:按"科目+期间+金额+方向"反查凭证
     * <p>
     * 典型调用:
     * GET /api/ledger/drill-down?accountSetId=1&subjectCode=1001&year=2025&month=6&amount=10000.00&direction=debit
     *
     * @param accountSetId 账套ID
     * @param subjectCode  科目编码(支持前缀匹配,如 "1001" 命中下级)
     * @param year         年度
     * @param month        月份
     * @param amount       目标金额
     * @param direction    方向:debit/credit
     * @param fuzzy        是否模糊匹配(±0.01 容差),默认 false
     */
    @Operation(summary = "报表钻取:按科目+期间+金额反查凭证")
    @GetMapping("/drill-down")
    @RequireAccountSetAccess
    public Result<DrillDownResultVO> drillDown(
            @Parameter(description = "账套ID", required = true) @RequestParam Long accountSetId,
            @Parameter(description = "科目编码(支持前缀匹配)", required = true) @RequestParam String subjectCode,
            @Parameter(description = "年度", required = true) @RequestParam Integer year,
            @Parameter(description = "月份(1-12)", required = true) @RequestParam Integer month,
            @Parameter(description = "目标金额", required = true) @RequestParam BigDecimal amount,
            @Parameter(description = "方向:debit/credit", required = true) @RequestParam String direction,
            @Parameter(description = "是否模糊匹配(±0.01 容差),默认 false") @RequestParam(required = false, defaultValue = "false") Boolean fuzzy) {
        DrillDownResultVO result = drillDownService.drillDown(accountSetId, subjectCode, year, month, amount, direction, fuzzy);
        return Result.success(result);
    }
}
