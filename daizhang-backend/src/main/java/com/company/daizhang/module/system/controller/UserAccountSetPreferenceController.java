package com.company.daizhang.module.system.controller;

import com.company.daizhang.common.result.Result;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.system.dto.AccountSetSortItem;
import com.company.daizhang.module.system.service.UserAccountSetPreferenceService;
import com.company.daizhang.module.system.vo.AccountSetPreferenceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户账套偏好控制器(顶部账套切换器优化:最近访问 + 收藏置顶)
 * <p>
 * 所有端点均使用当前登录用户 ID({@link SecurityUtils#getCurrentUserIdRequired()}),
 * 不接受前端传入 userId,避免越权读写他人偏好。
 * 对 accountSetId 的访问权统一由 {@link AccountSetAccessService#checkAccess} 校验。
 */
@Tag(name = "账套偏好管理")
@RestController
@RequestMapping("/account-set")
@RequiredArgsConstructor
public class UserAccountSetPreferenceController {

    private final UserAccountSetPreferenceService preferenceService;
    private final AccountSetAccessService accountSetAccessService;

    @Operation(summary = "获取当前用户的账套偏好列表(收藏在前,按最近访问时间倒序)")
    @GetMapping("/preferences")
    public Result<List<AccountSetPreferenceVO>> listPreferences() {
        Long userId = SecurityUtils.getCurrentUserIdRequired();
        List<AccountSetPreferenceVO> list = preferenceService.listPreferences(userId);
        return Result.success(list);
    }

    @Operation(summary = "记录账套访问(异步更新最近访问时间+访问次数,不阻塞主流程)")
    @PostMapping("/{accountSetId}/access")
    public Result<Void> recordAccess(@PathVariable Long accountSetId) {
        // 同步校验访问权(快速失败),通过后异步记录
        accountSetAccessService.checkAccess(accountSetId);
        Long userId = SecurityUtils.getCurrentUserIdRequired();
        preferenceService.recordAccess(userId, accountSetId);
        return Result.success();
    }

    @Operation(summary = "切换账套收藏状态")
    @PostMapping("/{accountSetId}/favorite")
    public Result<Boolean> toggleFavorite(@PathVariable Long accountSetId) {
        accountSetAccessService.checkAccess(accountSetId);
        Long userId = SecurityUtils.getCurrentUserIdRequired();
        boolean favorite = preferenceService.toggleFavorite(userId, accountSetId);
        return Result.success(favorite);
    }

    @Operation(summary = "批量更新账套偏好排序")
    @PutMapping("/preferences/sort")
    public Result<Void> updateSort(@RequestBody List<AccountSetSortItem> items) {
        Long userId = SecurityUtils.getCurrentUserIdRequired();
        // 逐个校验访问权,防止越权为无权访问的账套设置排序
        if (items != null) {
            for (AccountSetSortItem item : items) {
                if (item != null && item.getAccountSetId() != null) {
                    accountSetAccessService.checkAccess(item.getAccountSetId());
                }
            }
        }
        preferenceService.updateSort(userId, items);
        return Result.success();
    }
}
