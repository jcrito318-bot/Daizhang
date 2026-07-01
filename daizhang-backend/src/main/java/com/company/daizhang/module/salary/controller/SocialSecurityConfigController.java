package com.company.daizhang.module.salary.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.salary.dto.SocialSecurityConfigRequest;
import com.company.daizhang.module.salary.service.SocialSecurityConfigService;
import com.company.daizhang.module.salary.vo.SocialSecurityCalculationVO;
import com.company.daizhang.module.salary.vo.SocialSecurityConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 社保公积金配置管理控制器
 */
@Slf4j
@Tag(name = "社保公积金配置管理")
@RestController
@RequestMapping("/salary/social-security")
@RequiredArgsConstructor
public class SocialSecurityConfigController {

    private final SocialSecurityConfigService socialSecurityConfigService;

    @Operation(summary = "获取社保公积金配置")
    @GetMapping
    @RequireAccountSetAccess
    public Result<SocialSecurityConfigVO> get(@RequestParam Long accountSetId, @RequestParam Integer year) {
        SocialSecurityConfigVO vo = socialSecurityConfigService.getConfig(accountSetId, year);
        return Result.success(vo);
    }

    @Operation(summary = "保存或更新社保公积金配置")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody SocialSecurityConfigRequest request) {
        socialSecurityConfigService.saveConfig(request);
        return Result.success();
    }

    @Operation(summary = "根据基数计算社保")
    @GetMapping("/calculate")
    @RequireAccountSetAccess
    public Result<SocialSecurityCalculationVO> calculate(@RequestParam Long accountSetId,
                                                         @RequestParam Integer year,
                                                         @RequestParam BigDecimal baseSalary) {
        SocialSecurityCalculationVO vo = socialSecurityConfigService.calculate(accountSetId, year, baseSalary);
        return Result.success(vo);
    }
}
