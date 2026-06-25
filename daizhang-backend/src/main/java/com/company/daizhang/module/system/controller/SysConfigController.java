package com.company.daizhang.module.system.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.system.dto.SysConfigRequest;
import com.company.daizhang.module.system.service.SysConfigService;
import com.company.daizhang.module.system.vo.SysConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统设置控制器
 */
@Tag(name = "系统设置管理")
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @Operation(summary = "分页查询系统设置")
    @GetMapping("/page")
    public Result<PageResult<SysConfigVO>> page(
            @RequestParam(required = false) String configKey,
            @RequestParam(required = false) String configName,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<SysConfigVO> page = sysConfigService.pageConfigs(configKey, configName, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "根据key获取配置值")
    @GetMapping("/value")
    public Result<String> getValue(@RequestParam String key) {
        String value = sysConfigService.getConfigValue(key);
        return Result.success(value);
    }

    @Operation(summary = "创建系统设置")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody SysConfigRequest request) {
        sysConfigService.createConfig(request);
        return Result.success();
    }

    @Operation(summary = "更新系统设置")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysConfigRequest request) {
        sysConfigService.updateConfig(id, request);
        return Result.success();
    }

    @Operation(summary = "删除系统设置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysConfigService.deleteConfig(id);
        return Result.success();
    }
}
