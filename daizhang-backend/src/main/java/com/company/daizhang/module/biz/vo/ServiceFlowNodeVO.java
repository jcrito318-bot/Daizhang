package com.company.daizhang.module.biz.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代账服务流程节点视图对象
 */
@Data
public class ServiceFlowNodeVO {

    private Long id;

    private String nodeCode;

    private String nodeName;

    private Integer sortOrder;

    private String description;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
