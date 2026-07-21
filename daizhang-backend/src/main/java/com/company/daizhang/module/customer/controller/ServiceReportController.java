package com.company.daizhang.module.customer.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.customer.dto.ServiceReportRequest;
import com.company.daizhang.module.customer.service.ServiceReportService;
import com.company.daizhang.module.customer.vo.ServiceReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 客户服务报告控制器
 */
@Tag(name = "客户服务报告")
@RestController
@RequestMapping("/service-report")
@RequiredArgsConstructor
public class ServiceReportController {

    private final ServiceReportService serviceReportService;

    @Operation(summary = "分页查询服务报告")
    @GetMapping("/page")
    @RequireAccountSetAccess
    public Result<PageResult<ServiceReportVO>> page(@RequestParam(required = false) Long accountSetId,
                                                     @RequestParam(required = false) Long customerId,
                                                     @RequestParam(required = false) Integer reportYear,
                                                     @RequestParam(required = false) Integer reportMonth,
                                                     @RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<ServiceReportVO> page = serviceReportService.pageReports(accountSetId, customerId,
                reportYear, reportMonth, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询服务报告")
    @GetMapping("/{id}")
    public Result<ServiceReportVO> getById(@PathVariable Long id) {
        ServiceReportVO report = serviceReportService.getReportById(id);
        return Result.success(report);
    }

    @Operation(summary = "创建服务报告")
    @PostMapping
    // IDOR 防护(纵深防御):edge-level 预校验,Service 层仍保留 checkOwner 作为兜底
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> create(@Valid @RequestBody ServiceReportRequest request) {
        serviceReportService.createReport(request);
        return Result.success();
    }

    @Operation(summary = "更新服务报告")
    @PutMapping("/{id}")
    // IDOR 防护(纵深防御):edge-level 预校验,Service 层仍保留 checkOwner 作为兜底
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ServiceReportRequest request) {
        serviceReportService.updateReport(id, request);
        return Result.success();
    }

    @Operation(summary = "删除服务报告")
    @DeleteMapping("/{id}")
    // IDOR 防护(纵深防御):edge-level 预校验,Service 层仍保留 checkOwner 作为兜底
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> delete(@PathVariable Long id) {
        serviceReportService.deleteReport(id);
        return Result.success();
    }

    @Operation(summary = "发布服务报告")
    @PostMapping("/{id}/publish")
    // IDOR 防护(纵深防御):edge-level 预校验,Service 层仍保留 checkOwner 作为兜底
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> publish(@PathVariable Long id) {
        serviceReportService.publishReport(id);
        return Result.success();
    }

    @Operation(summary = "自动生成服务报告")
    @PostMapping("/generate")
    @RequireAccountSetAccess
    public Result<ServiceReportVO> generate(@RequestParam Long accountSetId,
                                            @RequestParam(required = false) Long customerId,
                                            @RequestParam Integer year,
                                            @RequestParam(required = false) Integer month) {
        ServiceReportVO report = serviceReportService.generateReport(accountSetId, customerId, year, month);
        return Result.success(report);
    }
}
