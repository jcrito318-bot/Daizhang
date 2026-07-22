package com.company.daizhang.module.system.notification.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.system.notification.entity.Notification;
import com.company.daizhang.module.system.notification.mapper.NotificationMapper;
import com.company.daizhang.module.system.notification.service.NotificationService;
import com.company.daizhang.module.system.notification.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 站内信通知服务实现 (B3/B7)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    /**
     * 默认级别(发送时未指定 level 使用)
     */
    private static final String DEFAULT_LEVEL = "INFO";

    /**
     * 默认状态:0-未读
     */
    private static final int STATUS_UNREAD = 0;

    /**
     * 已读状态:1-已读
     */
    private static final int STATUS_READ = 1;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(Long userId, Long accountSetId, Long customerId, String type,
                                 String title, String content, String level) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setAccountSetId(accountSetId);
        notification.setCustomerId(customerId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setLevel(level != null ? level : DEFAULT_LEVEL);
        notification.setStatus(STATUS_UNREAD);
        notificationMapper.insert(notification);
        log.info("发送站内信通知: type={}, userId={}, customerId={}, title={}", type, userId, customerId, title);
    }

    @Override
    public List<NotificationVO> listMyNotifications(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        // 仅返回直接归属当前用户的通知(广播通知 user_id 为 null,如需查询广播需另行接口)
        wrapper.eq(Notification::getUserId, userId)
               .orderByDesc(Notification::getCreateTime);
        return notificationMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationVO> listUnread(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getStatus, STATUS_UNREAD)
               .orderByDesc(Notification::getCreateTime);
        return notificationMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "通知不存在");
        }
        // IDOR治理:防止用户操作他人通知(广播通知 user_id 为 null,仅管理员可标记,此处跳过普通用户场景)
        if (notification.getUserId() != null && !notification.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "无权操作此通知");
        }
        LambdaUpdateWrapper<Notification> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Notification::getId, notificationId)
                     .eq(Notification::getStatus, STATUS_UNREAD)
                     .set(Notification::getStatus, STATUS_READ);
        notificationMapper.update(null, updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<Notification> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Notification::getUserId, userId)
                     .eq(Notification::getStatus, STATUS_UNREAD)
                     .set(Notification::getStatus, STATUS_READ);
        notificationMapper.update(null, updateWrapper);
    }

    @Override
    public int countUnread(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getStatus, STATUS_UNREAD);
        Long count = notificationMapper.selectCount(wrapper);
        return count == null ? 0 : count.intValue();
    }

    private NotificationVO convertToVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        BeanUtil.copyProperties(notification, vo);
        return vo;
    }
}
