package com.company.daizhang.module.bank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.daizhang.module.bank.entity.BankTransaction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 银行流水Mapper
 */
@Mapper
public interface BankTransactionMapper extends BaseMapper<BankTransaction> {
}
