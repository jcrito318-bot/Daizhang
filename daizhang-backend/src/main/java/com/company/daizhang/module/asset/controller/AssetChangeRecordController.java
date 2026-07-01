package com.company.daizhang.module.asset.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.asset.dto.AssetChangeRecordRequest;
import com.company.daizhang.module.asset.service.AssetChangeRecordService;
import com.company.daizhang.module.asset.vo.AssetChangeRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资产变动记录控制器
 */
@Tag(name = "资产变动记录")
@RestController
@RequestMapping("/asset/change-record")
@RequiredArgsConstructor
public class AssetChangeRecordController {

    private final AssetChangeRecordService assetChangeRecordService;

    @Operation(summary = "分页查询资产变动记录")
    @GetMapping("/page")
    @RequireAccountSetAccess
    public Result<PageResult<AssetChangeRecordVO>> page(
            @RequestParam(required = false) Long accountSetId,
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) String changeType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<AssetChangeRecordVO> page = assetChangeRecordService.pageRecords(accountSetId, assetId, changeType, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "根据资产ID查询变动记录列表")
    @GetMapping("/list")
    public Result<List<AssetChangeRecordVO>> listByAssetId(@RequestParam Long assetId) {
        List<AssetChangeRecordVO> list = assetChangeRecordService.listByAssetId(assetId);
        return Result.success(list);
    }

    @Operation(summary = "创建资产变动记录")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody AssetChangeRecordRequest request) {
        assetChangeRecordService.createRecord(request);
        return Result.success();
    }

    @Operation(summary = "删除资产变动记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        assetChangeRecordService.deleteRecord(id);
        return Result.success();
    }
}
