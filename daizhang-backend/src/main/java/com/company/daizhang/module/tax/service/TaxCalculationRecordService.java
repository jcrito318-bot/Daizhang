package com.company.daizhang.module.tax.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.tax.dto.TaxCalculationCreateRequest;
import com.company.daizhang.module.tax.dto.TaxCalculationQueryRequest;
import com.company.daizhang.module.tax.dto.TaxCalculationUpdateRequest;
import com.company.daizhang.module.tax.vo.TaxCalculationResultVO;
import com.company.daizhang.module.tax.vo.TaxCalculationVO;

import java.util.List;

/**
 * 税务计算记录服务接口（管理tax_calculation表）
 */
public interface TaxCalculationRecordService {

    /**
     * 分页查询税务计算记录
     */
    PageResult<TaxCalculationVO> pageCalculations(TaxCalculationQueryRequest request);

    /**
     * 根据ID查询税务计算记录
     */
    TaxCalculationVO getCalculationById(Long id);

    /**
     * 创建税务计算记录
     */
    void createCalculation(TaxCalculationCreateRequest request);

    /**
     * 更新税务计算记录
     */
    void updateCalculation(Long id, TaxCalculationUpdateRequest request);

    /**
     * 删除税务计算记录
     */
    void deleteCalculation(Long id);

    /**
     * 触发税额自动计算并持久化结果
     *
     * @param accountSetId 账套ID
     * @param year         年度
     * @param month        月份
     * @return 各税种计算结果
     */
    List<TaxCalculationResultVO> calculateTax(Long accountSetId, Integer year, Integer month);
}
