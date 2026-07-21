package com.company.daizhang.module.voucher.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 常用摘要库实体
 * <p>
 * 代账会计在录入凭证时,常用摘要(如"计提工资"、"计提折旧"、"缴纳社保")会反复输入。
 * 将常用摘要保存为摘要库,在凭证录入页面通过 el-autocomplete 模糊搜索,
 * 并按使用次数(use_count DESC)智能排序,提升录入效率。
 * <p>
 * 凭证保存时调用摘要使用次数 +1 接口,自动累计使用次数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("abstract_library")
public class AbstractLibrary extends BaseEntity {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 摘要文本
     */
    private String abstractText;

    /**
     * 分类: 工资/折旧/社保/税金/报销/采购/销售/其他
     */
    private String abstractCategory;

    /**
     * 使用次数(用于智能排序)
     */
    private Integer useCount;
}
