package com.company.daizhang.module.system.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 站内信通知实体 (B3/B7)
 * <p>
 * 对应 sys_notification 表,通用通知承载催收预警/合同到期/工资单/系统广播等场景。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notification")
public class Notification extends BaseEntity {

    /**
     * 接收人用户ID(null=全员广播)
     */
    private Long userId;

    /**
     * 关联账套ID(null=非账套级)
     */
    private Long accountSetId;

    /**
     * 关联客户ID(null=非客户级)
     */
    private Long customerId;

    /**
     * 通知类型: ARREARS_WARNING/CONTRACT_EXPIRING/PAYSHEET/SYSTEM
     */
    private String type;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 级别: INFO/WARN/URGENT
     */
    private String level;

    /**
     * 状态: 0-未读 1-已读
     */
    private Integer status;
}
