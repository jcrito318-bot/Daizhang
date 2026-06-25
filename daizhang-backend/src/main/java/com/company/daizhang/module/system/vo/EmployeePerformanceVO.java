package com.company.daizhang.module.system.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 员工绩效视图对象
 */
@Data
public class EmployeePerformanceVO {

    private Long id;

    private Long userId;

    private String userName;

    private Integer year;

    private Integer month;

    private Integer voucherCount;

    private Integer auditCount;

    private Integer taskCompleteCount;

    private Integer customerCount;

    private BigDecimal performanceScore;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
