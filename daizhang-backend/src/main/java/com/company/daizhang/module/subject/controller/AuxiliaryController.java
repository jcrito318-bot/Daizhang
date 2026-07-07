package com.company.daizhang.module.subject.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.subject.dto.AuxiliaryCategoryRequest;
import com.company.daizhang.module.subject.dto.AuxiliaryItemRequest;
import com.company.daizhang.module.subject.service.AuxiliaryService;
import com.company.daizhang.module.subject.vo.AuxiliaryCategoryVO;
import com.company.daizhang.module.subject.vo.AuxiliaryItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 辅助核算管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/auxiliary")
@RequiredArgsConstructor
@Tag(name = "辅助核算管理", description = "辅助核算类别与项目管理接口")
public class AuxiliaryController {

    private final AuxiliaryService auxiliaryService;

    // ==================== 类别管理 ====================

    @Operation(summary = "查询辅助核算类别列表")
    @GetMapping("/category/list")
    @RequireAccountSetAccess
    public Result<List<AuxiliaryCategoryVO>> listCategories(@RequestParam Long accountSetId) {
        List<AuxiliaryCategoryVO> list = auxiliaryService.listCategories(accountSetId);
        return Result.success(list);
    }

    @Operation(summary = "创建辅助核算类别")
    @PostMapping("/category")
    @RequireAccountSetAccess
    public Result<Void> createCategory(@Valid @RequestBody AuxiliaryCategoryRequest request) {
        auxiliaryService.createCategory(request);
        return Result.success();
    }

    @Operation(summary = "更新辅助核算类别")
    @PutMapping("/category/{id}")
    @RequireAccountSetAccess(required = false)
    public Result<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody AuxiliaryCategoryRequest request) {
        auxiliaryService.updateCategory(id, request);
        return Result.success();
    }

    @Operation(summary = "删除辅助核算类别")
    @DeleteMapping("/category/{id}")
    @RequireAccountSetAccess(required = false)
    public Result<Void> deleteCategory(@PathVariable Long id) {
        auxiliaryService.deleteCategory(id);
        return Result.success();
    }

    // ==================== 项目管理 ====================

    @Operation(summary = "分页查询辅助核算项目")
    @GetMapping("/item/page")
    @RequireAccountSetAccess
    public Result<PageResult<AuxiliaryItemVO>> pageItems(@RequestParam(required = false) Long accountSetId,
                                                         @RequestParam(required = false) Long categoryId,
                                                         @RequestParam(required = false) String itemName,
                                                         @RequestParam(defaultValue = "1") int pageNum,
                                                         @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<AuxiliaryItemVO> page = auxiliaryService.pageItems(accountSetId, categoryId, itemName, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "根据类别查询项目列表")
    @GetMapping("/item/list")
    public Result<List<AuxiliaryItemVO>> listItems(@RequestParam Long categoryId) {
        List<AuxiliaryItemVO> list = auxiliaryService.listItemsByCategory(categoryId);
        return Result.success(list);
    }

    @Operation(summary = "创建辅助核算项目")
    @PostMapping("/item")
    @RequireAccountSetAccess
    public Result<Void> createItem(@Valid @RequestBody AuxiliaryItemRequest request) {
        auxiliaryService.createItem(request);
        return Result.success();
    }

    @Operation(summary = "更新辅助核算项目")
    @PutMapping("/item/{id}")
    @RequireAccountSetAccess(required = false)
    public Result<Void> updateItem(@PathVariable Long id, @Valid @RequestBody AuxiliaryItemRequest request) {
        auxiliaryService.updateItem(id, request);
        return Result.success();
    }

    @Operation(summary = "删除辅助核算项目")
    @DeleteMapping("/item/{id}")
    @RequireAccountSetAccess(required = false)
    public Result<Void> deleteItem(@PathVariable Long id) {
        auxiliaryService.deleteItem(id);
        return Result.success();
    }
}
