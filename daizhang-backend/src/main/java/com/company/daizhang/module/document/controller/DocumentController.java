package com.company.daizhang.module.document.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.common.utils.FileValidationUtil;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 票据管理控制器
 */
@Tag(name = "票据管理")
@RestController
@RequestMapping("/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @Value("${file.upload-path:./data/uploads/}")
    private String uploadPath;

    @Operation(summary = "分页查询票据")
    @GetMapping("/page")
    public Result<PageResult<DocumentVO>> page(@Valid DocumentQueryRequest request) {
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
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> create(@Valid @RequestBody DocumentCreateRequest request) {
        documentService.createDocument(request);
        return Result.success();
    }

    @Operation(summary = "更新票据")
    @PutMapping("/{id}")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DocumentUpdateRequest request) {
        documentService.updateDocument(id, request);
        return Result.success();
    }

    @Operation(summary = "删除票据")
    @DeleteMapping("/{id}")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> delete(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return Result.success();
    }

    @Operation(summary = "关联凭证")
    @PostMapping("/{id}/link-voucher/{voucherId}")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> linkVoucher(@PathVariable Long id, @PathVariable Long voucherId) {
        documentService.linkVoucher(id, voucherId);
        return Result.success();
    }

    @Operation(summary = "取消关联凭证")
    @PostMapping("/{id}/unlink-voucher")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> unlinkVoucher(@PathVariable Long id) {
        documentService.unlinkVoucher(id);
        return Result.success();
    }

    @Operation(summary = "获取票据台账")
    @GetMapping("/ledger")
    @RequireAccountSetAccess
    public Result<DocumentLedgerVO> ledger(@RequestParam Long accountSetId,
                                            @RequestParam Integer year) {
        DocumentLedgerVO ledger = documentService.getDocumentLedger(accountSetId, year);
        return Result.success(ledger);
    }

    @Operation(summary = "归档票据")
    @PostMapping("/archive")
    @RequireAccountSetAccess
    public Result<Void> archive(@RequestParam Long accountSetId,
                                 @RequestParam Integer year,
                                 @RequestParam Integer month) {
        documentService.archiveDocuments(accountSetId, year, month);
        return Result.success();
    }

    /**
     * 票据附件上传:不绑定具体票据,仅返回文件 URL,由前端在创建票据时回填 fileUrl 字段。
     * 复用 FileValidationUtil 的安全校验(扩展名白名单 + Magic Number + 防路径穿越 + 大小限制)。
     *
     * 安全修复:
     * 1. 路径拼接:使用 java.nio.file.Paths 安全拼接,避免 uploadPath 末尾缺失分隔符导致路径错误。
     * 2. 信息泄露:不再返回 destFile.getAbsolutePath(),改为返回相对路径(/uploads/document/xxx.pdf),
     *    防止攻击者通过 fileUrl 探测服务器绝对路径(目录结构、用户名等敏感信息)。
     */
    @Operation(summary = "上传票据附件")
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        FileValidationUtil.validate(file);

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;

        // 安全修复(BUG-后端):用 Paths.get 安全拼接路径,避免 uploadPath 末尾无分隔符时
        // 拼成 "./data/uploadsdocument/" 这种错误目录(原 uploadPath + "document/" 实现的缺陷)。
        java.nio.file.Path uploadDir = java.nio.file.Paths.get(uploadPath, "document");
        File dir = uploadDir.toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new com.company.daizhang.common.exception.BusinessException("创建上传目录失败：" + dir.getPath());
        }
        File destFile = new File(dir, newFileName);
        try {
            file.transferTo(destFile.getAbsoluteFile());
        } catch (IOException e) {
            throw new com.company.daizhang.common.exception.BusinessException("上传文件失败：" + e.getMessage());
        }
        Map<String, String> result = new HashMap<>();
        result.put("fileName", originalFilename);
        // 安全修复(BUG-后端):fileUrl 不再返回 destFile.getAbsolutePath(),
        // 改为返回相对路径 "/uploads/document/{filename}",前端通过此相对 URL 访问文件,
        // 避免泄露服务器绝对路径(目录结构、用户名等)。
        // 注:前端创建票据时直接保存此相对路径作为 fileUrl 字段值。
        result.put("fileUrl", "/uploads/document/" + newFileName);
        result.put("fileSize", String.valueOf(file.getSize()));
        return Result.success(result);
    }
}
