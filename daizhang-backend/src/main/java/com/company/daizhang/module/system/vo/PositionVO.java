package com.company.daizhang.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 岗位视图对象
 */
@Data
public class PositionVO {

    private Long id;

    private String positionCode;

    private String positionName;

    private Long departmentId;

    private String description;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
