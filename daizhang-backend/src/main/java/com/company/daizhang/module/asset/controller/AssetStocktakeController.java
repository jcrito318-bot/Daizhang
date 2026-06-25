package com.company.daizhang.module.asset.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.asset.dto.AssetStocktakeQueryRequest;
import com.company.daizhang.module.asset.dto.AssetStocktakeRequest;
import com.company.daizhang.module.asset.service.AssetStocktakeService;
import com.company.daizhang.module.asset.vo.AssetStocktakeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 资产盘点管理控制器
 */
@Tag(name = "资产盘点管理")
@RestController
@RequestMapping("/asset/stocktake")
@RequiredArgsConstructor
public class AssetStocktakeController {

    private final AssetStocktakeService assetStocktakeService;

    @Operation(summary = "分页查询盘点单")
    @GetMapping("/page")
    public Result<PageResult<AssetStocktakeVO>> pageStocktakes(AssetStocktakeQueryRequest request) {
        PageResult<AssetStocktakeVO> page = assetStocktakeService.pageStocktakes(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询盘点单详情（含明细）")
    @GetMapping("/{id}")
    public Result<AssetStocktakeVO> getStocktakeById(@PathVariable Long id) {
        AssetStocktakeVO vo = assetStocktakeService.getStocktakeById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建盘点单（自动生成盘点明细，账面数据带入）")
    @PostMapping
    public Result<Long> createStocktake(@Valid @RequestBody AssetStocktakeRequest request) {
        Long id = assetStocktakeService.createStocktake(request);
        return Result.success(id);
    }

    @Operation(summary = "删除盘点单")
    @DeleteMapping("/{id}")
    public Result<Void> deleteStocktake(@PathVariable Long id) {
        assetStocktakeService.deleteStocktake(id);
        return Result.success();
    }

    @Operation(summary = "录入实盘数据（更新明细实盘数量/原值，自动计算差异和结果）")
    @PutMapping("/detail/{detailId}/actual")
    public Result<Void> inputActualData(@PathVariable Long detailId,
                                        @RequestParam(required = false) BigDecimal actualQuantity,
                                        @RequestParam(required = false) BigDecimal actualValue,
                                        @RequestParam(required = false) String handleOpinion) {
        assetStocktakeService.inputActualData(detailId, actualQuantity, actualValue, handleOpinion);
        return Result.success();
    }

    @Operation(summary = "完成盘点（汇总盘盈盘亏数量，更新盘点单状态为已完成）")
    @PutMapping("/{id}/complete")
    public Result<Void> completeStocktake(@PathVariable Long id) {
        assetStocktakeService.completeStocktake(id);
        return Result.success();
    }

    @Operation(summary = "生成盘点差异调账凭证（盘亏借待处理损溢贷固定资产；盘盈借固定资产贷待处理损溢）")
    @PostMapping("/{id}/voucher")
    public Result<Long> generateStocktakeVoucher(@PathVariable Long id) {
        Long voucherId = assetStocktakeService.generateStocktakeVoucher(id);
        return Result.success(voucherId);
    }
}
