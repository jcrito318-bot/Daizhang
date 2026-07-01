package com.company.daizhang.module.voucher.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.voucher.dto.VoucherTemplateRequest;
import com.company.daizhang.module.voucher.service.VoucherTemplateService;
import com.company.daizhang.module.voucher.vo.VoucherTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 凭证模板管理控制器
 */
@Tag(name = "凭证模板管理")
@RestController
@RequestMapping("/voucher/template")
@RequiredArgsConstructor
public class VoucherTemplateController {

    private final VoucherTemplateService voucherTemplateService;

    @Operation(summary = "分页查询凭证模板")
    @GetMapping("/page")
    @RequireAccountSetAccess
    public Result<PageResult<VoucherTemplateVO>> page(@RequestParam Long accountSetId,
                                                       @RequestParam(required = false) String templateName,
                                                       @RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<VoucherTemplateVO> page = voucherTemplateService.pageTemplates(accountSetId, templateName, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询凭证模板")
    @GetMapping("/{id}")
    public Result<VoucherTemplateVO> getById(@PathVariable Long id) {
        VoucherTemplateVO vo = voucherTemplateService.getTemplateById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建凭证模板")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody VoucherTemplateRequest request) {
        voucherTemplateService.createTemplate(request);
        return Result.success();
    }

    @Operation(summary = "更新凭证模板")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody VoucherTemplateRequest request) {
        voucherTemplateService.updateTemplate(id, request);
        return Result.success();
    }

    @Operation(summary = "删除凭证模板")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        voucherTemplateService.deleteTemplate(id);
        return Result.success();
    }
}
