package com.company.daizhang.module.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.daizhang.module.customer.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收款记录Mapper接口
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}
