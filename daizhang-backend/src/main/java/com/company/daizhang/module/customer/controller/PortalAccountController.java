package com.company.daizhang.module.customer.controller;

import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.customer.dto.PortalAccountRequest;
import com.company.daizhang.module.customer.service.PortalAccountService;
import com.company.daizhang.module.customer.vo.PortalAccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户看账门户控制器
 */
@Tag(name = "客户看账门户管理")
@RestController
@RequestMapping("/portal-account")
@RequiredArgsConstructor
public class PortalAccountController {

    private final PortalAccountService portalAccountService;

    @Operation(summary = "查询客户门户列表")
    @GetMapping("/list")
    public Result<List<PortalAccountVO>> list(@RequestParam(required = false) Long customerId) {
        List<PortalAccountVO> list = portalAccountService.listPortals(customerId);
        return Result.success(list);
    }

    @Operation(summary = "创建门户账户")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody PortalAccountRequest request) {
        portalAccountService.createPortal(request);
        return Result.success();
    }

    @Operation(summary = "更新门户账户")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PortalAccountRequest request) {
        portalAccountService.updatePortal(id, request);
        return Result.success();
    }

    @Operation(summary = "删除门户账户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        portalAccountService.deletePortal(id);
        return Result.success();
    }

    @Operation(summary = "重置门户密码")
    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        portalAccountService.resetPassword(id, newPassword);
        return Result.success();
    }

    @Operation(summary = "更新门户状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        portalAccountService.updateStatus(id, status);
        return Result.success();
    }
}
