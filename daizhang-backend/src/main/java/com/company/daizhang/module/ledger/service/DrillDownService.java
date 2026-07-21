package com.company.daizhang.module.ledger.service;

import com.company.daizhang.module.ledger.vo.DrillDownResultVO;

import java.math.BigDecimal;

/**
 * 报表钻取服务
 * <p>
 * 在财务报表(资负表、利润表、现金流量表)的金额单元格上双击后,
 * 根据科目范围 + 期间 + 金额 + 借贷方向反查已过账凭证明细,用于代账会计追溯原始凭证。
 */
public interface DrillDownService {

    /**
     * 报表钻取:按科目+期间+金额反查凭证
     *
     * @param accountSetId 账套ID
     * @param subjectCode  科目编码(支持前缀匹配,如 "1001" 命中 "1001" 及其下级 "100101" 等)
     * @param year         年度
     * @param month        月份(1-12)
     * @param amount       目标金额(正数)
     * @param direction    方向:debit(借方) 或 credit(贷方)
     * @param fuzzy        是否模糊匹配(±0.01 容差),默认 false 精确匹配
     * @return 命中的凭证分录列表
     */
    DrillDownResultVO drillDown(Long accountSetId, String subjectCode, Integer year, Integer month,
                                BigDecimal amount, String direction, Boolean fuzzy);
}
