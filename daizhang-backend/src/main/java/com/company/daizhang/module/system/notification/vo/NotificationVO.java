package com.company.daizhang.module.system.notification.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内信通知视图对象 (B3/B7)
 */
@Data
public class NotificationVO {

    /**
     * 通知ID
     */
    private Long id;

    /**
     * 接收人用户ID(null=全员广播)
     */
    private Long userId;

    /**
     * 关联账套ID
     */
    private Long accountSetId;

    /**
     * 关联客户ID
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

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
