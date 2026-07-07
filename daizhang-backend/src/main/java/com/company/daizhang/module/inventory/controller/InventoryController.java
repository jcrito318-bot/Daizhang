package com.company.daizhang.module.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.annotation.OperationLog;
import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.inventory.dto.*;
import com.company.daizhang.module.inventory.entity.InventoryItem;
import com.company.daizhang.module.inventory.entity.InventoryIn;
import com.company.daizhang.module.inventory.entity.InventoryOut;
import com.company.daizhang.module.inventory.entity.InventoryStock;
import com.company.daizhang.module.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "库存管理")
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "商品列表（分页）")
    @GetMapping("/item/page")
    public Result<Page<InventoryItem>> itemPage(InventoryItemQueryRequest request) {
        return Result.success(inventoryService.getItemPage(request));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/item/{id}")
    public Result<InventoryItem> itemDetail(@PathVariable Long id) {
        return Result.success(inventoryService.getItemById(id));
    }

    @Operation(summary = "新增商品")
    @PostMapping("/item")
    @OperationLog("新增商品")
    @RequireAccountSetAccess(RequireAccountSetAccess.AccessLevel.OWNER)
    public Result<Long> createItem(@RequestBody InventoryItemCreateRequest request) {
        return Result.success(inventoryService.createItem(request));
    }

    @Operation(summary = "修改商品")
    @PutMapping("/item")
    @OperationLog("修改商品")
    public Result<Void> updateItem(@RequestBody InventoryItemUpdateRequest request) {
        inventoryService.updateItem(request);
        return Result.success();
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/item/{id}")
    @OperationLog("删除商品")
    public Result<Void> deleteItem(@PathVariable Long id) {
        inventoryService.deleteItem(id);
        return Result.success();
    }

    @Operation(summary = "库存余额（分页）")
    @GetMapping("/stock/page")
    public Result<Page<InventoryStock>> stockPage(InventoryStockQueryRequest request) {
        return Result.success(inventoryService.getStockPage(request));
    }

    @Operation(summary = "库存余额列表")
    @GetMapping("/stock/list")
    public Result<List<InventoryStock>> stockList(InventoryStockQueryRequest request) {
        return Result.success(inventoryService.getStockList(request));
    }

    @Operation(summary = "入库单列表（分页）")
    @GetMapping("/in/page")
    public Result<Page<InventoryIn>> inPage(InventoryInQueryRequest request) {
        return Result.success(inventoryService.getInPage(request));
    }

    @Operation(summary = "入库单详情")
    @GetMapping("/in/{id}")
    public Result<InventoryIn> inDetail(@PathVariable Long id) {
        return Result.success(inventoryService.getInById(id));
    }

    @Operation(summary = "新增入库单")
    @PostMapping("/in")
    @OperationLog("新增入库单")
    @RequireAccountSetAccess(RequireAccountSetAccess.AccessLevel.OWNER)
    public Result<Long> createIn(@RequestBody InventoryInCreateRequest request) {
        return Result.success(inventoryService.createIn(request));
    }

    @Operation(summary = "修改入库单")
    @PutMapping("/in")
    @OperationLog("修改入库单")
    public Result<Void> updateIn(@RequestBody InventoryInUpdateRequest request) {
        inventoryService.updateIn(request);
        return Result.success();
    }

    @Operation(summary = "删除入库单")
    @DeleteMapping("/in/{id}")
    @OperationLog("删除入库单")
    public Result<Void> deleteIn(@PathVariable Long id) {
        inventoryService.deleteIn(id);
        return Result.success();
    }

    @Operation(summary = "审核入库单")
    @PostMapping("/in/audit/{id}")
    @OperationLog("审核入库单")
    public Result<Void> auditIn(@PathVariable Long id) {
        inventoryService.auditIn(id);
        return Result.success();
    }

    @Operation(summary = "出库单列表（分页）")
    @GetMapping("/out/page")
    public Result<Page<InventoryOut>> outPage(InventoryOutQueryRequest request) {
        return Result.success(inventoryService.getOutPage(request));
    }

    @Operation(summary = "出库单详情")
    @GetMapping("/out/{id}")
    public Result<InventoryOut> outDetail(@PathVariable Long id) {
        return Result.success(inventoryService.getOutById(id));
    }

    @Operation(summary = "新增出库单")
    @PostMapping("/out")
    @OperationLog("新增出库单")
    @RequireAccountSetAccess(RequireAccountSetAccess.AccessLevel.OWNER)
    public Result<Long> createOut(@RequestBody InventoryOutCreateRequest request) {
        return Result.success(inventoryService.createOut(request));
    }

    @Operation(summary = "修改出库单")
    @PutMapping("/out")
    @OperationLog("修改出库单")
    public Result<Void> updateOut(@RequestBody InventoryOutUpdateRequest request) {
        inventoryService.updateOut(request);
        return Result.success();
    }

    @Operation(summary = "删除出库单")
    @DeleteMapping("/out/{id}")
    @OperationLog("删除出库单")
    public Result<Void> deleteOut(@PathVariable Long id) {
        inventoryService.deleteOut(id);
        return Result.success();
    }

    @Operation(summary = "审核出库单")
    @PostMapping("/out/audit/{id}")
    @OperationLog("审核出库单")
    public Result<Void> auditOut(@PathVariable Long id) {
        inventoryService.auditOut(id);
        return Result.success();
    }
}
