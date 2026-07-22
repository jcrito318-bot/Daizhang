package com.company.daizhang.module.system.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.daizhang.module.system.security.entity.LoginAttempt;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录尝试记录 Mapper (P4.3)
 */
@Mapper
public interface LoginAttemptMapper extends BaseMapper<LoginAttempt> {
}
