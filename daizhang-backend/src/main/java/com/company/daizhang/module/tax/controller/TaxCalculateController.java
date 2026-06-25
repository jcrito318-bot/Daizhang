package com.company.daizhang.module.tax.controller;

import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.tax.service.TaxCalculateService;
import com.company.daizhang.module.tax.vo.TaxCalculationResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 税种自动计算控制器
 * 暴露TaxCalculateService的5个税种自动计算方法，实时计算不落库
 */
@Slf4j
@Tag(name = "税种自动计算")
@RestController
@RequestMapping("/tax/calculate")
@RequiredArgsConstructor
public class TaxCalculateController {

    private final TaxCalculateService taxCalculateService;

    @Operation(summary = "计算所有税种（增值税/附加税/企业所得税/个人所得税）")
    @GetMapping("/all")
    public Result<List<TaxCalculationResultVO>> calculateAllTaxes(@RequestParam Long accountSetId,
                                                                   @RequestParam Integer year,
                                                                   @RequestParam Integer month) {
        List<TaxCalculationResultVO> results = taxCalculateService.calculateAllTaxes(accountSetId, year, month);
        return Result.success(results);
    }

    @Operation(summary = "计算增值税")
    @GetMapping("/vat")
    public Result<TaxCalculationResultVO> calculateVAT(@RequestParam Long accountSetId,
                                                        @RequestParam Integer year,
                                                        @RequestParam Integer month) {
        TaxCalculationResultVO result = taxCalculateService.calculateVAT(accountSetId, year, month);
        return Result.success(result);
    }

    @Operation(summary = "计算附加税（城建税/教育附加/地方教育附加）")
    @GetMapping("/surcharge")
    public Result<TaxCalculationResultVO> calculateSurchargeTax(@RequestParam Long accountSetId,
                                                                @RequestParam Integer year,
                                                                @RequestParam Integer month) {
        TaxCalculationResultVO result = taxCalculateService.calculateSurchargeTax(accountSetId, year, month);
        return Result.success(result);
    }

    @Operation(summary = "计算企业所得税（季度预缴）")
    @GetMapping("/income-tax")
    public Result<TaxCalculationResultVO> calculateCorporateIncomeTax(@RequestParam Long accountSetId,
                                                                      @RequestParam Integer year,
                                                                      @RequestParam Integer month) {
        TaxCalculationResultVO result = taxCalculateService.calculateCorporateIncomeTax(accountSetId, year, month);
        return Result.success(result);
    }

    @Operation(summary = "计算个人所得税（独立计算接口）")
    @GetMapping("/personal-income-tax")
    public Result<TaxCalculationResultVO> calculatePersonalIncomeTax(@RequestParam Long accountSetId,
                                                                     @RequestParam Integer year,
                                                                     @RequestParam Integer month) {
        TaxCalculationResultVO result = taxCalculateService.calculatePersonalIncomeTax(accountSetId, year, month);
        return Result.success(result);
    }
}
