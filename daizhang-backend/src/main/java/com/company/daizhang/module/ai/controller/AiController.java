package com.company.daizhang.module.ai.controller;

import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.ai.dto.AccountingSuggestRequest;
import com.company.daizhang.module.ai.dto.InvoiceRecognizeRequest;
import com.company.daizhang.module.ai.service.GlmAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

/**
 * AI功能控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI功能", description = "GLM大模型AI功能接口")
public class AiController {

    private final GlmAiService glmAiService;

    /**
     * 票据OCR识别
     */
    @PostMapping("/recognize/invoice")
    @Operation(summary = "票据OCR识别", description = "使用GLM大模型识别票据图片")
    public Result<String> recognizeInvoice(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "invoiceType", required = false) Integer invoiceType) {
        try {
            if (file.isEmpty()) {
                return Result.error(400, "文件不能为空");
            }

            String result = glmAiService.recognizeInvoice(file, invoiceType);
            return Result.success(result);
        } catch (IOException e) {
            log.error("票据识别失败", e);
            return Result.error(500, "票据识别失败：" + e.getMessage());
        }
    }

    /**
     * 票据OCR识别（Base64方式）
     */
    @PostMapping("/recognize/invoice/base64")
    @Operation(summary = "票据OCR识别（Base64）", description = "使用GLM大模型识别票据图片（Base64编码）")
    public Result<String> recognizeInvoiceBase64(@RequestBody InvoiceRecognizeRequest request) {
        try {
            if (request.getImageBase64() == null || request.getImageBase64().isEmpty()) {
                return Result.error(400, "图片数据不能为空");
            }

            // 将Base64转换为字节数组
            byte[] imageBytes = Base64.getDecoder().decode(request.getImageBase64());

            String result = glmAiService.recognizeInvoice(imageBytes, request.getInvoiceType());
            return Result.success(result);
        } catch (Exception e) {
            log.error("票据识别失败", e);
            return Result.error(500, "票据识别失败：" + e.getMessage());
        }
    }

    /**
     * 票据OCR识别（URL方式）
     */
    @PostMapping("/recognize/invoice/url")
    @Operation(summary = "票据OCR识别（URL）", description = "使用GLM大模型识别票据图片（通过URL）")
    public Result<String> recognizeInvoiceByUrl(@RequestBody java.util.Map<String, Object> request) {
        try {
            String fileUrl = (String) request.get("fileUrl");
            if (fileUrl == null || fileUrl.isEmpty()) {
                return Result.error(400, "文件URL不能为空");
            }

            // 获取票据类型（可选）
            Integer invoiceType = null;
            if (request.containsKey("invoiceType")) {
                invoiceType = (Integer) request.get("invoiceType");
            }

            // 从URL下载图片并识别
            byte[] imageBytes = glmAiService.downloadImageFromUrl(fileUrl);
            String result = glmAiService.recognizeInvoice(imageBytes, invoiceType);
            return Result.success(result);
        } catch (Exception e) {
            log.error("票据识别失败", e);
            return Result.error(500, "票据识别失败：" + e.getMessage());
        }
    }

    /**
     * 智能记账建议
     */
    @PostMapping("/suggest/accounting")
    @Operation(summary = "智能记账建议", description = "使用GLM大模型提供记账建议")
    public Result<String> suggestAccounting(@RequestBody AccountingSuggestRequest request) {
        try {
            if (request.getDescription() == null || request.getDescription().isEmpty()) {
                return Result.error(400, "业务描述不能为空");
            }

            if (request.getAmount() == null) {
                return Result.error(400, "金额不能为空");
            }

            String result = glmAiService.suggestAccounting(
                request.getDescription(),
                request.getAmount()
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("智能记账建议生成失败", e);
            return Result.error(500, "生成记账建议失败：" + e.getMessage());
        }
    }
}
