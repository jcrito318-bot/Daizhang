package com.company.daizhang.module.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("inv_in_detail")
public class InventoryInDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inId;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String specification;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String remark;
}
