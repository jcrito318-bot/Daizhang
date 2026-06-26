package com.company.daizhang.module.industrycommerce.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工商外勤任务实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ic_task")
public class IndustryCommerceTask extends BaseEntity {

    /**
     * 工商服务ID
     */
    private Long serviceId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务状态:0待处理 1进行中 2已完成 3已取消
     */
    private Integer taskStatus;

    /**
     * 经办人ID
     */
    private Long assigneeId;

    /**
     * 外勤日期
     */
    private LocalDate fieldDate;

    /**
     * 地点
     */
    private String location;

    /**
     * 备注
     */
    private String remark;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;
}
