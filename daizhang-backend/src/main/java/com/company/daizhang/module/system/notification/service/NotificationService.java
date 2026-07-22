package com.company.daizhang.module.system.notification.service;

import com.company.daizhang.module.system.notification.vo.NotificationVO;

import java.util.List;

/**
 * 站内信通知服务 (B3/B7)
 */
public interface NotificationService {

    /**
     * 发送通知(单条)
     *
     * @param userId       接收人用户ID(null=全员广播)
     * @param accountSetId 关联账套ID(null=非账套级)
     * @param customerId    关联客户ID(null=非客户级)
     * @param type         通知类型: ARREARS_WARNING/CONTRACT_EXPIRING/PAYSHEET/SYSTEM
     * @param title        通知标题
     * @param content      通知内容
     * @param level        级别: INFO/WARN/URGENT
     */
    void sendNotification(Long userId, Long accountSetId, Long customerId, String type,
                          String title, String content, String level);

    /**
     * 查询我的全部通知(未读+已读)
     *
     * @param userId 用户ID
     * @return 通知列表
     */
    List<NotificationVO> listMyNotifications(Long userId);

    /**
     * 查询未读通知
     *
     * @param userId 用户ID
     * @return 未读通知列表
     */
    List<NotificationVO> listUnread(Long userId);

    /**
     * 标记单条通知为已读
     *
     * @param notificationId 通知ID
     * @param userId         用户ID(用于校验归属,防止越权操作他人通知)
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * 全部标记已读
     *
     * @param userId 用户ID
     */
    void markAllAsRead(Long userId);

    /**
     * 统计未读数量
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    int countUnread(Long userId);
}
