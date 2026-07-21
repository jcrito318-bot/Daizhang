package com.company.daizhang.module.accountset.controller;

import com.company.daizhang.common.annotation.SensitiveOperation;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.accountset.dto.AccountSetCreateRequest;
import com.company.daizhang.module.accountset.dto.AccountSetQueryRequest;
import com.company.daizhang.module.accountset.dto.AccountSetUpdateRequest;
import com.company.daizhang.module.accountset.service.AccountSetService;
import com.company.daizhang.module.accountset.vo.AccountSetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 账套管理控制器
 */
@Tag(name = "账套管理")
@RestController
@RequestMapping("/accountset")
@RequiredArgsConstructor
public class AccountSetController {
    
    private final AccountSetService accountSetService;
    
    @Operation(summary = "分页查询账套")
    @GetMapping("/page")
    public Result<PageResult<AccountSetVO>> page(@Valid AccountSetQueryRequest request) {
        PageResult<AccountSetVO> page = accountSetService.pageAccountSets(request);
        return Result.success(page);
    }
    
    @Operation(summary = "查询所有账套")
    @GetMapping("/list")
    public Result<List<AccountSetVO>> list() {
        List<AccountSetVO> list = accountSetService.listAllAccountSets();
        return Result.success(list);
    }
    
    @Operation(summary = "根据ID查询账套")
    @GetMapping("/{id}")
    public Result<AccountSetVO> getById(@PathVariable Long id) {
        AccountSetVO accountSet = accountSetService.getAccountSetById(id);
        return Result.success(accountSet);
    }
    
    @Operation(summary = "创建账套")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody AccountSetCreateRequest request) {
        accountSetService.createAccountSet(request);
        return Result.success();
    }
    
    @Operation(summary = "更新账套")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AccountSetUpdateRequest request) {
        accountSetService.updateAccountSet(id, request);
        return Result.success();
    }

    @Operation(summary = "启用账套")
    @PutMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        accountSetService.enableAccountSet(id);
        return Result.success();
    }

    @Operation(summary = "停用账套")
    @PutMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        accountSetService.disableAccountSet(id);
        return Result.success();
    }

    @Operation(summary = "删除账套")
    @DeleteMapping("/{id}")
    @SensitiveOperation("删除账套")
    public Result<Void> delete(@PathVariable Long id) {
        accountSetService.deleteAccountSet(id);
        return Result.success();
    }
    
    @Operation(summary = "初始化账套")
    @PostMapping("/{id}/init")
    public Result<Void> init(@PathVariable Long id) {
        accountSetService.initAccountSet(id);
        return Result.success();
    }
}
