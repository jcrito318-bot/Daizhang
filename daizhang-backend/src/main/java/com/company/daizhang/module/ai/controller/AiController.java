package com.company.daizhang.module.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.ai.dto.AccountingSuggestRequest;
import com.company.daizhang.module.ai.dto.InvoiceRecognizeRequest;
import com.company.daizhang.module.ai.service.GlmAiService;
import com.company.daizhang.module.document.dto.InputInvoiceRequest;
import com.company.daizhang.module.document.entity.Document;
import com.company.daizhang.module.document.service.DocumentService;
import com.company.daizhang.module.document.service.InvoiceService;
import com.company.daizhang.module.document.vo.DocumentVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    private final DocumentService documentService;
    private final InvoiceService invoiceService;
    private final ObjectMapper objectMapper;
    private final AccountSetAccessService accountSetAccessService;

    /**
     * 票据OCR识别
     */
    @PostMapping("/recognize/invoice")
    @Operation(summary = "票据OCR识别", description = "使用GLM大模型识别票据图片")
    public Result<String> recognizeInvoice(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "invoiceType", required = false) Integer invoiceType) {
        // OCR为通用功能不绑定特定账套，但要求当前用户至少拥有任意账套访问权，避免无账套用户滥用AI算力
        checkAiAccess();
        try {
            if (file.isEmpty()) {
                return Result.error(400, "文件不能为空");
            }

            String result = glmAiService.recognizeInvoice(file, invoiceType);
            return Result.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("票据识别失败", e);
            return Result.error(500, "票据识别失败，请检查文件格式或稍后重试");
        }
    }

    /**
     * 票据OCR识别（Base64方式）
     */
    @PostMapping("/recognize/invoice/base64")
    @Operation(summary = "票据OCR识别（Base64）", description = "使用GLM大模型识别票据图片（Base64编码）")
    public Result<String> recognizeInvoiceBase64(@Valid @RequestBody InvoiceRecognizeRequest request) {
        // OCR为通用功能不绑定特定账套，但要求当前用户至少拥有任意账套访问权，避免无账套用户滥用AI算力
        checkAiAccess();
        try {
            if (request.getImageBase64() == null || request.getImageBase64().isEmpty()) {
                return Result.error(400, "图片数据不能为空");
            }

            // 将Base64转换为字节数组
            byte[] imageBytes = Base64.getDecoder().decode(request.getImageBase64());

            String result = glmAiService.recognizeInvoice(imageBytes, request.getInvoiceType());
            return Result.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("票据识别失败", e);
            return Result.error(500, "票据识别失败，请检查图片格式或稍后重试");
        }
    }

    /**
     * 票据OCR识别（URL方式）
     */
    @PostMapping("/recognize/invoice/url")
    @Operation(summary = "票据OCR识别（URL）", description = "使用GLM大模型识别票据图片（通过URL）")
    public Result<String> recognizeInvoiceByUrl(@RequestBody java.util.Map<String, Object> request) {
        // OCR为通用功能不绑定特定账套，但要求当前用户至少拥有任意账套访问权，避免无账套用户滥用AI算力
        checkAiAccess();
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
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("票据识别失败", e);
            return Result.error(500, "票据识别失败，请检查URL或稍后重试");
        }
    }

    /**
     * OCR识别历史
     * OCR识别结果保存在document模块的票据表中(ocr_content字段不为空即为OCR来源)
     */
    @GetMapping("/ocr/history")
    @Operation(summary = "OCR识别历史", description = "分页查询OCR识别产生的票据记录(ocr_content非空)")
    @RequireAccountSetAccess
    public Result<PageResult<Document>> ocrHistory(@RequestParam Long accountSetId,
                                                     @RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "10") int pageSize) {
        Page<Document> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<Document>()
                .eq(Document::getAccountSetId, accountSetId)
                .isNotNull(Document::getOcrContent)
                .orderByDesc(Document::getCreateTime);
        Page<Document> result = documentService.page(page, wrapper);
        return Result.success(new PageResult<>(result.getRecords(), result.getTotal(), pageNum, pageSize));
    }

    /**
     * 智能记账建议
     */
    @PostMapping("/suggest/accounting")
    @Operation(summary = "智能记账建议", description = "使用GLM大模型提供记账建议")
    public Result<String> suggestAccounting(@Valid @RequestBody AccountingSuggestRequest request) {
        // AI为通用功能不绑定特定账套,但要求当前用户至少拥有任意账套访问权,避免无账套用户滥用AI算力
        checkAiAccess();
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

    /**
     * AI财税问答
     */
    @PostMapping("/chat")
    @Operation(summary = "AI财税问答", description = "使用GLM大模型回答财税相关问题")
    public Result<String> chat(@RequestBody Map<String, String> request) {
        // AI为通用功能不绑定特定账套,但要求当前用户至少拥有任意账套访问权,避免无账套用户滥用AI算力
        checkAiAccess();
        try {
            String question = request.get("question");
            if (question == null || question.trim().isEmpty()) {
                return Result.error(400, "问题不能为空");
            }

            String result = glmAiService.chat(question);
            return Result.success(result);
        } catch (Exception e) {
            log.error("AI财税问答失败", e);
            return Result.error(500, "AI财税问答失败：" + e.getMessage());
        }
    }

    /**
     * 票据OCR识别并保存
     * 识别票据图片后，自动创建票据记录和进项发票记录
     */
    @PostMapping("/recognize/invoice-and-save")
    @Operation(summary = "票据OCR识别并保存", description = "识别票据图片后自动创建票据(Document)和进项发票(InputInvoice)记录")
    @RequireAccountSetAccess
    public Result<DocumentVO> recognizeInvoiceAndSave(@RequestParam Long accountSetId,
                                                      @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error(400, "文件不能为空");
            }

            // 1. 调用AI识别票据
            String ocrResult = glmAiService.recognizeInvoiceAndParseResult(file);
            log.info("票据OCR识别完成，accountSetId={}，结果：{}", accountSetId, ocrResult);

            // 2. 解析识别结果
            Map<String, Object> parseResult = parseOcrResult(ocrResult);

            // 3. 创建Document记录
            Document document = buildDocument(accountSetId, ocrResult, parseResult);
            documentService.save(document);
            log.info("票据记录创建成功，id={}，documentNo={}", document.getId(), document.getDocumentNo());

            // 4. 创建InputInvoice记录（如果识别到发票号码）
            createInputInvoiceIfPossible(accountSetId, parseResult);

            // 5. 返回DocumentVO
            DocumentVO documentVO = documentService.getDocumentById(document.getId());
            return Result.success(documentVO);
        } catch (Exception e) {
            log.error("票据识别并保存失败", e);
            return Result.error(500, "票据识别并保存失败：" + e.getMessage());
        }
    }

    /**
     * AI接口访问校验：OCR等通用AI功能不绑定特定账套，
     * 但要求当前用户至少拥有任意一个账套的访问权，避免无账套权限用户滥用AI算力。
     * fail-closed：普通用户无可访问账套时抛 FORBIDDEN；超级管理员(listAccessibleAccountSetIds 返回 null)放行。
     */
    private void checkAiAccess() {
        Set<Long> accessibleIds = accountSetAccessService.listAccessibleAccountSetIds();
        if (accessibleIds != null && accessibleIds.isEmpty()) {
            log.warn("AI接口访问拦截：当前用户无任何账套访问权限");
            throw new BusinessException(ErrorCode.FORBIDDEN, "无账套访问权限，无法使用AI功能");
        }
    }

    /**
     * 解析OCR识别结果为Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseOcrResult(String ocrResult) {
        try {
            Map<String, Object> result = objectMapper.readValue(ocrResult, Map.class);
            return result != null ? result : new HashMap<>();
        } catch (Exception e) {
            log.warn("解析OCR结果JSON失败，返回空Map", e);
            return new HashMap<>();
        }
    }

    /**
     * 构建Document实体
     */
    private Document buildDocument(Long accountSetId, String ocrResult, Map<String, Object> parseResult) {
        Document document = new Document();
        document.setAccountSetId(accountSetId);
        document.setDocumentNo(generateDocumentNo());
        document.setDocumentType(1); // 发票
        document.setDocumentDate(parseDate(parseResult));
        document.setAmount(parseAmount(parseResult, "amount", "金额", "不含税金额"));
        document.setTaxAmount(parseAmount(parseResult, "taxAmount", "税额"));
        document.setTotalAmount(parseAmount(parseResult, "totalAmount", "价税合计", "合计金额"));
        document.setSellerName(getStringField(parseResult, "sellerName", "销售方名称", "销方名称"));
        document.setBuyerName(getStringField(parseResult, "buyerName", "购买方名称", "购方名称"));
        document.setInvoiceCode(getStringField(parseResult, "invoiceCode", "发票代码"));
        document.setInvoiceNumber(getStringField(parseResult, "invoiceNumber", "发票号码", "发票号"));
        document.setOcrContent(ocrResult);
        document.setStatus(0); // 待处理
        return document;
    }

    /**
     * 创建进项发票记录（如果识别到发票号码）
     */
    private void createInputInvoiceIfPossible(Long accountSetId, Map<String, Object> parseResult) {
        String invoiceNumber = getStringField(parseResult, "invoiceNumber", "发票号码", "发票号");
        if (invoiceNumber == null || invoiceNumber.isEmpty()) {
            log.warn("OCR结果中未识别到发票号码，跳过创建进项发票");
            return;
        }

        try {
            InputInvoiceRequest request = new InputInvoiceRequest();
            request.setAccountSetId(accountSetId);
            request.setInvoiceNumber(invoiceNumber);
            request.setInvoiceCode(getStringField(parseResult, "invoiceCode", "发票代码"));
            request.setInvoiceDate(parseDate(parseResult));
            request.setInvoiceType(getStringField(parseResult, "invoiceType", "发票类型", "票据类型"));
            if (request.getInvoiceType() == null) {
                request.setInvoiceType("增值税普通发票");
            }
            request.setSellerName(getStringField(parseResult, "sellerName", "销售方名称", "销方名称"));
            request.setSellerTaxNumber(getStringField(parseResult, "sellerTaxNumber", "销售方税号", "销方税号", "销售方纳税人识别号"));
            request.setBuyerName(getStringField(parseResult, "buyerName", "购买方名称", "购方名称"));
            request.setBuyerTaxNumber(getStringField(parseResult, "buyerTaxNumber", "购买方税号", "购方税号", "购买方纳税人识别号"));
            request.setAmount(parseAmount(parseResult, "amount", "金额", "不含税金额"));
            request.setTaxAmount(parseAmount(parseResult, "taxAmount", "税额"));
            request.setTotalAmount(parseAmount(parseResult, "totalAmount", "价税合计", "合计金额"));
            request.setTaxRate(parseAmount(parseResult, "taxRate", "税率"));

            invoiceService.createInputInvoice(request);
            log.info("进项发票创建成功，发票号码：{}", invoiceNumber);
        } catch (Exception e) {
            log.warn("创建进项发票失败，发票号码：{}，原因：{}", invoiceNumber, e.getMessage());
        }
    }

    /**
     * 生成票据编号：OCR-yyyyMMdd-时间戳后6位
     */
    private String generateDocumentNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "OCR-" + dateStr + "-" + (System.currentTimeMillis() % 1000000);
    }

    /**
     * 从Map中获取字符串字段（支持多个可能的key）
     */
    private String getStringField(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !value.toString().trim().isEmpty()) {
                return value.toString().trim();
            }
        }
        return null;
    }

    /**
     * 从Map中解析金额字段（支持多个可能的key）
     */
    private BigDecimal parseAmount(Map<String, Object> map, String... keys) {
        String value = getStringField(map, keys);
        if (value == null || value.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            // 去除逗号等千分位分隔符
            return new BigDecimal(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            log.warn("金额解析失败：{}", value);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 从Map中解析日期字段
     */
    private LocalDate parseDate(Map<String, Object> map) {
        String value = getStringField(map, "invoiceDate", "开票日期", "日期", "documentDate", "票据日期");
        if (value == null || value.isEmpty()) {
            return LocalDate.now();
        }
        // 尝试多种日期格式
        String[] patterns = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyy年MM月dd日", "yyyyMMdd"};
        for (String pattern : patterns) {
            try {
                return LocalDate.parse(value.trim(), DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
            }
        }
        log.warn("日期解析失败：{}，使用当前日期", value);
        return LocalDate.now();
    }
}
