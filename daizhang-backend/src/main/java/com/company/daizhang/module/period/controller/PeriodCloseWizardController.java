package com.company.daizhang.module.period.controller;

import com.company.daizhang.common.annotation.OperationLog;
import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.period.dto.PeriodCloseWizardRequest;
import com.company.daizhang.module.period.service.PeriodCloseWizardService;
import com.company.daizhang.module.period.vo.PeriodCloseWizardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 期末结账向导控制器
 * <p>
 * 一键完成"结转损益 + 结账 + 下月开启"的月末结账流程。
 * 需 OWNER 权限:结账为高危状态变更操作,涉及期间关闭与结转损益凭证生成。
 */
@Tag(name = "期末处理")
@RestController
@RequestMapping("/period")
@RequiredArgsConstructor
public class PeriodCloseWizardController {

    private final PeriodCloseWizardService periodCloseWizardService;

    /**
     * 执行期末结账向导
     * <p>
     * 在单个事务中按序执行 7 个步骤(数据完整性检查/期末调汇/结转损益/结转成本/
     * 计提折旧/结账/下月开启),任一必选步骤失败则整体回滚。
     *
     * @param accountSetId 账套 ID
     * @param year         年度
     * @param month        月份
     * @param request      向导请求参数(skipOptionalSteps / autoCloseIfNoErrors,均可省略默认 true)
     * @return 向导执行结果,含各步骤状态与整体汇总
     */
    @Operation(summary = "期末结账向导")
    @PostMapping("/close-wizard")
    @OperationLog("期末结账向导")
    @RequireAccountSetAccess(RequireAccountSetAccess.AccessLevel.OWNER)
    public Result<PeriodCloseWizardVO> closeWizard(@RequestParam Long accountSetId,
                                                    @RequestParam int year,
                                                    @RequestParam int month,
                                                    @RequestBody(required = false) PeriodCloseWizardRequest request) {
        PeriodCloseWizardVO result = periodCloseWizardService.executeCloseWizard(
                accountSetId, year, month, request);
        return Result.success(result);
    }
}
