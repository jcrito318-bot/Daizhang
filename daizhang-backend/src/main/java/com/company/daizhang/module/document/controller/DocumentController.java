package com.company.daizhang.module.document.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.document.dto.DocumentCreateRequest;
import com.company.daizhang.module.document.dto.DocumentQueryRequest;
import com.company.daizhang.module.document.dto.DocumentUpdateRequest;
import com.company.daizhang.module.document.service.DocumentService;
import com.company.daizhang.module.document.vo.DocumentLedgerVO;
import com.company.daizhang.module.document.vo.DocumentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 票据管理控制器
 */
@Tag(name = "票据管理")
@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "分页查询票据")
    @GetMapping("/page")
    public Result<PageResult<DocumentVO>> page(DocumentQueryRequest request) {
        PageResult<DocumentVO> page = documentService.pageDocuments(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询票据")
    @GetMapping("/{id}")
    public Result<DocumentVO> getById(@PathVariable Long id) {
        DocumentVO document = documentService.getDocumentById(id);
        return Result.success(document);
    }

    @Operation(summary = "创建票据")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody DocumentCreateRequest request) {
        documentService.createDocument(request);
        return Result.success();
    }

    @Operation(summary = "更新票据")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DocumentUpdateRequest request) {
        documentService.updateDocument(id, request);
        return Result.success();
    }

    @Operation(summary = "删除票据")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return Result.success();
    }

    @Operation(summary = "关联凭证")
    @PostMapping("/{id}/link-voucher/{voucherId}")
    public Result<Void> linkVoucher(@PathVariable Long id, @PathVariable Long voucherId) {
        documentService.linkVoucher(id, voucherId);
        return Result.success();
    }

    @Operation(summary = "取消关联凭证")
    @PostMapping("/{id}/unlink-voucher")
    public Result<Void> unlinkVoucher(@PathVariable Long id) {
        documentService.unlinkVoucher(id);
        return Result.success();
    }

    @Operation(summary = "获取票据台账")
    @GetMapping("/ledger")
    public Result<DocumentLedgerVO> ledger(@RequestParam Long accountSetId,
                                            @RequestParam Integer year) {
        DocumentLedgerVO ledger = documentService.getDocumentLedger(accountSetId, year);
        return Result.success(ledger);
    }

    @Operation(summary = "归档票据")
    @PostMapping("/archive")
    public Result<Void> archive(@RequestParam Long accountSetId,
                                 @RequestParam Integer year,
                                 @RequestParam Integer month) {
        documentService.archiveDocuments(accountSetId, year, month);
        return Result.success();
    }
}
