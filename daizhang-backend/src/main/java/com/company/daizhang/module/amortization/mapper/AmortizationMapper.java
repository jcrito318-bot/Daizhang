package com.company.daizhang.module.amortization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.daizhang.module.amortization.entity.Amortization;
import org.apache.ibatis.annotations.Mapper;

/**
 * 长期待摊费用Mapper
 */
@Mapper
public interface AmortizationMapper extends BaseMapper<Amortization> {
}
