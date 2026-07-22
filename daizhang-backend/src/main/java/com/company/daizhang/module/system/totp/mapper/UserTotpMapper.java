package com.company.daizhang.module.system.totp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.daizhang.module.system.totp.entity.UserTotp;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 TOTP Mapper (P4.2)
 */
@Mapper
public interface UserTotpMapper extends BaseMapper<UserTotp> {
}
