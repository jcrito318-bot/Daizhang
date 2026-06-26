package com.company.daizhang.module.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("inv_stock")
public class InventoryStock {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountSetId;
    private Long itemId;
    @TableField("`year`")
    private Integer year;
    @TableField("`month`")
    private Integer month;
    private BigDecimal beginQuantity;
    private BigDecimal beginAmount;
    private BigDecimal inQuantity;
    private BigDecimal inAmount;
    private BigDecimal outQuantity;
    private BigDecimal outAmount;
    private BigDecimal endQuantity;
    private BigDecimal endAmount;
    private BigDecimal unitCost;
}
