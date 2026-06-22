package com.company.daizhang.module.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.daizhang.module.document.entity.Document;
import org.apache.ibatis.annotations.Mapper;

/**
 * 票据Mapper
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}
