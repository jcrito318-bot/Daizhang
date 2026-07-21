package com.company.daizhang.module.period.service;

import com.company.daizhang.module.period.dto.PeriodCloseWizardRequest;
import com.company.daizhang.module.period.vo.PeriodCloseWizardVO;

/**
 * 期末结账向导编排服务
 * <p>
 * 一键完成"结转损益 + 结账 + 下月开启"的月末结账流程,替代手动多步操作。
 * <p>
 * 向导共 7 个步骤:
 * <ol>
 *   <li>数据完整性检查(未审核凭证/借贷不平凭证)</li>
 *   <li>期末调汇(可选,暂未实现,预留)</li>
 *   <li>结转损益(借:收入类科目→本年利润;贷:费用类科目→本年利润)</li>
 *   <li>结转成本(可选,工业账套,商业账套跳过)</li>
 *   <li>计提折旧(可选,依赖固定资产模块)</li>
 *   <li>结账(调用 PeriodService.closePeriod)</li>
 *   <li>下月开启(自动创建下月会计期间,状态为"开")</li>
 * </ol>
 * 整个流程在一个事务中执行,任一必选步骤失败则整体回滚。
 */
public interface PeriodCloseWizardService {

    /**
     * 执行期末结账向导
     *
     * @param accountSetId 账套 ID
     * @param year         年度
     * @param month        月份
     * @param request      向导请求参数(skipOptionalSteps / autoCloseIfNoErrors)
     * @return 向导执行结果,含各步骤状态与整体汇总
     */
    PeriodCloseWizardVO executeCloseWizard(Long accountSetId, int year, int month, PeriodCloseWizardRequest request);
}
