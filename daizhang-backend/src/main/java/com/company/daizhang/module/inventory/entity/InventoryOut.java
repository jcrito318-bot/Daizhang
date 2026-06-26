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
@TableName("inv_out")
public class InventoryOut {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountSetId;
    private String outNo;
    private Integer outType;
    private LocalDate outDate;
    private String customer;
    private BigDecimal totalQuantity;
    private BigDecimal totalAmount;
    private BigDecimal costAmount;
    private Integer status;
    private String remark;
    private Long createBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<InventoryOutDetail> details;
}
