package com.company.daizhang.module.report.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.report.dto.CustomReportRequest;
import com.company.daizhang.module.report.service.CustomReportService;
import com.company.daizhang.module.report.vo.CustomReportDataVO;
import com.company.daizhang.module.report.vo.CustomReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 自定义报表控制器
 */
@Tag(name = "自定义报表管理")
@RestController
@RequestMapping("/report/custom")
@RequiredArgsConstructor
public class CustomReportController {

    private final CustomReportService customReportService;

    @Operation(summary = "分页查询自定义报表")
    @GetMapping("/page")
    public Result<PageResult<CustomReportVO>> page(
            @RequestParam(required = false) String reportName,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<CustomReportVO> page = customReportService.pageReports(reportName, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询自定义报表(含明细)")
    @GetMapping("/{id}")
    public Result<CustomReportVO> getById(@PathVariable Long id) {
        CustomReportVO vo = customReportService.getReportById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建自定义报表")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CustomReportRequest request) {
        customReportService.createReport(request);
        return Result.success();
    }

    @Operation(summary = "更新自定义报表")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CustomReportRequest request) {
        customReportService.updateReport(id, request);
        return Result.success();
    }

    @Operation(summary = "删除自定义报表")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customReportService.deleteReport(id);
        return Result.success();
    }

    @Operation(summary = "执行自定义报表取数")
    @GetMapping("/{id}/execute")
    public Result<CustomReportDataVO> execute(
            @PathVariable Long id,
            @RequestParam Long accountSetId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        CustomReportDataVO data = customReportService.executeReport(id, accountSetId, year, month);
        return Result.success(data);
    }
}
