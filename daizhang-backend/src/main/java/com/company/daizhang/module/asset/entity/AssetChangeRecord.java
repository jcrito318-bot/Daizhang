package com.company.daizhang.module.asset.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产变动记录实体
 */
@Data
@TableName("asset_change_record")
public class AssetChangeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 资产ID
     */
    private Long assetId;

    /**
     * 变动类型:购入/出售/报废/调拨/盘亏
     */
    private String changeType;

    /**
     * 变动日期
     */
    private LocalDate changeDate;

    /**
     * 变动金额
     */
    private BigDecimal changeAmount;

    /**
     * 原部门
     */
    private String fromDepartment;

    /**
     * 新部门
     */
    private String toDepartment;

    /**
     * 关联凭证ID
     */
    private Long voucherId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
