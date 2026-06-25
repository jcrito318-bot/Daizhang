package com.company.daizhang.module.subject.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 辅助核算项目实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acc_auxiliary_item")
public class AuxiliaryItem extends BaseEntity {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 类别ID
     */
    private Long categoryId;

    /**
     * 项目编码
     */
    private String itemCode;

    /**
     * 项目名称
     */
    private String itemName;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 状态 0-禁用 1-正常
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
