package com.company.daizhang.module.voucher.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.annotation.SensitiveOperation;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.common.vo.ImportResultVO;
import com.company.daizhang.module.voucher.dto.VoucherCreateRequest;
import com.company.daizhang.module.voucher.dto.VoucherQueryRequest;
import com.company.daizhang.module.voucher.dto.VoucherUpdateRequest;
import com.company.daizhang.module.voucher.service.VoucherImportService;
import com.company.daizhang.module.voucher.service.VoucherPrintService;
import com.company.daizhang.module.voucher.service.VoucherService;
import com.company.daizhang.module.voucher.vo.VoucherVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 凭证管理控制器
 */
@Slf4j
@Tag(name = "凭证管理")
@RestController
@RequestMapping("/voucher")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;
    private final VoucherImportService voucherImportService;
    private final VoucherPrintService voucherPrintService;

    @Operation(summary = "分页查询凭证")
    @GetMapping("/page")
    public Result<PageResult<VoucherVO>> page(@Valid VoucherQueryRequest request) {
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
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Long> create(@Valid @RequestBody VoucherCreateRequest request) {
        Long id = voucherService.createVoucher(request);
        return Result.success(id);
    }

    @Operation(summary = "更新凭证")
    @PutMapping("/{id}")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody VoucherUpdateRequest request) {
        voucherService.updateVoucher(id, request);
        return Result.success();
    }

    @Operation(summary = "删除凭证")
    @DeleteMapping("/{id}")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> delete(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return Result.success();
    }

    @Operation(summary = "审核凭证")
    @PostMapping("/{id}/audit")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> audit(@PathVariable Long id) {
        voucherService.auditVoucher(id);
        return Result.success();
    }

    @Operation(summary = "反审核凭证")
    @PostMapping("/{id}/unaudit")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> unaudit(@PathVariable Long id) {
        voucherService.unauditVoucher(id);
        return Result.success();
    }

    @Operation(summary = "批量审核凭证")
    @PostMapping("/batch-audit")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    @SensitiveOperation("批量审核凭证")
    public Result<Integer> batchAudit(@RequestBody java.util.List<Long> ids) {
        int success = voucherService.batchAuditVoucher(ids);
        return Result.success(success);
    }

    @Operation(summary = "批量反审核凭证")
    @PostMapping("/batch-unaudit")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Integer> batchUnaudit(@RequestBody java.util.List<Long> ids) {
        int success = voucherService.batchUnauditVoucher(ids);
        return Result.success(success);
    }

    @Operation(summary = "过账凭证")
    @PostMapping("/{id}/post")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> post(@PathVariable Long id) {
        voucherService.postVoucher(id);
        return Result.success();
    }

    @Operation(summary = "反过账凭证")
    @PostMapping("/{id}/unpost")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> unpost(@PathVariable Long id) {
        voucherService.unpostVoucher(id);
        return Result.success();
    }

    @Operation(summary = "作废凭证")
    @PostMapping("/{id}/cancel")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> cancel(@PathVariable Long id) {
        voucherService.cancelVoucher(id);
        return Result.success();
    }

    @Operation(summary = "复制凭证")
    @PostMapping("/{id}/copy")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Long> copy(@PathVariable Long id) {
        Long newId = voucherService.copyVoucher(id);
        return Result.success(newId);
    }

    @Operation(summary = "红冲凭证")
    @PostMapping("/{id}/reverse")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Long> reverse(@PathVariable Long id,
                                @RequestParam(required = false) Integer targetYear,
                                @RequestParam(required = false) Integer targetMonth) {
        Long newId = voucherService.reverseVoucher(id, targetYear, targetMonth);
        return Result.success(newId);
    }

    @Operation(summary = "保存凭证草稿")
    @PostMapping("/draft")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Long> saveDraft(@Valid @RequestBody VoucherCreateRequest request) {
        Long id = voucherService.saveDraft(request);
        return Result.success(id);
    }

    @Operation(summary = "提交凭证草稿")
    @PostMapping("/{id}/submit-draft")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> submitDraft(@PathVariable Long id) {
        voucherService.submitDraft(id);
        return Result.success();
    }

    @Operation(summary = "凭证整理（断号重编）")
    @PostMapping("/rearrange")
    @RequireAccountSetAccess
    public Result<Void> rearrange(@RequestParam Long accountSetId,
                                  @RequestParam Integer year,
                                  @RequestParam Integer month) {
        voucherService.rearrangeVoucherNo(accountSetId, year, month);
        return Result.success();
    }

    @Operation(summary = "批量导入凭证")
    @PostMapping("/import")
    @RequireAccountSetAccess
    public Result<ImportResultVO> importVouchers(@RequestParam Long accountSetId,
                                                  @RequestParam("file") MultipartFile file) {
        ImportResultVO result = voucherImportService.importVouchers(accountSetId, file);
        return Result.success(result);
    }

    @Operation(summary = "下载凭证导入模板")
    @GetMapping("/import/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        byte[] data = voucherImportService.downloadTemplate();
        writeExcelResponse(response, data, "凭证导入模板.xlsx");
    }

    // ==================== 凭证打印与导出（标准会计凭证格式）====================

    @Operation(summary = "凭证打印预览HTML（标准会计凭证格式）")
    @GetMapping("/{id}/print-html")
    public void printHtml(@PathVariable Long id, HttpServletResponse response) throws IOException {
        String html = voucherPrintService.generatePrintHtml(id);
        response.setContentType("text/html;charset=UTF-8");
        try (OutputStream os = response.getOutputStream()) {
            os.write(html.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    @Operation(summary = "批量凭证打印预览HTML")
    @GetMapping("/print-html-batch")
    public void printHtmlBatch(@RequestParam List<Long> ids, HttpServletResponse response) throws IOException {
        String html = voucherPrintService.generatePrintHtmlBatch(ids);
        response.setContentType("text/html;charset=UTF-8");
        try (OutputStream os = response.getOutputStream()) {
            os.write(html.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    @Operation(summary = "导出凭证为PDF（标准会计凭证格式）")
    @GetMapping("/{id}/export-pdf")
    public void exportPdf(@PathVariable Long id, HttpServletResponse response) throws IOException {
        byte[] data = voucherPrintService.exportPdf(id);
        writePdfResponse(response, data, "凭证_" + id + ".pdf");
    }

    @Operation(summary = "批量导出凭证为PDF")
    @GetMapping("/export-pdf-batch")
    public void exportPdfBatch(@RequestParam List<Long> ids, HttpServletResponse response) throws IOException {
        byte[] data = voucherPrintService.exportPdfBatch(ids);
        writePdfResponse(response, data, "凭证.pdf");
    }

    @Operation(summary = "导出凭证为Excel（标准会计凭证格式）")
    @GetMapping("/export-excel")
    public void exportExcel(@RequestParam List<Long> ids, HttpServletResponse response) throws IOException {
        byte[] data = voucherPrintService.exportExcel(ids);
        writeExcelResponse(response, data, "会计凭证.xlsx");
    }

    /**
     * 输出Excel文件到响应
     */
    private void writeExcelResponse(HttpServletResponse response, byte[] data, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName);
        try (OutputStream os = response.getOutputStream()) {
            os.write(data);
            os.flush();
        }
    }

    /**
     * 输出PDF文件到响应
     */
    private void writePdfResponse(HttpServletResponse response, byte[] data, String fileName) throws IOException {
        response.setContentType("application/pdf");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName);
        try (OutputStream os = response.getOutputStream()) {
            os.write(data);
            os.flush();
        }
    }
}
