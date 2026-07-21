package com.company.daizhang.module.voucher.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.voucher.dto.VoucherTemplateQueryRequest;
import com.company.daizhang.module.voucher.dto.VoucherTemplateRequest;
import com.company.daizhang.module.voucher.service.VoucherTemplateService;
import com.company.daizhang.module.voucher.vo.VoucherTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 凭证模板管理控制器
 * <p>
 * 提供凭证模板的增删改查及"应用模板"能力。代账会计可将每月重复录入的凭证
 * (如工资/折旧/社保)保存为模板,后续一键调用以提升录入效率。
 * <p>
 * 写端点(创建/更新/删除)使用 {@code @RequireAccountSetAccess(value = OWNER, required = false)}
 * 进行 IDOR 治理:注解层面从请求体中解析 accountSetId 并校验所有者权限,
 * required=false 允许 accountSetId 为可选场景(实际由 Service 层兜底校验)。
 */
@Tag(name = "凭证模板管理")
@RestController
@RequestMapping("/voucher/template")
@RequiredArgsConstructor
public class VoucherTemplateController {

    private final VoucherTemplateService voucherTemplateService;

    @Operation(summary = "分页查询凭证模板")
    @GetMapping("/page")
    public Result<PageResult<VoucherTemplateVO>> page(@Valid VoucherTemplateQueryRequest request) {
        PageResult<VoucherTemplateVO> page = voucherTemplateService.pageTemplates(request);
        return Result.success(page);
    }

    @Operation(summary = "不分页查询凭证模板(下拉用)")
    @GetMapping("/list")
    @RequireAccountSetAccess(required = false)
    public Result<List<VoucherTemplateVO>> list(@RequestParam Long accountSetId) {
        List<VoucherTemplateVO> list = voucherTemplateService.listTemplates(accountSetId);
        return Result.success(list);
    }

    @Operation(summary = "根据ID查询凭证模板(含明细)")
    @GetMapping("/{id}")
    public Result<VoucherTemplateVO> getById(@PathVariable Long id) {
        VoucherTemplateVO vo = voucherTemplateService.getTemplateById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建凭证模板")
    @PostMapping
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> create(@Valid @RequestBody VoucherTemplateRequest request) {
        voucherTemplateService.createTemplate(request);
        return Result.success();
    }

    @Operation(summary = "更新凭证模板")
    @PutMapping("/{id}")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody VoucherTemplateRequest request) {
        voucherTemplateService.updateTemplate(id, request);
        return Result.success();
    }

    @Operation(summary = "删除凭证模板")
    @DeleteMapping("/{id}")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> delete(@PathVariable Long id) {
        voucherTemplateService.deleteTemplate(id);
        return Result.success();
    }

    @Operation(summary = "应用模板,返回构造好的凭证数据(不直接保存,由前端调 voucherApi.create 保存)")
    @PostMapping("/{id}/apply")
    public Result<VoucherTemplateVO> apply(@PathVariable Long id) {
        VoucherTemplateVO vo = voucherTemplateService.applyTemplate(id);
        return Result.success(vo);
    }
}
