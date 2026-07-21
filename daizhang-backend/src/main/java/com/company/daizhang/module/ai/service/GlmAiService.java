package com.company.daizhang.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.ai.config.AiConfig;
import com.company.daizhang.module.ai.dto.AccountingSuggestionResponse;
import com.company.daizhang.module.ai.entity.AiAccountingRule;
import com.company.daizhang.module.ai.entity.AiRecognitionFeedback;
import com.company.daizhang.module.ai.service.RecognitionFeedbackService;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.service.SubjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * GLM AI服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlmAiService {

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;
    private final AccountingRuleService accountingRuleService;
    private final RecognitionFeedbackService recognitionFeedbackService;
    private final SubjectService subjectService;

    /** 注入 prompt 的科目数量上限(避免 prompt 过长导致 token 超限/费用过高) */
    private static final int MAX_SUBJECTS_IN_PROMPT = 100;
    /** few-shot 示例数量 */
    private static final int FEW_SHOT_EXAMPLES_COUNT = 5;
    /** 建议来源:规则命中 */
    private static final String SOURCE_RULE = "rule";
    /** 建议来源:AI 推理 */
    private static final String SOURCE_AI = "ai";

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
     * 增强版智能记账建议(规则优先 + few-shot + 科目上下文)
     * <p>
     * 三级降级策略,确保既省钱又稳定:
     * 1. 规则库匹配:命中直接返回(source=rule, confidence=1.0),不调用 AI
     * 2. AI 推理:未命中规则时,注入当前账套科目体系 + 历史成功反馈 few-shot,调用 GLM
     * 3. 科目校验:AI 返回的科目编码必须在可用科目列表中,否则回退到最接近的科目或返回错误
     * <p>
     * 与旧版 {@link #suggestAccounting(String, Double)} 区别:
     * - 旧版返回原始 JSON 字符串,新版返回结构化 {@link AccountingSuggestionResponse}
     * - 新版支持规则库优先匹配,省时省钱
     * - 新版注入科目体系上下文 + few-shot,识别率更高
     *
     * @param description       业务描述
     * @param amount            金额(元,允许为 null)
     * @param accountSetId      账套ID(用于匹配账套级规则与注入科目体系)
     * @param availableSubjects 可用科目列表(允许为 null,为 null 时自动按 accountSetId 查询)
     * @return 结构化记账建议
     */
    public AccountingSuggestionResponse suggestAccountingWithContext(String description, Double amount,
                                                                     Long accountSetId,
                                                                     List<Subject> availableSubjects) {
        if (description == null || description.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "业务描述不能为空");
        }
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }

        // Step 1: 规则库匹配(优先于 AI,命中直接返回,不调用 AI)
        AiAccountingRule matchedRule = accountingRuleService.matchRule(accountSetId, description);
        if (matchedRule != null) {
            log.info("规则命中,跳过AI调用:accountSetId={},keyword={},ruleId={}",
                    accountSetId, matchedRule.getKeyword(), matchedRule.getId());
            return buildRuleResponse(matchedRule);
        }

        // Step 2: 未命中规则,走 AI 推理
        if (!aiConfig.getEnabled()) {
            throw new BusinessException(ErrorCode.AI_NOT_ENABLED, "AI功能未启用且无规则命中");
        }

        // 准备可用科目列表(为空时按 accountSetId 查询)
        List<Subject> subjects = availableSubjects;
        if (subjects == null || subjects.isEmpty()) {
            subjects = loadSubjectsForAccountSet(accountSetId);
        }
        if (subjects.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "当前账套无可用科目,无法生成记账建议");
        }

        // 拉取 few-shot 示例(完全采纳的历史反馈)
        List<AiRecognitionFeedback> fewShotExamples =
                recognitionFeedbackService.getRecentFewShotExamples(accountSetId, FEW_SHOT_EXAMPLES_COUNT);

        // 构建增强 prompt
        Map<String, Object> request = buildEnhancedAccountingRequest(description, amount, subjects, fewShotExamples);

        // 调用 GLM
        String response = callGlmApi(request, aiConfig.getChatModel());
        log.info("AI记账建议生成完成:accountSetId={},description={}", accountSetId, description);

        // Step 3: 解析并校验科目
        return parseAndValidateAiResponse(response, subjects);
    }

    /**
     * 根据账套ID加载科目列表(限制前 100 个常用科目,避免 prompt 过长)
     */
    private List<Subject> loadSubjectsForAccountSet(Long accountSetId) {
        LambdaQueryWrapper<Subject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getStatus, 1)
                .orderByAsc(Subject::getCode)
                .last("LIMIT " + MAX_SUBJECTS_IN_PROMPT);
        return subjectService.list(wrapper);
    }

    /**
     * 由规则命中结果构建响应
     */
    private AccountingSuggestionResponse buildRuleResponse(AiAccountingRule rule) {
        AccountingSuggestionResponse response = new AccountingSuggestionResponse();
        response.setSource(SOURCE_RULE);
        response.setDebitSubjectCode(rule.getDebitSubjectCode());
        response.setDebitSubjectName(rule.getDebitSubjectName());
        response.setCreditSubjectCode(rule.getCreditSubjectCode());
        response.setCreditSubjectName(rule.getCreditSubjectName());
        response.setSummary(rule.getVoucherSummary());
        // 规则命中视为完全可信(用户可手动维护规则)
        response.setConfidence(1.0);
        response.setRuleId(rule.getId());
        return response;
    }

    /**
     * 构建增强 prompt 的 GLM 请求
     * 注入:可用科目列表 + 历史记账示例(few-shot) + 业务描述 + 金额 + 输出要求
     */
    private Map<String, Object> buildEnhancedAccountingRequest(String description, Double amount,
                                                               List<Subject> subjects,
                                                               List<AiRecognitionFeedback> fewShotExamples) {
        Map<String, Object> request = new HashMap<>();

        Map<String, Object>[] messages = new Map[2];

        // 系统消息:角色 + 科目列表 + few-shot 示例
        messages[0] = new HashMap<>();
        messages[0].put("role", "system");
        messages[0].put("content", buildSystemPrompt(subjects, fewShotExamples));

        // 用户消息:业务描述 + 金额
        messages[1] = new HashMap<>();
        messages[1].put("role", "user");
        String amountStr = (amount == null) ? "未提供" : String.format("%.2f", amount);
        messages[1].put("content", String.format(
                "## 业务描述\n%s\n\n金额: %s元\n\n请根据上述业务描述推荐借贷科目,并严格按指定 JSON 格式返回。",
                description, amountStr));

        request.put("model", aiConfig.getChatModel());
        request.put("messages", messages);
        return request;
    }

    /**
     * 构建系统 prompt:科目列表 + few-shot 示例 + 输出要求
     */
    private String buildSystemPrompt(List<Subject> subjects, List<AiRecognitionFeedback> fewShotExamples) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是专业的会计记账助手。根据业务描述推荐借贷科目。\n\n");

        // 可用科目列表
        sb.append("## 可用科目列表\n");
        sb.append("必须从以下科目中选择,不可使用列表外的科目编码:\n");
        String accountList = subjects.stream()
                .map(s -> "- " + s.getCode() + " " + s.getName())
                .collect(Collectors.joining("\n"));
        sb.append(accountList);
        sb.append("\n\n");

        // few-shot 示例
        if (fewShotExamples != null && !fewShotExamples.isEmpty()) {
            sb.append("## 历史记账示例\n");
            sb.append("以下是同账套下用户已采纳的记账示例,可参考其科目选择:\n");
            for (AiRecognitionFeedback ex : fewShotExamples) {
                sb.append("- 描述: ").append(ex.getOriginalDescription())
                        .append(" → 借: ").append(ex.getActualDebitCode())
                        .append(", 贷: ").append(ex.getActualCreditCode());
                if (ex.getActualSummary() != null && !ex.getActualSummary().isEmpty()) {
                    sb.append(", 摘要: ").append(ex.getActualSummary());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        // 业务类型分类提示
        sb.append("## 业务类型分类提示\n");
        sb.append("- 费用类(差旅/办公/水电/电话/租金/折旧): 借 管理费用, 贷 银行存款/库存现金/累计折旧\n");
        sb.append("- 薪酬类(工资/社保/公积金): 借 管理费用, 贷 应付职工薪酬\n");
        sb.append("- 采购类(采购材料): 借 原材料/库存商品, 贷 应付账款/银行存款\n");
        sb.append("- 销售类(销售商品): 借 应收账款/银行存款, 贷 主营业务收入\n");
        sb.append("- 税金类(进项税/销项税): 涉及应交税费科目\n\n");

        // 输出要求
        sb.append("## 要求\n");
        sb.append("1. 必须从可用科目列表中选择科目,不可编造\n");
        sb.append("2. 借贷必须平衡\n");
        sb.append("3. 返回 JSON: {\"debitSubjectCode\": \"xxxx\", \"debitSubjectName\": \"xxx\", ");
        sb.append("\"creditSubjectCode\": \"xxxx\", \"creditSubjectName\": \"xxx\", ");
        sb.append("\"summary\": \"xxx\", \"confidence\": 0.85}\n");
        sb.append("4. confidence 范围 0-1,表示对推荐结果的置信度\n");
        sb.append("5. 仅返回 JSON,不要包含任何其他文字或 markdown 标记");
        return sb.toString();
    }

    /**
     * 解析 AI 返回的 JSON 并校验科目编码是否在可用科目列表中
     * - 科目编码存在:返回原始 AI 建议
     * - 科目编码不存在:尝试按编码前缀回退到最接近的科目
     * - 解析失败:抛业务异常,由 GlobalExceptionHandler 返回 500
     */
    @SuppressWarnings("unchecked")
    private AccountingSuggestionResponse parseAndValidateAiResponse(String response, List<Subject> availableSubjects) {
        String cleaned = cleanJsonResult(response);
        Map<String, Object> parsed;
        try {
            parsed = objectMapper.readValue(cleaned, Map.class);
        } catch (Exception e) {
            log.warn("AI返回JSON解析失败,raw={}", response, e);
            throw new BusinessException(ErrorCode.AI_OCR_PARSE_ERROR.getCode(),
                    "AI返回结果解析失败,请重试或人工选择科目");
        }

        AccountingSuggestionResponse result = new AccountingSuggestionResponse();
        result.setSource(SOURCE_AI);
        result.setRawAiResponse(response);

        String debitCode = getStringValue(parsed, "debitSubjectCode");
        String debitName = getStringValue(parsed, "debitSubjectName");
        String creditCode = getStringValue(parsed, "creditSubjectCode");
        String creditName = getStringValue(parsed, "creditSubjectName");
        String summary = getStringValue(parsed, "summary");
        Double confidence = getDoubleValue(parsed, "confidence");

        // 校验科目编码是否存在,不存在则按前缀回退
        Subject debitSubject = findSubjectByCode(availableSubjects, debitCode);
        if (debitSubject == null) {
            debitSubject = findClosestSubjectByPrefix(availableSubjects, debitCode);
            if (debitSubject != null) {
                log.info("借方科目回退:{} -> {}", debitCode, debitSubject.getCode());
            }
        }
        Subject creditSubject = findSubjectByCode(availableSubjects, creditCode);
        if (creditSubject == null) {
            creditSubject = findClosestSubjectByPrefix(availableSubjects, creditCode);
            if (creditSubject != null) {
                log.info("贷方科目回退:{} -> {}", creditCode, creditSubject.getCode());
            }
        }

        // 借贷科目均无法匹配时返回错误
        if (debitSubject == null && creditSubject == null) {
            log.warn("AI返回的借贷科目均不在可用列表中:debit={},credit={}", debitCode, creditCode);
            throw new BusinessException(ErrorCode.AI_OCR_PARSE_ERROR.getCode(),
                    "AI推荐的科目不在当前账套科目体系中,请人工选择科目");
        }

        // 部分缺失时,另一方可保留 AI 原始返回(由前端二次校验)
        result.setDebitSubjectCode(debitSubject != null ? debitSubject.getCode() : debitCode);
        result.setDebitSubjectName(debitSubject != null ? debitSubject.getName() : debitName);
        result.setCreditSubjectCode(creditSubject != null ? creditSubject.getCode() : creditCode);
        result.setCreditSubjectName(creditSubject != null ? creditSubject.getName() : creditName);
        result.setSummary(summary);
        result.setConfidence(confidence != null ? confidence : 0.5);
        return result;
    }

    /**
     * 按编码精确匹配科目
     */
    private Subject findSubjectByCode(List<Subject> subjects, String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        return subjects.stream()
                .filter(s -> code.equals(s.getCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 按编码前缀回退查找最接近的科目
     * 例:AI 返回 "6602.05" 但实际科目只有 "6602",回退到 "6602"
     */
    private Subject findClosestSubjectByPrefix(List<Subject> subjects, String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        // 截取前 4 位作为一级科目编码
        String prefix = code.length() >= 4 ? code.substring(0, 4) : code;
        return subjects.stream()
                .filter(s -> s.getCode() != null && s.getCode().startsWith(prefix))
                .findFirst()
                .orElse(null);
    }

    /**
     * 从 Map 中安全取字符串值
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? null : v.toString();
    }

    /**
     * 从 Map 中安全取 Double 值(支持 Number/字符串)
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number num) {
            return num.doubleValue();
        }
        try {
            return Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
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
