package com.company.daizhang.module.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("inv_in")
public class InventoryIn {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountSetId;
    private String inNo;
    private Integer inType;
    private LocalDate inDate;
    private String supplier;
    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;
    private Integer status;
    private String remark;
    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<InventoryInDetail> details;
}
