package com.company.daizhang.module.tax.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.daizhang.module.tax.entity.TaxDeclaration;
import org.apache.ibatis.annotations.Mapper;

/**
 * 税务申报Mapper
 */
@Mapper
public interface TaxDeclarationMapper extends BaseMapper<TaxDeclaration> {
}
