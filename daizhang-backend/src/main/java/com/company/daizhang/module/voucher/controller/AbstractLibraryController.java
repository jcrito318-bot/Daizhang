package com.company.daizhang.module.voucher.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.voucher.dto.AbstractLibraryQueryRequest;
import com.company.daizhang.module.voucher.dto.AbstractLibraryRequest;
import com.company.daizhang.module.voucher.service.AbstractLibraryService;
import com.company.daizhang.module.voucher.vo.AbstractLibraryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 常用摘要库管理控制器
 * <p>
 * 提供常用摘要的增删查及"使用次数 +1"能力。凭证录入页面通过 el-autocomplete
 * 调用 {@code /abstract/search} 模糊搜索摘要,按使用次数 DESC 排序返回。
 */
@Tag(name = "常用摘要库管理")
@RestController
@RequestMapping("/abstract")
@RequiredArgsConstructor
public class AbstractLibraryController {

    private final AbstractLibraryService abstractLibraryService;

    @Operation(summary = "分页查询常用摘要")
    @GetMapping("/page")
    public Result<PageResult<AbstractLibraryVO>> page(@Valid AbstractLibraryQueryRequest request) {
        PageResult<AbstractLibraryVO> page = abstractLibraryService.pageAbstracts(request);
        return Result.success(page);
    }

    @Operation(summary = "搜索常用摘要(按使用次数 DESC 排序,用于凭证录入 el-autocomplete)")
    @GetMapping("/search")
    @RequireAccountSetAccess(required = false)
    public Result<List<AbstractLibraryVO>> search(@RequestParam Long accountSetId,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(defaultValue = "10") Integer limit) {
        List<AbstractLibraryVO> list = abstractLibraryService.searchAbstracts(accountSetId, keyword, limit);
        return Result.success(list);
    }

    @Operation(summary = "新增常用摘要")
    @PostMapping
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Long> create(@Valid @RequestBody AbstractLibraryRequest request) {
        Long id = abstractLibraryService.createAbstract(request);
        return Result.success(id);
    }

    @Operation(summary = "摘要使用次数 +1(凭证保存时调用)")
    @PostMapping("/increment-use")
    public Result<Void> incrementUse(@RequestParam Long id) {
        abstractLibraryService.incrementUseCount(id);
        return Result.success();
    }

    @Operation(summary = "删除常用摘要")
    @DeleteMapping("/{id}")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> delete(@PathVariable Long id) {
        abstractLibraryService.deleteAbstract(id);
        return Result.success();
    }
}
