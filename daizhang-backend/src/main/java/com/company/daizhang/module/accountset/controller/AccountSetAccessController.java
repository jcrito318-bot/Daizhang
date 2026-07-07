package com.company.daizhang.module.accountset.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.accountset.vo.UserAccountSetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 账套权限管理控制器(IDOR越权治理)
 * <p>
 * 提供账套访问权限的分配、移除、查询接口。
 * 分配/移除仅账套OWNER或管理员可操作;查询需对账套有访问权。
 */
@Tag(name = "账套权限管理")
@RestController
@RequestMapping("/accountset/access")
@RequiredArgsConstructor
public class AccountSetAccessController {

    private final AccountSetAccessService accountSetAccessService;

    @Operation(summary = "分配账套访问权限(仅OWNER或管理员)")
    @PostMapping("/assign")
    @RequireAccountSetAccess(RequireAccountSetAccess.AccessLevel.OWNER)
    public Result<Void> assign(@RequestParam Long userId,
                               @RequestParam Long accountSetId,
                               @RequestParam String roleType) {
        accountSetAccessService.assignAccountSet(userId, accountSetId, roleType);
        return Result.success();
    }

    @Operation(summary = "移除账套访问权限(仅OWNER或管理员)")
    @DeleteMapping("/revoke")
    @RequireAccountSetAccess(RequireAccountSetAccess.AccessLevel.OWNER)
    public Result<Void> revoke(@RequestParam Long userId,
                               @RequestParam Long accountSetId) {
        accountSetAccessService.revokeAccountSet(userId, accountSetId);
        return Result.success();
    }

    @Operation(summary = "查询账套下的所有用户及角色")
    @GetMapping("/users")
    @RequireAccountSetAccess
    public Result<List<UserAccountSetVO>> listUsers(@RequestParam Long accountSetId) {
        List<UserAccountSetVO> users = accountSetAccessService.listAccountSetUsers(accountSetId);
        return Result.success(users);
    }

    @Operation(summary = "查询用户可访问的所有账套及角色")
    @GetMapping("/user/{userId}/accountsets")
    public Result<List<UserAccountSetVO>> listUserAccountSets(@PathVariable Long userId) {
        List<UserAccountSetVO> accountSets = accountSetAccessService.listUserAccountSets(userId);
        return Result.success(accountSets);
    }
}
