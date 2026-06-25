package com.company.daizhang.module.system.controller;

import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.system.dto.IndustryTemplateRequest;
import com.company.daizhang.module.system.service.IndustryTemplateService;
import com.company.daizhang.module.system.vo.IndustryTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行业模板控制器
 */
@Tag(name = "行业模板管理")
@RestController
@RequestMapping("/system/industry-template")
@RequiredArgsConstructor
public class IndustryTemplateController {

    private final IndustryTemplateService industryTemplateService;

    @Operation(summary = "查询行业模板列表")
    @GetMapping("/list")
    public Result<List<IndustryTemplateVO>> list(@RequestParam(required = false) String industryType) {
        List<IndustryTemplateVO> list = industryTemplateService.listTemplates(industryType);
        return Result.success(list);
    }

    @Operation(summary = "根据ID查询行业模板")
    @GetMapping("/{id}")
    public Result<IndustryTemplateVO> getById(@PathVariable Long id) {
        IndustryTemplateVO vo = industryTemplateService.getTemplateById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建行业模板")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody IndustryTemplateRequest request) {
        industryTemplateService.createTemplate(request);
        return Result.success();
    }

    @Operation(summary = "更新行业模板")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody IndustryTemplateRequest request) {
        industryTemplateService.updateTemplate(id, request);
        return Result.success();
    }

    @Operation(summary = "删除行业模板")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        industryTemplateService.deleteTemplate(id);
        return Result.success();
    }
}
