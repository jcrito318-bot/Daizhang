package com.company.daizhang.module.ai.service;

import com.company.daizhang.module.ai.config.AiConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * GLM AI服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlmAiService {

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * 票据OCR识别
     * @param imageFile 票据图片文件
     * @return 识别结果（JSON格式）
     */
    public String recognizeInvoice(MultipartFile imageFile) throws IOException {
        return recognizeInvoice(imageFile.getBytes(), null);
    }

    /**
     * 票据OCR识别（带票据类型）
     * @param imageFile 票据图片文件
     * @param invoiceType 票据类型 1-增值税发票 2-普通发票 3-银行回单 4-其他
     * @return 识别结果（JSON格式）
     */
    public String recognizeInvoice(MultipartFile imageFile, Integer invoiceType) throws IOException {
        return recognizeInvoice(imageFile.getBytes(), invoiceType);
    }

    /**
     * 票据OCR识别（字节数组方式，带票据类型）
     * @param imageBytes 票据图片字节数组
     * @param invoiceType 票据类型 1-增值税发票 2-普通发票 3-银行回单 4-其他
     * @return 识别结果（JSON格式）
     */
    public String recognizeInvoice(byte[] imageBytes, Integer invoiceType) throws IOException {
        if (!aiConfig.getEnabled()) {
            throw new RuntimeException("AI功能未启用");
        }

        // 将图片转换为Base64
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // 构建GLM API请求（根据票据类型使用不同的提示词）
        Map<String, Object> request = buildOcrRequest(base64Image, invoiceType);

        // 调用GLM API
        String response = callGlmApi(request, aiConfig.getOcrModel());

        log.info("票据识别完成，票据类型：{}", invoiceType == null ? "自动识别" : getInvoiceTypeName(invoiceType));
        return response;
    }

    /**
     * 票据OCR识别并返回结构化JSON结果
     * 自动识别票据类型，并清理返回结果中的非JSON内容（如markdown代码块标记）
     * @param file 票据图片文件
     * @return 结构化JSON字符串
     */
    public String recognizeInvoiceAndParseResult(MultipartFile file) throws IOException {
        String result = recognizeInvoice(file, null);
        return cleanJsonResult(result);
    }

    /**
     * 清理GLM返回结果中的非JSON内容（如markdown代码块标记）
     */
    private String cleanJsonResult(String result) {
        if (result == null || result.isEmpty()) {
            return result;
        }
        String trimmed = result.trim();
        // 去除markdown代码块标记 ```json ... ``` 或 ``` ... ```
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            trimmed = trimmed.trim();
        }
        return trimmed;
    }

    /**
     * 获取票据类型名称
     */
    private String getInvoiceTypeName(Integer invoiceType) {
        if (invoiceType == null) return "自动识别";
        return switch (invoiceType) {
            case 1 -> "增值税发票";
            case 2 -> "普通发票";
            case 3 -> "银行回单";
            case 4 -> "其他票据";
            default -> "未知类型";
        };
    }

    /**
     * 从URL下载图片
     * @param imageUrl 图片URL
     * @return 图片字节数组
     */
    public byte[] downloadImageFromUrl(String imageUrl) throws IOException {
        // SSRF防护:校验协议白名单 + 禁止访问内网/保留地址段
        // 原实现直接用用户输入URL发起请求,可探测内网/读取云元数据(169.254.169.254)
        validateImageUrl(imageUrl);

        Request request = new Request.Builder()
                .url(imageUrl)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载图片失败，状态码：" + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("下载图片失败，响应体为空");
            }
            // 限制响应体大小(20MB),防止用SSRF做盲打放大攻击
            long contentLength = body.contentLength();
            if (contentLength > 20L * 1024 * 1024) {
                throw new IOException("图片体积超过限制(20MB)");
            }
            return body.bytes();
        }
    }

    /**
     * 校验图片URL,防止SSRF
     * - 仅允许http/https协议
     * - 拒绝内网/保留/环回/链路本地地址
     */
    private void validateImageUrl(String imageUrl) throws IOException {
        URL url;
        try {
            url = new URL(imageUrl);
        } catch (java.net.MalformedURLException e) {
            throw new IOException("URL格式不合法: " + e.getMessage());
        }
        String protocol = url.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            throw new IOException("仅支持http/https协议,拒绝: " + protocol);
        }
        String host = url.getHost();
        if (host == null || host.isEmpty()) {
            throw new IOException("URL缺少主机名");
        }
        // 解析所有IP并逐一校验,防止多IP中混入内网地址
        InetAddress[] addrs = InetAddress.getAllByName(host);
        for (InetAddress addr : addrs) {
            if (addr.isAnyLocalAddress()
                    || addr.isLoopbackAddress()
                    || addr.isSiteLocalAddress()
                    || addr.isLinkLocalAddress()
                    || addr.isMulticastAddress()) {
                throw new IOException("禁止访问内网/保留地址: " + addr.getHostAddress());
            }
            // 显式拦截云元数据服务地址(169.254.169.254等链路本地,部分JVM实现未归入isLinkLocalAddress)
            String ip = addr.getHostAddress();
            if (ip.startsWith("169.254.") || ip.startsWith("0.")) {
                throw new IOException("禁止访问保留地址段: " + ip);
            }
        }
    }

    /**
     * 智能记账建议
     * @param description 业务描述
     * @param amount 金额
     * @return 记账建议（包含科目、借贷方向等）
     */
    public String suggestAccounting(String description, Double amount) {
        if (!aiConfig.getEnabled()) {
            throw new RuntimeException("AI功能未启用");
        }

        // 构建GLM API请求
        Map<String, Object> request = buildAccountingRequest(description, amount);

        // 调用GLM API
        String response = callGlmApi(request, aiConfig.getChatModel());

        log.info("智能记账建议生成完成");
        return response;
    }

    /**
     * 将图片转换为Base64
     */
    private String convertToBase64(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 构建OCR识别请求（自动识别票据类型）
     */
    private Map<String, Object> buildOcrRequest(String base64Image, Integer invoiceType) {
        Map<String, Object> request = new HashMap<>();

        // 构建消息
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");

        // 构建内容（包含文本和图片）
        Map<String, Object>[] content = new Map[2];

        // 根据票据类型生成不同的提示词
        String prompt = buildOcrPrompt(invoiceType);
        
        // 文本提示
        content[0] = new HashMap<>();
        content[0].put("type", "text");
        content[0].put("text", prompt);

        // 图片
        content[1] = new HashMap<>();
        content[1].put("type", "image_url");
        Map<String, String> imageUrl = new HashMap<>();
        imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
        content[1].put("image_url", imageUrl);

        message.put("content", content);

        request.put("model", aiConfig.getOcrModel());
        request.put("messages", new Map[]{message});

        return request;
    }

    /**
     * 根据票据类型构建OCR提示词
     */
    private String buildOcrPrompt(Integer invoiceType) {
        if (invoiceType == null) {
            // 自动识别：通用提示词
            return "请识别这张票据图片中的信息，包括：票据类型、发票号码、发票代码、开票日期、购买方名称、销售方名称、金额、税额、价税合计等关键信息。请以JSON格式返回。";
        }
        
        return switch (invoiceType) {
            case 1 -> // 增值税发票
                "请识别这张增值税发票图片中的信息，包括：发票代码、发票号码、开票日期、购买方名称、购买方纳税人识别号、销售方名称、销售方纳税人识别号、金额、税率、税额、价税合计等关键信息。请以JSON格式返回。";
            case 2 -> // 普通发票
                "请识别这张普通发票图片中的信息，包括：发票代码、发票号码、开票日期、付款方名称、收款方名称、金额、税额、价税合计等关键信息。请以JSON格式返回。";
            case 3 -> // 银行回单
                "请识别这张银行回单图片中的信息，包括：交易日期、付款人名称、付款人账号、收款人名称、收款人账号、交易金额、交易类型、摘要、流水号等关键信息。请以JSON格式返回。";
            case 4 -> // 其他票据
                "请识别这张票据图片中的信息，包括：票据类型、票据号码、日期、相关方名称、金额、税额等关键信息。请以JSON格式返回。";
            default ->
                "请识别这张票据图片中的信息，包括：票据类型、发票号码、发票代码、开票日期、购买方名称、销售方名称、金额、税额、价税合计等关键信息。请以JSON格式返回。";
        };
    }

    /**
     * 构建智能记账请求
     */
    private Map<String, Object> buildAccountingRequest(String description, Double amount) {
        Map<String, Object> request = new HashMap<>();

        // 构建消息
        Map<String, Object>[] messages = new Map[2];

        // 系统消息
        messages[0] = new HashMap<>();
        messages[0].put("role", "system");
        messages[0].put("content", "你是一个专业的会计助手。根据用户提供的业务描述和金额，提供记账建议，包括：借方科目、贷方科目、金额、摘要。请以JSON格式返回。");

        // 用户消息
        messages[1] = new HashMap<>();
        messages[1].put("role", "user");
        messages[1].put("content", String.format("业务描述：%s，金额：%.2f元", description, amount));

        request.put("model", aiConfig.getChatModel());
        request.put("messages", messages);

        return request;
    }

    /**
     * 调用GLM API
     */
    private String callGlmApi(Map<String, Object> request, String model) {
        try {
            String url = aiConfig.getBaseUrl() + "/chat/completions";
            String requestBody = objectMapper.writeValueAsString(request);

            RequestBody body = RequestBody.create(
                    requestBody,
                    MediaType.parse("application/json")
            );

            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + aiConfig.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            log.info("调用GLM API，模型：{}，URL：{}", model, url);

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("GLM API调用失败，状态码：{}，错误信息：{}", response.code(), errorBody);
                    throw new RuntimeException("GLM API调用失败：" + response.code() + " - " + errorBody);
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("GLM API响应成功");

                // 解析响应，提取content
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                Object[] choices = (Object[]) responseMap.get("choices");
                if (choices != null && choices.length > 0) {
                    Map<String, Object> firstChoice = (Map<String, Object>) choices[0];
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }

                return responseBody;
            }
        } catch (IOException e) {
            log.error("GLM API调用异常", e);
            throw new RuntimeException("GLM API调用异常：" + e.getMessage(), e);
        }
    }
}
