package com.company.daizhang.module.system.backup.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.daizhang.module.system.backup.entity.BackupRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据库备份记录 Mapper (P3.3)
 */
@Mapper
public interface BackupRecordMapper extends BaseMapper<BackupRecord> {
}
