package com.company.daizhang.module.system.dto;

import lombok.Data;

/**
 * 账套偏好排序项(用于批量更新排序)
 */
@Data
public class AccountSetSortItem {

    /** 账套ID */
    private Long accountSetId;

    /** 排序序号 */
    private Integer sortOrder;
}
