package com.company.daizhang.module.tax.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.tax.dto.TaxRiskWarningRequest;
import com.company.daizhang.module.tax.vo.TaxRiskWarningVO;

/**
 * 税务风险预警服务接口
 */
public interface TaxRiskWarningService {

    /**
     * 分页查询风险预警
     */
    PageResult<TaxRiskWarningVO> pageWarnings(Long accountSetId, Integer year, Integer month,
                                              Integer riskLevel, int pageNum, int pageSize);

    /**
     * 创建风险预警
     */
    void createWarning(TaxRiskWarningRequest request);

    /**
     * 处理预警
     */
    void handleWarning(Long id, String handleRemark);

    /**
     * 忽略预警
     */
    void ignoreWarning(Long id);

    /**
     * 扫描生成风险预警
     */
    void scanRiskWarnings(Long accountSetId, Integer year, Integer month);
}
