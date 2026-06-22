package com.company.daizhang.module.salary.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 薪资项目视图对象
 */
@Data
public class SalaryItemVO {

    private Long id;

    private Long accountSetId;

    private String itemName;

    private String itemCode;

    private String itemType;

    private String calculationMethod;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;

    private String createByName;
}
