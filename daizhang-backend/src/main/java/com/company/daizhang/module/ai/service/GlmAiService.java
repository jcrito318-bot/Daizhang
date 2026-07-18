package com.company.daizhang.module.ai.service;

import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
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
import java.util.List;
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
            // 关闭重定向跟随:防止 SSRF 校验通过的 URL 通过 302 跳转到内网/云元数据服务
            // (如 https://attacker.com/img.jpg -> http://169.254.169.254/latest/meta-data/)
            .followRedirects(false)
            .followSslRedirects(false)
            .build();

    /**
     * 票据OCR识别
     * @param imageFile 票据图片文件
     * @return 识别结果（JSON格式）
     */
    public String recognizeInvoice(MultipartFile imageFile) throws IOException {
        return recognizeInvoice(imageFile.getBytes(), null, imageFile.getContentType());
    }

    /**
     * 票据OCR识别（带票据类型）
     * @param imageFile 票据图片文件
     * @param invoiceType 票据类型 1-增值税发票 2-普通发票 3-银行回单 4-其他
     * @return 识别结果（JSON格式）
     */
    public String recognizeInvoice(MultipartFile imageFile, Integer invoiceType) throws IOException {
        return recognizeInvoice(imageFile.getBytes(), invoiceType, imageFile.getContentType());
    }

    /**
     * 票据OCR识别（字节数组方式，带票据类型）
     * @param imageBytes 票据图片字节数组
     * @param invoiceType 票据类型 1-增值税发票 2-普通发票 3-银行回单 4-其他
     * @return 识别结果（JSON格式）
     */
    public String recognizeInvoice(byte[] imageBytes, Integer invoiceType) throws IOException {
        // 字节数组方式无法获取原始 MIME，按 jpeg 处理（GLM 视觉模型对常见图片格式兼容）
        return recognizeInvoice(imageBytes, invoiceType, "image/jpeg");
    }

    /**
     * 票据OCR识别（字节数组方式，带票据类型与 MIME）
     * @param imageBytes 票据图片字节数组
     * @param invoiceType 票据类型 1-增值税发票 2-普通发票 3-银行回单 4-其他
     * @param contentType 图片 MIME 类型(如 image/jpeg、image/png、image/webp、application/pdf)
     * @return 识别结果（JSON格式）
     */
    public String recognizeInvoice(byte[] imageBytes, Integer invoiceType, String contentType) throws IOException {
        if (!aiConfig.getEnabled()) {
            // 用业务异常替代 RuntimeException,便于 GlobalExceptionHandler 统一处理且不暴露堆栈
            throw new BusinessException(ErrorCode.AI_NOT_ENABLED, "AI功能未启用");
        }

        // 将图片转换为Base64
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // 规范化 MIME:取默认 jpeg,避免空值或非标准类型导致 data URL 构造异常
        String mime = (contentType == null || contentType.trim().isEmpty())
                ? "image/jpeg" : contentType.trim().toLowerCase();

        // 构建GLM API请求（根据票据类型使用不同的提示词）
        Map<String, Object> request = buildOcrRequest(base64Image, invoiceType, mime);

        // 调用GLM API
        String response = callGlmApi(request, aiConfig.getOcrModel());

        // 统一清理 GLM 返回结果中的 markdown 代码块包裹(如 ```json ... ```)
        // 这样所有 OCR 入口(/invoice、/base64、/url、/invoice-and-save)都得到纯净 JSON,前端无需重复处理
        String cleaned = cleanJsonResult(response);

        log.info("票据识别完成，票据类型：{}", invoiceType == null ? "自动识别" : getInvoiceTypeName(invoiceType));
        return cleaned;
    }

    /**
     * 票据OCR识别并返回结构化JSON结果
     * 自动识别票据类型，并清理返回结果中的非JSON内容（如markdown代码块标记）
     * @param file 票据图片文件
     * @return 结构化JSON字符串
     */
    public String recognizeInvoiceAndParseResult(MultipartFile file) throws IOException {
        return recognizeInvoice(file, null);
    }

    /**
     * 清理GLM返回结果中的非JSON内容（如markdown代码块标记、引导语等）
     * 支持以下场景:
     * 1. 纯代码块: "```json\n{...}\n```"
     * 2. 代码块带引导语: "识别结果如下：\n```json\n{...}\n```\n"
     * 3. 纯JSON: "{...}"
     * 4. JSON带文字: "结果：{...}"
     */
    private String cleanJsonResult(String result) {
        if (result == null || result.isEmpty()) {
            return result;
        }
        String trimmed = result.trim();

        // 1. 剥离 markdown 代码块标记(无论代码块是否位于开头)
        // 匹配 ```json ... ``` 或 ``` ... ``` 中的内容
        int codeBlockStart = trimmed.indexOf("```");
        if (codeBlockStart >= 0) {
            int firstNewline = trimmed.indexOf('\n', codeBlockStart);
            if (firstNewline > 0) {
                int codeBlockEnd = trimmed.indexOf("```", firstNewline);
                if (codeBlockEnd > firstNewline) {
                    trimmed = trimmed.substring(firstNewline + 1, codeBlockEnd).trim();
                }
            }
        }

        // 2. 提取首个 { 到末尾 } 的 JSON 对象子串(兼容带引导语场景)
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            trimmed = trimmed.substring(firstBrace, lastBrace + 1);
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
        URL parsedUrl;
        try {
            parsedUrl = new URL(imageUrl);
        } catch (java.net.MalformedURLException e) {
            throw new IOException("URL格式不合法");
        }
        String protocol = parsedUrl.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            throw new IOException("仅支持http/https协议");
        }
        String host = parsedUrl.getHost();
        if (host == null || host.isEmpty()) {
            throw new IOException("URL缺少主机名");
        }

        // 解析所有IP并逐一校验,防止多IP中混入内网地址;校验通过后直接用 IP 直连防 DNS rebinding
        InetAddress[] addrs = InetAddress.getAllByName(host);
        InetAddress chosenIp = null;
        for (InetAddress addr : addrs) {
            validateInetAddress(addr);
            if (chosenIp == null) {
                chosenIp = addr;
            }
        }
        if (chosenIp == null) {
            throw new IOException("无法解析图片URL主机");
        }

        // 使用解析后的 IP 直连,Host 头保持原域名以兼容虚拟主机
        int port = parsedUrl.getPort();
        String path = parsedUrl.getPath() + (parsedUrl.getQuery() != null ? "?" + parsedUrl.getQuery() : "");
        String ipStr = chosenIp.getHostAddress();
        // IPv6 地址需用方括号包裹
        String hostPart = (ipStr.contains(":") ? "[" + ipStr + "]" : ipStr) + (port > 0 ? ":" + port : "");
        String directUrl = parsedUrl.getProtocol() + "://" + hostPart + path;

        Request request = new Request.Builder()
                .url(directUrl)
                .header("Host", host)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载图片失败，状态码：" + response.code());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("下载图片失败，响应体为空");
            }
            // 限制响应体大小(20MB),防止用SSRF做盲打放大攻击或 OOM
            long contentLength = body.contentLength();
            if (contentLength > 20L * 1024 * 1024) {
                throw new IOException("图片体积超过限制(20MB)");
            }
            // 读取时按字节上限截断,防止 chunked transfer 不返回 Content-Length 时 OOM
            return readWithLimit(body.byteStream(), 20L * 1024 * 1024);
        }
    }

    /**
     * 限制最大读取字节数,防止恶意服务器无限制返回数据导致 OOM
     */
    private byte[] readWithLimit(java.io.InputStream is, long maxBytes) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int n;
        while ((n = is.read(buffer)) != -1) {
            total += n;
            if (total > maxBytes) {
                throw new IOException("图片体积超过限制(" + (maxBytes / 1024 / 1024) + "MB)");
            }
            baos.write(buffer, 0, n);
        }
        return baos.toByteArray();
    }

    /**
     * 校验 IP 地址是否为禁止访问的内网/保留地址
     * - JDK 标准判定:anyLocal/loopback/siteLocal/linkLocal/multicast
     * - 显式补充:云元数据 169.254/16、0/8、CGNAT 100.64/10、基准测试 198.18/15、保留 240/4
     * - IPv6 链路本地 fe80::/10、ULA fc00::/7
     * 注:getHostAddress() 返回的 IPv6 一律小写,无需检查大写前缀
     */
    private void validateInetAddress(InetAddress addr) throws IOException {
        if (addr.isAnyLocalAddress()
                || addr.isLoopbackAddress()
                || addr.isSiteLocalAddress()
                || addr.isLinkLocalAddress()
                || addr.isMulticastAddress()) {
            throw new IOException("禁止访问内网/保留地址");
        }
        // 去除 IPv6 zone id(如 fe80::1%eth0),并统一小写
        String ip = addr.getHostAddress().toLowerCase();
        int zoneIdx = ip.indexOf('%');
        if (zoneIdx > 0) {
            ip = ip.substring(0, zoneIdx);
        }

        // IPv4 点分十进制前缀校验
        if (ip.startsWith("169.254.")        // 链路本地(含云元数据 169.254.169.254)
                || ip.startsWith("0.")        // 0.0.0.0/8 本网络
                || ip.startsWith("198.18")    // 基准测试 198.18.0.0/15 (RFC 2544)
                || ip.startsWith("198.19")
                || ip.startsWith("240.")      // 保留 240.0.0.0/4 (RFC 1112)
                || ip.startsWith("241.")
                || ip.startsWith("242.")
                || ip.startsWith("243.")
                || ip.startsWith("244.")
                || ip.startsWith("245.")
                || ip.startsWith("246.")
                || ip.startsWith("247.")
                || ip.startsWith("248.")
                || ip.startsWith("249.")
                || ip.startsWith("25.")
                || ip.startsWith("255.")      // 广播 255.255.255.255
                || ip.startsWith("fe80")      // IPv6 链路本地 fe80::/10
                || ip.startsWith("fc")        // IPv6 ULA fc00::/7
                || ip.startsWith("fd")) {     // IPv6 ULA fd00::/8
            throw new IOException("禁止访问保留地址段");
        }
        // CGNAT 100.64.0.0/10 (100.64.0.0 ~ 100.127.255.255):按第一段+第二段数值精确判断
        if (ip.startsWith("100.")) {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 64 && second <= 127) {
                        throw new IOException("禁止访问保留地址段");
                    }
                } catch (NumberFormatException ignored) {
                    // 非标准 IPv4,跳过
                }
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
            throw new BusinessException(ErrorCode.AI_NOT_ENABLED, "AI功能未启用");
        }

        // 入参校验：金额必须为正数。amount 为 Double 包装类，
        // 若为 null 传入 buildAccountingRequest 的 %.2f 会自动拆箱触发 NPE。
        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "金额必须为正数");
        }

        // 构建GLM API请求
        Map<String, Object> request = buildAccountingRequest(description, amount);

        // 调用GLM API
        String response = callGlmApi(request, aiConfig.getChatModel());

        log.info("智能记账建议生成完成");
        return response;
    }

    /**
     * AI财税问答
     * @param question 用户财税问题
     * @return AI回答内容
     */
    public String chat(String question) {
        if (!aiConfig.getEnabled()) {
            throw new BusinessException(ErrorCode.AI_NOT_ENABLED, "AI功能未启用");
        }
        if (question == null || question.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "问题不能为空");
        }

        // 构建GLM API请求
        Map<String, Object> request = buildChatRequest(question);

        // 调用GLM API
        String response = callGlmApi(request, aiConfig.getChatModel());

        log.info("AI财税问答完成");
        return response;
    }

    /**
     * 构建OCR识别请求（自动识别票据类型）
     */
    private Map<String, Object> buildOcrRequest(String base64Image, Integer invoiceType, String contentType) {
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

        // 图片:按实际上传文件 MIME 构造 data URL,避免对 png/webp/pdf 统一声明 jpeg 导致模型识别异常
        content[1] = new HashMap<>();
        content[1].put("type", "image_url");
        Map<String, String> imageUrl = new HashMap<>();
        imageUrl.put("url", "data:" + contentType + ";base64," + base64Image);
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
        // 防御性兜底：amount 为 null 时 %.2f 自动拆箱会 NPE，此处兜底为 0
        messages[1].put("content", String.format("业务描述：%s，金额：%.2f元", description, amount == null ? 0.0 : amount));

        request.put("model", aiConfig.getChatModel());
        request.put("messages", messages);

        return request;
    }

    /**
     * 构建财税问答请求
     */
    private Map<String, Object> buildChatRequest(String question) {
        Map<String, Object> request = new HashMap<>();

        // 构建消息
        Map<String, Object>[] messages = new Map[2];

        // 系统消息：财税专家角色设定
        messages[0] = new HashMap<>();
        messages[0].put("role", "system");
        messages[0].put("content", "你是一名专业的财税顾问，精通中国会计准则、税法（增值税、企业所得税、个人所得税、印花税等）及代账实务。"
                + "请根据用户的问题，提供准确、专业、实用的财税解答。回答应条理清晰，必要时给出法规依据或实操建议。");

        // 用户消息
        messages[1] = new HashMap<>();
        messages[1].put("role", "user");
        messages[1].put("content", question);

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
                    // 仅记录日志,不把 errorBody 放进异常 message,避免向客户端泄露 GLM 内部错误体
                    log.error("GLM API调用失败，状态码：{}，错误信息：{}", response.code(), errorBody);
                    throw new BusinessException(ErrorCode.AI_API_CALL_ERROR,
                            "AI服务调用失败（HTTP " + response.code() + "），请稍后重试");
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("GLM API响应成功");

                // 解析响应，提取content
                // 注意：Jackson 默认将 JSON 数组反序列化为 ArrayList，不能强转为 Object[]，否则必抛 ClassCastException
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                Object choicesObj = responseMap.get("choices");
                if (choicesObj instanceof List<?> choicesList && !choicesList.isEmpty()) {
                    Object firstChoiceObj = choicesList.get(0);
                    if (firstChoiceObj instanceof Map<?, ?> firstChoice) {
                        Object messageObj = firstChoice.get("message");
                        if (messageObj instanceof Map<?, ?> message) {
                            Object content = message.get("content");
                            if (content != null) {
                                return content.toString();
                            }
                        }
                    }
                }

                return responseBody;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("GLM API调用异常", e);
            throw new BusinessException(ErrorCode.AI_API_CALL_ERROR, "AI服务调用异常，请稍后重试");
        }
    }
}
