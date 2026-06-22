package com.company.daizhang.module.salary.dto;

import lombok.Data;

/**
 * 薪资项目更新请求
 */
@Data
public class SalaryItemUpdateRequest {

    private String itemName;

    private String itemCode;

    private String itemType;

    private String calculationMethod;

    private String remark;
}
