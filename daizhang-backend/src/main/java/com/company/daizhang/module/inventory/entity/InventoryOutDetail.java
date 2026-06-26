package com.company.daizhang.module.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("inv_out_detail")
public class InventoryOutDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long outId;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String specification;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal unitCost;
    private BigDecimal costAmount;
    private String remark;
}
