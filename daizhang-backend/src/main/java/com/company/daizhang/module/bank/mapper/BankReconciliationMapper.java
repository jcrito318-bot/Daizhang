package com.company.daizhang.module.bank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.daizhang.module.bank.entity.BankReconciliation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 银行对账结果Mapper
 */
@Mapper
public interface BankReconciliationMapper extends BaseMapper<BankReconciliation> {
}
