package com.company.daizhang.module.batch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.mapper.AccountSetMapper;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.asset.dto.DepreciationRequest;
import com.company.daizhang.module.asset.service.AssetService;
import com.company.daizhang.module.batch.dto.BatchDepreciationRequest;
import com.company.daizhang.module.batch.dto.BatchHistoryQueryRequest;
import com.company.daizhang.module.batch.dto.BatchOperationResponse;
import com.company.daizhang.module.batch.dto.BatchOperationResultVO;
import com.company.daizhang.module.batch.dto.BatchPeriodCloseRequest;
import com.company.daizhang.module.batch.dto.BatchReportExportRequest;
import com.company.daizhang.module.batch.dto.BatchReportGenerateRequest;
import com.company.daizhang.module.batch.dto.BatchVoucherAuditRequest;
import com.company.daizhang.module.batch.service.BatchOperationService;
import com.company.daizhang.module.period.service.PeriodService;
import com.company.daizhang.module.period.vo.ClosePeriodResultVO;
import com.company.daizhang.module.report.dto.ReportQueryRequest;
import com.company.daizhang.module.report.service.ReportService;
import com.company.daizhang.module.report.util.ReportExcelUtil;
import com.company.daizhang.module.report.vo.BalanceSheetVO;
import com.company.daizhang.module.report.vo.CashFlowStatementVO;
import com.company.daizhang.module.report.vo.IncomeStatementVO;
import com.company.daizhang.module.report.vo.SubjectBalanceTableVO;
import com.company.daizhang.module.system.entity.SysOperationLog;
import com.company.daizhang.module.system.service.SysOperationLogService;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import com.company.daizhang.module.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 跨账套批量操作服务实现
 * <p>
 * 核心设计:
 * <ol>
 *   <li>单账套隔离:每个账套独立 try-catch,单个失败不影响其他账套</li>
 *   <li>per item 事务:每个账套的操作包裹在独立事务中(非整体事务),
 *       通过 {@link TransactionTemplate} 实现,避免一个账套回滚连带回滚已成功账套</li>
 *   <li>权限校验:每个账套均调用 {@link AccountSetAccessService#checkAccountantOrOwner}
 *       (写操作)或 {@link AccountSetAccessService#checkAccess}(读操作)</li>
 *   <li>结果汇总:返回每个账套的成功/失败及原因</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchOperationServiceImpl implements BatchOperationService {

    private final VoucherService voucherService;
    private final PeriodService periodService;
    private final ReportService reportService;
    private final ReportExcelUtil reportExcelUtil;
    private final AssetService assetService;
    private final AccountSetAccessService accountSetAccessService;
    private final AccountSetMapper accountSetMapper;
    private final VoucherMapper voucherMapper;
    private final SysOperationLogService operationLogService;
    private final PlatformTransactionManager transactionManager;

    /** per item 事务模板(由 PlatformTransactionManager 构造,避免依赖自动配置的 Bean) */
    private TransactionTemplate transactionTemplate;

    /** 报表类型 -> 中文名称 映射,同时用于校验报表类型合法性 */
    private static final Map<String, String> REPORT_TYPE_MAP = new HashMap<>();

    static {
        REPORT_TYPE_MAP.put("balance-sheet", "资产负债表");
        REPORT_TYPE_MAP.put("income-statement", "利润表");
        REPORT_TYPE_MAP.put("cash-flow-statement", "现金流量表");
        REPORT_TYPE_MAP.put("subject-balance", "科目余额表");
    }

    /** 操作类型 -> 操作日志关键字 映射(用于历史查询过滤) */
    private static final Map<String, String> OPERATION_TYPE_KEYWORD = new HashMap<>();

    static {
        OPERATION_TYPE_KEYWORD.put("voucher-audit", "批量审核凭证");
        OPERATION_TYPE_KEYWORD.put("period-close", "批量结账");
        OPERATION_TYPE_KEYWORD.put("report-generate", "批量生成报表");
    }

    @PostConstruct
    public void init() {
        // per item 事务模板:每个账套的操作在独立事务中执行。
        // 默认对 RuntimeException/Error 回滚;对捕获到的受检异常,在 catch 块中显式调用
        // status.setRollbackOnly() 标记回滚,从而实现与 @Transactional(rollbackFor = Exception.class)
        // 一致的"遇任何异常均回滚当前 item 事务"语义,同时不影响其他账套。
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public BatchOperationResponse batchAuditVoucher(BatchVoucherAuditRequest request) {
        log.info("批量审核凭证开始, 账套数量={}", request.getItems().size());
        List<BatchVoucherAuditRequest.VoucherAuditItem> items = dedupAuditItems(request.getItems());
        Map<Long, String> accountSetNameMap = loadAccountSetNames(items.stream()
                .map(BatchVoucherAuditRequest.VoucherAuditItem::getAccountSetId)
                .collect(Collectors.toList()));

        List<BatchOperationResultVO> results = new ArrayList<>();
        for (BatchVoucherAuditRequest.VoucherAuditItem item : items) {
            // per item 事务:每个账套的审核在独立事务中执行,失败仅回滚该账套
            BatchOperationResultVO result = transactionTemplate.execute(status -> {
                BatchOperationResultVO r = new BatchOperationResultVO();
                r.setAccountSetId(item.getAccountSetId());
                r.setAccountSetName(accountSetNameMap.get(item.getAccountSetId()));
                try {
                    doAuditVoucher(item, r);
                } catch (Exception e) {
                    // 单个账套失败:标记回滚当前 item 事务,但不影响其他账套
                    status.setRollbackOnly();
                    r.setStatus(BatchOperationResultVO.STATUS_FAILED);
                    r.setMessage(e.getMessage());
                    log.warn("批量审核凭证失败, 账套ID={}: {}", item.getAccountSetId(), e.getMessage());
                }
                return r;
            });
            results.add(result);
        }
        return buildResponse(results);
    }

    @Override
    public BatchOperationResponse batchClosePeriod(BatchPeriodCloseRequest request) {
        log.info("批量结账开始, 账套数量={}", request.getItems().size());
        List<BatchPeriodCloseRequest.PeriodCloseItem> items = dedupCloseItems(request.getItems());
        Map<Long, String> accountSetNameMap = loadAccountSetNames(items.stream()
                .map(BatchPeriodCloseRequest.PeriodCloseItem::getAccountSetId)
                .collect(Collectors.toList()));

        List<BatchOperationResultVO> results = new ArrayList<>();
        for (BatchPeriodCloseRequest.PeriodCloseItem item : items) {
            BatchOperationResultVO result = transactionTemplate.execute(status -> {
                BatchOperationResultVO r = new BatchOperationResultVO();
                r.setAccountSetId(item.getAccountSetId());
                r.setAccountSetName(accountSetNameMap.get(item.getAccountSetId()));
                try {
                    doClosePeriod(item, r);
                } catch (Exception e) {
                    status.setRollbackOnly();
                    r.setStatus(BatchOperationResultVO.STATUS_FAILED);
                    r.setMessage(e.getMessage());
                    log.warn("批量结账失败, 账套ID={}: {}", item.getAccountSetId(), e.getMessage());
                }
                return r;
            });
            results.add(result);
        }
        return buildResponse(results);
    }

    @Override
    public BatchOperationResponse batchGenerateReport(BatchReportGenerateRequest request) {
        log.info("批量生成报表开始, 账套数量={}", request.getItems().size());
        List<BatchReportGenerateRequest.ReportGenerateItem> items = dedupReportItems(request.getItems());
        Map<Long, String> accountSetNameMap = loadAccountSetNames(items.stream()
                .map(BatchReportGenerateRequest.ReportGenerateItem::getAccountSetId)
                .collect(Collectors.toList()));

        List<BatchOperationResultVO> results = new ArrayList<>();
        for (BatchReportGenerateRequest.ReportGenerateItem item : items) {
            BatchOperationResultVO result = transactionTemplate.execute(status -> {
                BatchOperationResultVO r = new BatchOperationResultVO();
                r.setAccountSetId(item.getAccountSetId());
                r.setAccountSetName(accountSetNameMap.get(item.getAccountSetId()));
                try {
                    doGenerateReport(item, r);
                } catch (Exception e) {
                    status.setRollbackOnly();
                    r.setStatus(BatchOperationResultVO.STATUS_FAILED);
                    r.setMessage(e.getMessage());
                    log.warn("批量生成报表失败, 账套ID={}: {}", item.getAccountSetId(), e.getMessage());
                }
                return r;
            });
            results.add(result);
        }
        return buildResponse(results);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SysOperationLog> queryHistory(BatchHistoryQueryRequest request) {
        // 将操作类型映射为操作日志关键字(如 voucher-audit -> "批量审核凭证"),
        // 复用 SysOperationLogService 的 operation 模糊匹配
        String operation = null;
        if (request.getOperationType() != null && !request.getOperationType().isBlank()) {
            operation = OPERATION_TYPE_KEYWORD.get(request.getOperationType());
            if (operation == null) {
                // 非法操作类型:回退为查询所有批量操作(以"批量"为关键字)
                operation = "批量";
            }
        } else {
            // 未指定操作类型:查询全部批量操作
            operation = "批量";
        }
        int pageNum = request.getPageNum() == null ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        return operationLogService.pageLogs(null, operation,
                request.getStartDate(), request.getEndDate(), pageNum, pageSize);
    }

    @Override
    public void batchExportReportZip(BatchReportExportRequest request, HttpServletResponse response) {
        log.info("批量导出报表(zip)开始, 账套数量={}", request.getItems().size());
        Map<Long, String> accountSetNameMap = loadAccountSetNames(request.getItems().stream()
                .map(BatchReportExportRequest.ReportExportItem::getAccountSetId)
                .collect(Collectors.toList()));

        // 设置响应头:zip 流式下载,文件名 reports_{timestamp}.zip
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String zipFileName = "reports_" + timestamp + ".zip";
        response.setContentType("application/zip");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            for (BatchReportExportRequest.ReportExportItem item : request.getItems()) {
                Long accountSetId = item.getAccountSetId();
                String accountSetName = accountSetNameMap.getOrDefault(accountSetId, String.valueOf(accountSetId));
                // 单个账套内多报表类型逐个 try-catch,失败跳过不中断整个 zip
                for (String reportType : item.getReportTypes()) {
                    // try-with-resources 关闭 workbook;失败时仅告警并跳过
                    try (XSSFWorkbook workbook = buildReportWorkbook(reportType, accountSetId,
                            item.getYear(), item.getMonth())) {
                        String reportName = REPORT_TYPE_MAP.getOrDefault(reportType, reportType);
                        String entryName = accountSetName + "_" + reportName + "_"
                                + item.getYear() + "_" + item.getMonth() + ".xlsx";
                        zos.putNextEntry(new ZipEntry(entryName));
                        workbook.write(zos);
                        zos.closeEntry();
                    } catch (Exception e) {
                        // 单个报表类型失败:跳过并告警,不影响其他文件
                        log.warn("批量导出报表失败, 账套ID={}, 报表类型={}: {}", accountSetId, reportType, e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error("批量导出报表 zip 写入失败", e);
            throw new BusinessException("批量导出报表失败: " + e.getMessage());
        }
        log.info("批量导出报表(zip)完成");
    }

    @Override
    public BatchOperationResponse batchCalculateDepreciation(BatchDepreciationRequest request) {
        log.info("批量计提折旧开始, 账套数量={}", request.getItems().size());
        Map<Long, String> accountSetNameMap = loadAccountSetNames(request.getItems().stream()
                .map(BatchDepreciationRequest.DepreciationItem::getAccountSetId)
                .collect(Collectors.toList()));

        List<BatchOperationResultVO> results = new ArrayList<>();
        for (BatchDepreciationRequest.DepreciationItem item : request.getItems()) {
            // per item 事务:每个账套的计提在独立事务中执行,失败仅回滚该账套
            BatchOperationResultVO result = transactionTemplate.execute(status -> {
                BatchOperationResultVO r = new BatchOperationResultVO();
                r.setAccountSetId(item.getAccountSetId());
                r.setAccountSetName(accountSetNameMap.get(item.getAccountSetId()));
                try {
                    accountSetAccessService.checkOwner(item.getAccountSetId());
                    DepreciationRequest depReq = new DepreciationRequest();
                    depReq.setAccountSetId(item.getAccountSetId());
                    depReq.setYear(item.getYear());
                    depReq.setMonth(item.getMonth());
                    assetService.calculateDepreciation(depReq);
                    r.setStatus(BatchOperationResultVO.STATUS_SUCCESS);
                    r.setMessage("折旧计提成功");
                } catch (Exception e) {
                    status.setRollbackOnly();
                    r.setStatus(BatchOperationResultVO.STATUS_FAILED);
                    r.setMessage(e.getMessage());
                    log.warn("批量计提折旧失败, 账套ID={}: {}", item.getAccountSetId(), e.getMessage());
                }
                return r;
            });
            results.add(result);
        }
        return buildResponse(results);
    }

    // ==================== 单账套处理逻辑 ====================

    /**
     * 审核单个账套的凭证
     */
    private void doAuditVoucher(BatchVoucherAuditRequest.VoucherAuditItem item, BatchOperationResultVO result) {
        Long accountSetId = item.getAccountSetId();
        // 权限校验:审核为状态变更操作,需 ACCOUNTANT/OWNER 权限
        accountSetAccessService.checkAccountantOrOwner(accountSetId);

        List<Long> voucherIds = item.getVoucherIds();
        boolean auditAll = (voucherIds == null || voucherIds.isEmpty());
        if (auditAll) {
            // 审核该期间所有未审核凭证
            voucherIds = queryUnauditedVoucherIds(accountSetId, item.getYear(), item.getMonth());
            if (voucherIds.isEmpty()) {
                result.setStatus(BatchOperationResultVO.STATUS_SUCCESS);
                result.setMessage("该期间无未审核凭证");
                return;
            }
        }

        int successCount = voucherService.batchAuditVoucher(voucherIds);
        int failCount = voucherIds.size() - successCount;
        if (failCount > 0) {
            result.setStatus(BatchOperationResultVO.STATUS_PARTIAL);
            result.setMessage(String.format("成功审核 %d 张凭证，失败 %d 张（详见系统日志）", successCount, failCount));
        } else {
            result.setStatus(BatchOperationResultVO.STATUS_SUCCESS);
            result.setMessage(String.format("成功审核 %d 张凭证", successCount));
        }
    }

    /**
     * 结账单个账套
     */
    private void doClosePeriod(BatchPeriodCloseRequest.PeriodCloseItem item, BatchOperationResultVO result) {
        Long accountSetId = item.getAccountSetId();
        // 权限校验:结账为状态变更操作,需 ACCOUNTANT/OWNER 权限
        accountSetAccessService.checkAccountantOrOwner(accountSetId);

        ClosePeriodResultVO closeResult = periodService.closePeriod(accountSetId, item.getYear(), item.getMonth());
        if (closeResult != null && closeResult.isSuccess()) {
            result.setStatus(BatchOperationResultVO.STATUS_SUCCESS);
            result.setMessage("结账成功");
        } else {
            // 结账失败(存在未审核/未过账凭证等):标记为失败并返回原因
            result.setStatus(BatchOperationResultVO.STATUS_FAILED);
            String msg = (closeResult != null && closeResult.getMessage() != null)
                    ? closeResult.getMessage() : "结账失败";
            throw new BusinessException(msg);
        }
    }

    /**
     * 生成单个账套的报表
     */
    private void doGenerateReport(BatchReportGenerateRequest.ReportGenerateItem item, BatchOperationResultVO result) {
        Long accountSetId = item.getAccountSetId();
        // 权限校验:报表为只读操作,ACCESS 级别(OWNER/ACCOUNTANT/VIEWER)即可
        accountSetAccessService.checkAccess(accountSetId);

        ReportQueryRequest reportRequest = new ReportQueryRequest();
        reportRequest.setAccountSetId(accountSetId);
        reportRequest.setYear(item.getYear());
        reportRequest.setMonth(item.getMonth());

        List<String> successTypes = new ArrayList<>();
        List<String> failedTypes = new ArrayList<>();
        for (String reportType : item.getReportTypes()) {
            try {
                generateSingleReport(reportType, reportRequest, accountSetId, item.getYear(), item.getMonth());
                successTypes.add(reportType);
            } catch (Exception e) {
                String name = REPORT_TYPE_MAP.getOrDefault(reportType, reportType);
                failedTypes.add(name + ":" + e.getMessage());
                log.warn("生成报表失败, 账套ID={}, 报表类型={}: {}", accountSetId, reportType, e.getMessage());
            }
        }

        if (failedTypes.isEmpty()) {
            result.setStatus(BatchOperationResultVO.STATUS_SUCCESS);
            result.setMessage(String.format("成功生成 %d 张报表", successTypes.size()));
        } else if (successTypes.isEmpty()) {
            result.setStatus(BatchOperationResultVO.STATUS_FAILED);
            result.setMessage("全部报表生成失败：" + String.join("；", failedTypes));
        } else {
            result.setStatus(BatchOperationResultVO.STATUS_PARTIAL);
            result.setMessage(String.format("成功 %d 张，失败 %d 张：%s",
                    successTypes.size(), failedTypes.size(), String.join("；", failedTypes)));
        }
    }

    /**
     * 根据报表类型调用对应的报表生成方法
     */
    private void generateSingleReport(String reportType, ReportQueryRequest request,
                                      Long accountSetId, Integer year, Integer month) {
        switch (reportType) {
            case "balance-sheet":
                reportService.balanceSheet(request);
                break;
            case "income-statement":
                reportService.incomeStatement(request);
                break;
            case "cash-flow-statement":
                reportService.cashFlowStatement(accountSetId, year, month);
                break;
            case "subject-balance":
                reportService.subjectBalanceTable(request);
                break;
            default:
                throw new BusinessException("不支持的报表类型：" + reportType);
        }
    }

    /**
     * 根据报表类型生成数据并构建 Excel Workbook(供批量 zip 打包复用)
     * <p>
     * 权限校验:报表为只读操作,ACCESS 级别(OWNER/ACCOUNTANT/VIEWER)即可。
     *
     * @param reportType  报表类型
     * @param accountSetId 账套ID
     * @param year         年度
     * @param month        月份
     * @return 已填充的 XSSFWorkbook(调用方负责关闭)
     */
    private XSSFWorkbook buildReportWorkbook(String reportType, Long accountSetId, Integer year, Integer month) {
        // 权限校验:报表为只读操作,ACCESS 级别(OWNER/ACCOUNTANT/VIEWER)即可
        accountSetAccessService.checkAccess(accountSetId);

        ReportQueryRequest reportRequest = new ReportQueryRequest();
        reportRequest.setAccountSetId(accountSetId);
        reportRequest.setYear(year);
        reportRequest.setMonth(month);

        switch (reportType) {
            case "balance-sheet":
                BalanceSheetVO balanceData = reportService.balanceSheet(reportRequest);
                return reportExcelUtil.buildBalanceSheetWorkbook(balanceData, year, month);
            case "income-statement":
                IncomeStatementVO incomeData = reportService.incomeStatement(reportRequest);
                return reportExcelUtil.buildIncomeStatementWorkbook(incomeData, year, month);
            case "cash-flow-statement":
                CashFlowStatementVO cashFlowData = reportService.cashFlowStatement(accountSetId, year, month);
                return reportExcelUtil.buildCashFlowStatementWorkbook(cashFlowData, year, month);
            case "subject-balance":
                SubjectBalanceTableVO subjectBalanceData = reportService.subjectBalanceTable(reportRequest);
                return reportExcelUtil.buildSubjectBalanceTableWorkbook(subjectBalanceData, year, month);
            default:
                throw new BusinessException("不支持的报表类型：" + reportType);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 查询指定账套期间内所有未审核凭证ID
     */
    private List<Long> queryUnauditedVoucherIds(Long accountSetId, Integer year, Integer month) {
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Voucher::getAccountSetId, accountSetId)
               .eq(Voucher::getYear, year)
               .eq(Voucher::getMonth, month)
               .eq(Voucher::getStatus, 0); // 0-未审核
        return voucherMapper.selectList(wrapper).stream()
                .map(Voucher::getId)
                .collect(Collectors.toList());
    }

    /**
     * 批量加载账套名称(用于结果展示),保留存在与不存在的账套映射
     */
    private Map<Long, String> loadAccountSetNames(List<Long> accountSetIds) {
        if (accountSetIds == null || accountSetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 去重后批量查询
        Set<Long> idSet = new LinkedHashSet<>(accountSetIds);
        List<AccountSet> accountSets = accountSetMapper.selectBatchIds(idSet);
        return accountSets.stream()
                .collect(Collectors.toMap(AccountSet::getId, AccountSet::getName, (a, b) -> a));
    }

    /**
     * 审核项去重(同一账套+期间合并凭证ID,避免重复审核)
     */
    private List<BatchVoucherAuditRequest.VoucherAuditItem> dedupAuditItems(
            List<BatchVoucherAuditRequest.VoucherAuditItem> items) {
        // 以 accountSetId 为键合并同一账套的审核项
        Map<Long, BatchVoucherAuditRequest.VoucherAuditItem> merged = new HashMap<>();
        for (BatchVoucherAuditRequest.VoucherAuditItem item : items) {
            BatchVoucherAuditRequest.VoucherAuditItem existing = merged.get(item.getAccountSetId());
            if (existing == null) {
                merged.put(item.getAccountSetId(), item);
            } else {
                // 合并 voucherIds
                List<Long> combined = new ArrayList<>();
                if (existing.getVoucherIds() != null) {
                    combined.addAll(existing.getVoucherIds());
                }
                if (item.getVoucherIds() != null) {
                    combined.addAll(item.getVoucherIds());
                }
                if (!combined.isEmpty()) {
                    existing.setVoucherIds(combined);
                }
            }
        }
        return new ArrayList<>(merged.values());
    }

    /**
     * 结账项去重(同一账套+期间仅结账一次)
     */
    private List<BatchPeriodCloseRequest.PeriodCloseItem> dedupCloseItems(
            List<BatchPeriodCloseRequest.PeriodCloseItem> items) {
        Map<String, BatchPeriodCloseRequest.PeriodCloseItem> merged = new HashMap<>();
        for (BatchPeriodCloseRequest.PeriodCloseItem item : items) {
            String key = item.getAccountSetId() + "-" + item.getYear() + "-" + item.getMonth();
            merged.putIfAbsent(key, item);
        }
        return new ArrayList<>(merged.values());
    }

    /**
     * 报表生成项去重(同一账套+期间合并报表类型)
     */
    private List<BatchReportGenerateRequest.ReportGenerateItem> dedupReportItems(
            List<BatchReportGenerateRequest.ReportGenerateItem> items) {
        Map<String, BatchReportGenerateRequest.ReportGenerateItem> merged = new HashMap<>();
        for (BatchReportGenerateRequest.ReportGenerateItem item : items) {
            String key = item.getAccountSetId() + "-" + item.getYear() + "-" + item.getMonth();
            BatchReportGenerateRequest.ReportGenerateItem existing = merged.get(key);
            if (existing == null) {
                merged.put(key, item);
            } else {
                Set<String> types = new LinkedHashSet<>(existing.getReportTypes());
                types.addAll(item.getReportTypes());
                existing.setReportTypes(new ArrayList<>(types));
            }
        }
        return new ArrayList<>(merged.values());
    }

    /**
     * 构建批量操作响应(汇总成功/失败统计)
     */
    private BatchOperationResponse buildResponse(List<BatchOperationResultVO> results) {
        BatchOperationResponse response = new BatchOperationResponse();
        response.setResults(results);
        response.setTotalCount(results.size());
        int success = 0;
        int fail = 0;
        for (BatchOperationResultVO r : results) {
            if (BatchOperationResultVO.STATUS_SUCCESS.equals(r.getStatus())) {
                success++;
            } else {
                fail++;
            }
        }
        response.setSuccessCount(success);
        response.setFailCount(fail);
        log.info("批量操作完成, 总数={}, 成功={}, 失败={}", results.size(), success, fail);
        return response;
    }
}
