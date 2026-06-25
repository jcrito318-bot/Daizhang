package com.company.daizhang.module.subject.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 辅助核算类别实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acc_auxiliary_category")
public class AuxiliaryCategory extends BaseEntity {

    /**
     * 账套ID(0-全局)
     */
    private Long accountSetId;

    /**
     * 类别编码
     */
    private String categoryCode;

    /**
     * 类别名称
     */
    private String categoryName;

    /**
     * 类型:客户/供应商/部门/员工/项目
     */
    private String categoryType;

    /**
     * 备注
     */
    private String remark;
}
