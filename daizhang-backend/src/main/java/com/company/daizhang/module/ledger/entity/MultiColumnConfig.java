package com.company.daizhang.module.ledger.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 多栏账配置实体
 * 注意：此表没有version/create_by/update_by/update_time字段，不继承BaseEntity
 */
@Data
@TableName("acc_multi_column_config")
public class MultiColumnConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long accountSetId;

    private Long subjectId;

    private String subjectCode;

    private String configName;

    /**
     * 栏目项(逗号分隔)
     */
    private String columnItems;

    @TableLogic
    private Integer deleted;
}
