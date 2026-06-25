package com.company.daizhang.module.biz.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代账服务任务视图对象
 */
@Data
public class ServiceTaskVO {

    private Long id;

    private Long accountSetId;

    private Long customerId;

    private Integer year;

    private Integer month;

    private Long nodeId;

    private String nodeName;

    private Long assigneeId;

    private String assigneeName;

    private Integer taskStatus;

    private LocalDateTime completeTime;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
