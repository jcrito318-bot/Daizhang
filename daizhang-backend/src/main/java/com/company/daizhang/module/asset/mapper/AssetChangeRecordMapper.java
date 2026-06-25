package com.company.daizhang.module.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.daizhang.module.asset.entity.AssetChangeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资产变动记录Mapper
 */
@Mapper
public interface AssetChangeRecordMapper extends BaseMapper<AssetChangeRecord> {
}
