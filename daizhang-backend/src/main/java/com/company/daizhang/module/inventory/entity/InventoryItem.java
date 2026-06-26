package com.company.daizhang.module.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inv_item")
public class InventoryItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountSetId;
    private String itemCode;
    private String itemName;
    private String specification;
    private String unit;
    private BigDecimal unitPrice;
    private String category;
    private Integer status;
    private String remark;
    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private BigDecimal stockQuantity;
    @TableField(exist = false)
    private BigDecimal stockAmount;
}
