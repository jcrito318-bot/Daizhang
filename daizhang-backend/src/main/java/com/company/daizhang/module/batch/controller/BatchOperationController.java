package com.company.daizhang.module.batch.controller;

import com.company.daizhang.common.annotation.OperationLog;
import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.batch.dto.BatchDepreciationRequest;
import com.company.daizhang.module.batch.dto.BatchHistoryQueryRequest;
import com.company.daizhang.module.batch.dto.BatchOperationResponse;
import com.company.daizhang.module.batch.dto.BatchPeriodCloseRequest;
import com.company.daizhang.module.batch.dto.BatchReportExportRequest;
import com.company.daizhang.module.batch.dto.BatchReportGenerateRequest;
import com.company.daizhang.module.batch.dto.BatchVoucherAuditRequest;
import com.company.daizhang.module.batch.service.BatchOperationService;
import com.company.daizhang.module.system.entity.SysOperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 跨账套批量操作控制器
 * <p>
 * 面向代账公司核心场景:代账会计一人代多家账,通过本控制器批量审核凭证、批量结账、批量生成报表。
 * <p>
 * 权限说明:
 * <ul>
 *   <li>控制器标注 {@code @RequireAccountSetAccess(required = false)}:批量请求中的 accountSetId
 *       嵌套在列表 DTO 中,切面无法从方法参数直接解析,故 required=false 放行,
 *       由 Service 层对每个 accountSetId 逐一调用
 *       {@code AccountSetAccessService.checkAccountantOrOwner} 兜底校验</li>
 * </ul>
 */
@Slf4j
@Tag(name = "跨账套批量操作")
@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class BatchOperationController {

    private final BatchOperationService batchOperationService;

    @Operation(summary = "批量审核凭证(跨多个账套)")
    @PostMapping("/voucher/audit")
    @OperationLog("批量审核凭证")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<BatchOperationResponse> batchAuditVoucher(@Valid @RequestBody BatchVoucherAuditRequest request) {
        BatchOperationResponse response = batchOperationService.batchAuditVoucher(request);
        return Result.success(response);
    }

    @Operation(summary = "批量结账(跨多个账套)")
    @PostMapping("/period/close")
    @OperationLog("批量结账")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<BatchOperationResponse> batchClosePeriod(@Valid @RequestBody BatchPeriodCloseRequest request) {
        BatchOperationResponse response = batchOperationService.batchClosePeriod(request);
        return Result.success(response);
    }

    @Operation(summary = "批量生成报表(跨多个账套)")
    @PostMapping("/report/generate")
    @OperationLog("批量生成报表")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<BatchOperationResponse> batchGenerateReport(@Valid @RequestBody BatchReportGenerateRequest request) {
        BatchOperationResponse response = batchOperationService.batchGenerateReport(request);
        return Result.success(response);
    }

    @PostMapping("/report/export-zip")
    @Operation(summary = "批量导出报表(zip 打包)(P5.1.1)")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public void batchExportReportZip(@Valid @RequestBody BatchReportExportRequest request, HttpServletResponse response) {
        batchOperationService.batchExportReportZip(request, response);
    }

    @PostMapping("/asset/depreciation/calculate")
    @Operation(summary = "跨账套批量计提固定资产折旧(P5.1.2)")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<BatchOperationResponse> batchCalculateDepreciation(@Valid @RequestBody BatchDepreciationRequest request) {
        BatchOperationResponse response = batchOperationService.batchCalculateDepreciation(request);
        return Result.success(response);
    }

    @Operation(summary = "查询批量操作历史(分页)")
    @GetMapping("/history")
    public Result<PageResult<SysOperationLog>> queryHistory(BatchHistoryQueryRequest request) {
        PageResult<SysOperationLog> page = batchOperationService.queryHistory(request);
        return Result.success(page);
    }
}
