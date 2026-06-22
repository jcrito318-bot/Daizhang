package com.company.daizhang.module.voucher.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.voucher.dto.VoucherCreateRequest;
import com.company.daizhang.module.voucher.dto.VoucherQueryRequest;
import com.company.daizhang.module.voucher.dto.VoucherUpdateRequest;
import com.company.daizhang.module.voucher.service.VoucherService;
import com.company.daizhang.module.voucher.vo.VoucherVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 凭证管理控制器
 */
@Tag(name = "凭证管理")
@RestController
@RequestMapping("/voucher")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @Operation(summary = "分页查询凭证")
    @GetMapping("/page")
    public Result<PageResult<VoucherVO>> page(VoucherQueryRequest request) {
        PageResult<VoucherVO> page = voucherService.pageVouchers(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询凭证")
    @GetMapping("/{id}")
    public Result<VoucherVO> getById(@PathVariable Long id) {
        VoucherVO voucher = voucherService.getVoucherById(id);
        return Result.success(voucher);
    }

    @Operation(summary = "创建凭证")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody VoucherCreateRequest request) {
        voucherService.createVoucher(request);
        return Result.success();
    }

    @Operation(summary = "更新凭证")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody VoucherUpdateRequest request) {
        voucherService.updateVoucher(id, request);
        return Result.success();
    }

    @Operation(summary = "删除凭证")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return Result.success();
    }

    @Operation(summary = "审核凭证")
    @PostMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id) {
        voucherService.auditVoucher(id);
        return Result.success();
    }

    @Operation(summary = "反审核凭证")
    @PostMapping("/{id}/unaudit")
    public Result<Void> unaudit(@PathVariable Long id) {
        voucherService.unauditVoucher(id);
        return Result.success();
    }

    @Operation(summary = "过账凭证")
    @PostMapping("/{id}/post")
    public Result<Void> post(@PathVariable Long id) {
        voucherService.postVoucher(id);
        return Result.success();
    }
}
