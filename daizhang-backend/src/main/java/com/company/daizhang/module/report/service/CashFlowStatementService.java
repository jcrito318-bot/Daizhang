package com.company.daizhang.module.report.service;

import com.company.daizhang.module.report.vo.CashFlowStatementVO;

/**
 * 现金流量表服务（直接法）
 * <p>
 * 依据《企业会计准则第31号—现金流量表》编制，通过分析已过账凭证中
 * 现金类科目的借贷发生额，按对方科目归类到23项标准现金流量项目。
 */
public interface CashFlowStatementService {

    /**
     * 生成现金流量表（直接法）
     *
     * @param accountSetId 账套ID
     * @param year         年度
     * @param month        月份
     * @return 现金流量表数据
     */
    CashFlowStatementVO generateCashFlowStatement(Long accountSetId, Integer year, Integer month);
}
