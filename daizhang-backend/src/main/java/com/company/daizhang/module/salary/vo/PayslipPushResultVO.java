package com.company.daizhang.module.salary.vo;

import lombok.Data;

/**
 * 工资条批量推送结果视图对象
 */
@Data
public class PayslipPushResultVO {

    /**
     * 总数
     */
    private Integer totalCount;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failCount;

    /**
     * 提示信息
     */
    private String message;
}
