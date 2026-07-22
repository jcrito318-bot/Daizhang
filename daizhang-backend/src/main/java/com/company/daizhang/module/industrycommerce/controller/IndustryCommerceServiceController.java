package com.company.daizhang.module.industrycommerce.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.industrycommerce.dto.IndustryCommerceServiceCreateRequest;
import com.company.daizhang.module.industrycommerce.dto.IndustryCommerceServiceQueryRequest;
import com.company.daizhang.module.industrycommerce.dto.IndustryCommerceServiceUpdateRequest;
import com.company.daizhang.module.industrycommerce.service.IndustryCommerceServiceService;
import com.company.daizhang.module.industrycommerce.vo.IndustryCommerceServiceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/**
 * 工商服务管理控制器
 * <p>
 * P5 代账定位精简:默认关闭。通过 {@code app.module.industry-commerce.enabled=true} 启用。
 */
@Tag(name = "工商服务")
@RestController
@RequestMapping("/ic/service")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.module.industry-commerce.enabled", havingValue = "true", matchIfMissing = false)
public class IndustryCommerceServiceController {

    private final IndustryCommerceServiceService industryCommerceServiceService;

    @Operation(summary = "分页查询工商服务")
    @GetMapping("/page")
    public Result<PageResult<IndustryCommerceServiceVO>> page(@Valid IndustryCommerceServiceQueryRequest request) {
        PageResult<IndustryCommerceServiceVO> page = industryCommerceServiceService.pageServices(request);
        return Result.success(page);
    }

    @Operation(summary = "查询工商服务详情（含外勤任务列表）")
    @GetMapping("/{id}")
    public Result<IndustryCommerceServiceVO> getById(@PathVariable Long id) {
        IndustryCommerceServiceVO vo = industryCommerceServiceService.getServiceById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建工商服务")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody IndustryCommerceServiceCreateRequest request) {
        Long id = industryCommerceServiceService.createService(request);
        return Result.success(id);
    }

    @Operation(summary = "更新工商服务")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody IndustryCommerceServiceUpdateRequest request) {
        industryCommerceServiceService.updateService(id, request);
        return Result.success();
    }

    @Operation(summary = "删除工商服务")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        industryCommerceServiceService.deleteService(id);
        return Result.success();
    }

    @Operation(summary = "工商服务派工")
    @PostMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id, @RequestParam Long assigneeId) {
        industryCommerceServiceService.assignService(id, assigneeId);
        return Result.success();
    }

    @Operation(summary = "完成工商服务")
    @PostMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        industryCommerceServiceService.completeService(id);
        return Result.success();
    }

    @Operation(summary = "取消工商服务")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        industryCommerceServiceService.cancelService(id);
        return Result.success();
    }
}
