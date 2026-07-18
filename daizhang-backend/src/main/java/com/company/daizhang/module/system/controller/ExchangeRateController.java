package com.company.daizhang.module.system.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.system.dto.ExchangeRateRequest;
import com.company.daizhang.module.system.service.ExchangeRateService;
import com.company.daizhang.module.system.vo.ExchangeRateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 汇率管理控制器
 */
@Tag(name = "汇率管理")
@RestController
@RequestMapping("/system/exchange-rate")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @Operation(summary = "分页查询汇率")
    @GetMapping("/page")
    public Result<PageResult<ExchangeRateVO>> page(
            @RequestParam(required = false) String currencyCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<ExchangeRateVO> page = exchangeRateService.pageRates(currencyCode, startDate, endDate, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "获取最新汇率")
    @GetMapping("/latest")
    public Result<ExchangeRateVO> getLatestRate(@RequestParam String currencyCode) {
        ExchangeRateVO vo = exchangeRateService.getLatestRate(currencyCode);
        return Result.success(vo);
    }

    @Operation(summary = "创建汇率")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> create(@Valid @RequestBody ExchangeRateRequest request) {
        exchangeRateService.createRate(request);
        return Result.success();
    }

    @Operation(summary = "更新汇率")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ExchangeRateRequest request) {
        exchangeRateService.updateRate(id, request);
        return Result.success();
    }

    @Operation(summary = "删除汇率")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        exchangeRateService.deleteRate(id);
        return Result.success();
    }

    @Operation(summary = "货币转换")
    @GetMapping("/convert")
    public Result<BigDecimal> convert(@RequestParam BigDecimal amount,
                                       @RequestParam String fromCurrency,
                                       @RequestParam String toCurrency) {
        BigDecimal result = exchangeRateService.convert(amount, fromCurrency, toCurrency);
        return Result.success(result);
    }
}
