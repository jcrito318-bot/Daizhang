package com.company.daizhang.module.system.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.daizhang.module.system.security.entity.PasswordHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 密码历史 Mapper (P4.3)
 */
@Mapper
public interface PasswordHistoryMapper extends BaseMapper<PasswordHistory> {
}
