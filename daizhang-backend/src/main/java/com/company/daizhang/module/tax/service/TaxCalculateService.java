package com.company.daizhang.module.tax.service;

import com.company.daizhang.module.tax.vo.TaxCalculationResultVO;

import java.util.List;

/**
 * 税种自动计算服务接口
 */
public interface TaxCalculateService {

    /**
     * 计算所有税种
     */
    List<TaxCalculationResultVO> calculateAllTaxes(Long accountSetId, Integer year, Integer month);

    /**
     * 计算增值税
     */
    TaxCalculationResultVO calculateVAT(Long accountSetId, Integer year, Integer month);

    /**
     * 计算附加税（城建税7%/教育附加3%/地方教育附加2%）
     */
    TaxCalculationResultVO calculateSurchargeTax(Long accountSetId, Integer year, Integer month);

    /**
     * 计算企业所得税（季度预缴）
     */
    TaxCalculationResultVO calculateCorporateIncomeTax(Long accountSetId, Integer year, Integer month);

    /**
     * 计算个人所得税（工资薪金）
     */
    TaxCalculationResultVO calculatePersonalIncomeTax(Long accountSetId, Integer year, Integer month);
}
