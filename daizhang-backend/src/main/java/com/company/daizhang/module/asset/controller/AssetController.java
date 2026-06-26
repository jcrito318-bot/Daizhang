package com.company.daizhang.module.asset.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.asset.dto.*;
import com.company.daizhang.module.asset.service.AssetService;
import com.company.daizhang.module.asset.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 固定资产管理控制器
 */
@Tag(name = "固定资产管理")
@RestController
@RequestMapping("/asset")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    // ==================== 资产分类管理 ====================

    @Operation(summary = "分页查询资产分类")
    @GetMapping("/category/page")
    public Result<PageResult<AssetCategoryVO>> pageCategories(AssetCategoryQueryRequest request) {
        PageResult<AssetCategoryVO> page = assetService.pageCategories(request);
        return Result.success(page);
    }

    @Operation(summary = "查询资产分类树")
    @GetMapping("/category/tree")
    public Result<List<AssetCategoryVO>> listCategoryTree(@RequestParam Long accountSetId) {
        List<AssetCategoryVO> tree = assetService.listCategoryTree(accountSetId);
        return Result.success(tree);
    }

    @Operation(summary = "根据ID查询资产分类")
    @GetMapping("/category/{id}")
    public Result<AssetCategoryVO> getCategoryById(@PathVariable Long id) {
        AssetCategoryVO vo = assetService.getCategoryById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建资产分类")
    @PostMapping("/category")
    public Result<Void> createCategory(@Valid @RequestBody AssetCategoryCreateRequest request) {
        assetService.createCategory(request);
        return Result.success();
    }

    @Operation(summary = "更新资产分类")
    @PutMapping("/category/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody AssetCategoryUpdateRequest request) {
        assetService.updateCategory(id, request);
        return Result.success();
    }

    @Operation(summary = "删除资产分类")
    @DeleteMapping("/category/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        assetService.deleteCategory(id);
        return Result.success();
    }

    // ==================== 固定资产管理 ====================

    @Operation(summary = "分页查询固定资产")
    @GetMapping("/page")
    public Result<PageResult<FixedAssetVO>> pageAssets(FixedAssetQueryRequest request) {
        PageResult<FixedAssetVO> page = assetService.pageAssets(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询固定资产")
    @GetMapping("/{id}")
    public Result<FixedAssetVO> getAssetById(@PathVariable Long id) {
        FixedAssetVO vo = assetService.getAssetById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建固定资产")
    @PostMapping
    public Result<Void> createAsset(@Valid @RequestBody FixedAssetCreateRequest request) {
        assetService.createAsset(request);
        return Result.success();
    }

    @Operation(summary = "更新固定资产")
    @PutMapping("/{id}")
    public Result<Void> updateAsset(@PathVariable Long id, @Valid @RequestBody FixedAssetUpdateRequest request) {
        assetService.updateAsset(id, request);
        return Result.success();
    }

    @Operation(summary = "删除固定资产")
    @DeleteMapping("/{id}")
    public Result<Void> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        return Result.success();
    }

    @Operation(summary = "变更资产状态")
    @PostMapping("/change-status")
    public Result<Void> changeAssetStatus(@Valid @RequestBody AssetStatusChangeRequest request) {
        assetService.changeAssetStatus(request);
        return Result.success();
    }

    // ==================== 折旧管理 ====================

    @Operation(summary = "分页查询折旧记录")
    @GetMapping("/depreciation/page")
    public Result<PageResult<DepreciationRecordVO>> pageDepreciationRecords(DepreciationRecordQueryRequest request) {
        PageResult<DepreciationRecordVO> page = assetService.pageDepreciationRecords(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询折旧记录")
    @GetMapping("/depreciation/{id}")
    public Result<DepreciationRecordVO> getDepreciationRecordById(@PathVariable Long id) {
        DepreciationRecordVO vo = assetService.getDepreciationRecordById(id);
        return Result.success(vo);
    }

    @Operation(summary = "计提折旧")
    @PostMapping("/depreciation/calculate")
    public Result<Void> calculateDepreciation(@Valid @RequestBody DepreciationRequest request) {
        assetService.calculateDepreciation(request);
        return Result.success();
    }

    @Operation(summary = "生成折旧凭证")
    @PostMapping("/depreciation/{id}/voucher")
    public Result<Void> generateDepreciationVoucher(@PathVariable Long id) {
        assetService.generateDepreciationVoucher(id);
        return Result.success();
    }

    @Operation(summary = "批量生成折旧凭证")
    @PostMapping("/depreciation/batch-voucher")
    public Result<Void> batchGenerateDepreciationVoucher(@Valid @RequestBody DepreciationRequest request) {
        assetService.batchGenerateDepreciationVoucher(request);
        return Result.success();
    }

    @Operation(summary = "获取资产报表")
    @GetMapping("/report")
    public Result<AssetReportVO> report(@RequestParam Long accountSetId,
                                        @RequestParam Integer year) {
        AssetReportVO report = assetService.getAssetReport(accountSetId, year);
        return Result.success(report);
    }
}
