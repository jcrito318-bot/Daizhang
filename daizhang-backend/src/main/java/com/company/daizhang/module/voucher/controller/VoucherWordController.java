package com.company.daizhang.module.voucher.controller;

import cn.hutool.core.bean.BeanUtil;
import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.voucher.entity.VoucherWord;
import com.company.daizhang.module.voucher.service.VoucherWordService;
import com.company.daizhang.module.voucher.vo.VoucherWordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 凭证字管理控制器
 */
@Tag(name = "凭证字管理")
@RestController
@RequestMapping("/voucher/word")
@RequiredArgsConstructor
public class VoucherWordController {

    private final VoucherWordService voucherWordService;
    private final AccountSetAccessService accountSetAccessService;

    @Operation(summary = "根据账套ID查询凭证字列表")
    @GetMapping("/list")
    @RequireAccountSetAccess
    public Result<List<VoucherWordVO>> list(@RequestParam Long accountSetId) {
        List<VoucherWord> list = voucherWordService.listByAccountSetId(accountSetId);
        List<VoucherWordVO> voList = list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    @Operation(summary = "创建凭证字")
    @PostMapping
    @RequireAccountSetAccess
    public Result<Void> create(@Valid @RequestBody VoucherWordVO request) {
        VoucherWord voucherWord = new VoucherWord();
        BeanUtil.copyProperties(request, voucherWord);
        voucherWordService.save(voucherWord);
        return Result.success();
    }

    @Operation(summary = "更新凭证字")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody VoucherWordVO request) {
        VoucherWord voucherWord = voucherWordService.getById(id);
        if (voucherWord == null) {
            return Result.error(404, "凭证字不存在");
        }
        // IDOR治理:校验当前用户对该凭证字所属账套的所有者权限
        accountSetAccessService.checkOwner(voucherWord.getAccountSetId());
        // 保存原始accountSetId,不允许通过update修改所属账套
        Long originalAccountSetId = voucherWord.getAccountSetId();
        BeanUtil.copyProperties(request, voucherWord);
        voucherWord.setId(id);
        voucherWord.setAccountSetId(originalAccountSetId);
        voucherWordService.updateById(voucherWord);
        return Result.success();
    }

    @Operation(summary = "删除凭证字")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        VoucherWord voucherWord = voucherWordService.getById(id);
        if (voucherWord == null) {
            return Result.error(404, "凭证字不存在");
        }
        // IDOR治理:校验当前用户对该凭证字所属账套的所有者权限
        accountSetAccessService.checkOwner(voucherWord.getAccountSetId());
        voucherWordService.removeById(id);
        return Result.success();
    }

    private VoucherWordVO convertToVO(VoucherWord voucherWord) {
        VoucherWordVO vo = new VoucherWordVO();
        BeanUtil.copyProperties(voucherWord, vo);
        return vo;
    }
}
