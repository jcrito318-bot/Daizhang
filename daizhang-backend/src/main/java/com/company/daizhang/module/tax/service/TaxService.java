package com.company.daizhang.module.tax.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.tax.dto.*;
import com.company.daizhang.module.tax.entity.TaxCalculation;
import com.company.daizhang.module.tax.entity.TaxDeclaration;
import com.company.daizhang.module.tax.vo.TaxCalculationVO;
import com.company.daizhang.module.tax.vo.TaxDeclarationVO;

import java.math.BigDecimal;

/**
 * 税务服务接口
 */
public interface TaxService extends IService<TaxDeclaration> {

    /**
     * 分页查询税务申报
     */
    PageResult<TaxDeclarationVO> pageDeclarations(TaxDeclarationQueryRequest request);

    /**
     * 根据ID查询税务申报
     */
    TaxDeclarationVO getDeclarationById(Long id);

    /**
     * 创建税务申报
     */
    void createDeclaration(TaxDeclarationCreateRequest request);

    /**
     * 更新税务申报
     */
    void updateDeclaration(Long id, TaxDeclarationUpdateRequest request);

    /**
     * 删除税务申报
     */
    void deleteDeclaration(Long id);

    /**
     * 申报税务
     */
    void declare(Long id);

    /**
     * 缴纳税款
     */
    void pay(Long id);

    /**
     * 分页查询税务计算
     */
    PageResult<TaxCalculationVO> pageCalculations(TaxCalculationQueryRequest request);

    /**
     * 根据ID查询税务计算
     */
    TaxCalculationVO getCalculationById(Long id);

    /**
     * 创建税务计算
     */
    void createCalculation(TaxCalculationCreateRequest request);

    /**
     * 更新税务计算
     */
    void updateCalculation(Long id, TaxCalculationUpdateRequest request);

    /**
     * 删除税务计算
     */
    void deleteCalculation(Long id);

    /**
     * 计算税额
     */
    BigDecimal calculateTax(Long accountSetId, Integer year, Integer month, String taxType);
}
