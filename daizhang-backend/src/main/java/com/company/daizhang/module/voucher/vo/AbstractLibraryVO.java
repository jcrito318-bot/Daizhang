package com.company.daizhang.module.voucher.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 常用摘要视图对象
 */
@Data
public class AbstractLibraryVO {

    private Long id;

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

    private Long createBy;

    private LocalDateTime createTime;
}
