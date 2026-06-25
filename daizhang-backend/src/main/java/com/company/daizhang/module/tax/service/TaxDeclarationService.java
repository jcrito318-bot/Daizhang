package com.company.daizhang.module.tax.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.tax.dto.TaxDeclarationCreateRequest;
import com.company.daizhang.module.tax.dto.TaxDeclarationQueryRequest;
import com.company.daizhang.module.tax.dto.TaxDeclarationUpdateRequest;
import com.company.daizhang.module.tax.vo.TaxDeclarationVO;

/**
 * 税务申报记录服务接口
 */
public interface TaxDeclarationService {

    /**
     * 分页查询税务申报记录
     */
    PageResult<TaxDeclarationVO> pageDeclarations(TaxDeclarationQueryRequest request);

    /**
     * 根据ID查询税务申报记录
     */
    TaxDeclarationVO getDeclarationById(Long id);

    /**
     * 创建税务申报记录
     */
    void createDeclaration(TaxDeclarationCreateRequest request);

    /**
     * 更新税务申报记录
     */
    void updateDeclaration(Long id, TaxDeclarationUpdateRequest request);

    /**
     * 删除税务申报记录
     */
    void deleteDeclaration(Long id);

    /**
     * 执行申报（状态置为已申报）
     */
    void declare(Long id);

    /**
     * 执行缴款（状态置为已缴纳）
     */
    void pay(Long id, java.math.BigDecimal actualAmount);
}
