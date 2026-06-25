package com.company.daizhang.module.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.daizhang.module.customer.entity.BillingRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户开票记录Mapper
 */
@Mapper
public interface BillingRecordMapper extends BaseMapper<BillingRecord> {
}
