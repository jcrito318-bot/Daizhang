package com.company.daizhang.module.bank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.daizhang.module.bank.entity.BankAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 银行账户Mapper
 */
@Mapper
public interface BankAccountMapper extends BaseMapper<BankAccount> {
}
