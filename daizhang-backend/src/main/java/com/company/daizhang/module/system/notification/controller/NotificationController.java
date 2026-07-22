package com.company.daizhang.module.system.notification.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.system.notification.service.NotificationService;
import com.company.daizhang.module.system.notification.vo.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 站内信通知控制器 (B3/B7)
 */
@Tag(name = "站内信通知", description = "通知查询、未读统计、标记已读")
@RestController
@RequestMapping("/system/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "分页查询我的通知(未读+已读)")
    @GetMapping("/page")
    public Result<PageResult<NotificationVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = SecurityUtils.getCurrentUserIdRequired();
        List<NotificationVO> all = notificationService.listMyNotifications(userId);
        int total = all.size();
        int fromIndex = Math.min((pageNum - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<NotificationVO> pageList = fromIndex < toIndex
                ? all.subList(fromIndex, toIndex)
                : List.of();
        return Result.success(new PageResult<>(pageList, (long) total, pageNum, pageSize));
    }

    @Operation(summary = "查询未读通知")
    @GetMapping("/unread")
    public Result<List<NotificationVO>> unread() {
        Long userId = SecurityUtils.getCurrentUserIdRequired();
        return Result.success(notificationService.listUnread(userId));
    }

    @Operation(summary = "未读通知数量")
    @GetMapping("/unread/count")
    public Result<Integer> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserIdRequired();
        return Result.success(notificationService.countUnread(userId));
    }

    @Operation(summary = "标记单条通知为已读")
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserIdRequired();
        notificationService.markAsRead(id, userId);
        return Result.success();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        Long userId = SecurityUtils.getCurrentUserIdRequired();
        notificationService.markAllAsRead(userId);
        return Result.success();
    }
}
