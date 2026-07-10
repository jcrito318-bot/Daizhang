package com.company.daizhang.module.tax.controller;

import com.company.daizhang.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 税率字典管理
 * 提供常用税率字典查询(增值税/企业所得税/个税等)
 */
@Slf4j
@Tag(name = "税率字典")
@RestController
@RequestMapping("/tax/rate")
@RequiredArgsConstructor
public class TaxRateController {

    @Operation(summary = "查询税率字典列表")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        List<Map<String, Object>> rates = new ArrayList<>();

        // 增值税税率
        addRate(rates, "vat", "增值税", "13%", 0.13, "一般货物劳务");
        addRate(rates, "vat", "增值税", "9%", 0.09, "交通运输/邮政/基础电信/建筑/不动产");
        addRate(rates, "vat", "增值税", "6%", 0.06, "现代服务/金融/生活服务");
        addRate(rates, "vat", "增值税", "3%", 0.03, "小规模纳税人征收率");
        addRate(rates, "vat", "增值税", "1%", 0.01, "小规模纳税人减按征收");
        addRate(rates, "vat", "增值税", "0%", 0.00, "出口零税率");

        // 企业所得税
        addRate(rates, "income_tax", "企业所得税", "25%", 0.25, "一般企业");
        addRate(rates, "income_tax", "企业所得税", "20%", 0.20, "小型微利企业");
        addRate(rates, "income_tax", "企业所得税", "15%", 0.15, "高新技术企业/西部大开发");
        addRate(rates, "income_tax", "企业所得税", "10%", 0.10, "非居民企业");

        // 个人所得税(综合所得年度税率)
        addRate(rates, "personal_income_tax", "个人所得税", "3%", 0.03, "累计预扣率第1级(≤36000)");
        addRate(rates, "personal_income_tax", "个人所得税", "10%", 0.10, "累计预扣率第2级(36000-144000)");
        addRate(rates, "personal_income_tax", "个人所得税", "20%", 0.20, "累计预扣率第3级(144000-300000)");
        addRate(rates, "personal_income_tax", "个人所得税", "25%", 0.25, "累计预扣率第4级(300000-420000)");
        addRate(rates, "personal_income_tax", "个人所得税", "30%", 0.30, "累计预扣率第5级(420000-660000)");
        addRate(rates, "personal_income_tax", "个人所得税", "35%", 0.35, "累计预扣率第6级(660000-960000)");
        addRate(rates, "personal_income_tax", "个人所得税", "45%", 0.45, "累计预扣率第7级(>960000)");

        // 附加税费
        addRate(rates, "surcharge", "城建税", "7%", 0.07, "市区");
        addRate(rates, "surcharge", "城建税", "5%", 0.05, "县城/建制镇");
        addRate(rates, "surcharge", "城建税", "1%", 0.01, "其他地区");
        addRate(rates, "surcharge", "教育费附加", "3%", 0.03, "增值税*3%");
        addRate(rates, "surcharge", "地方教育附加", "2%", 0.02, "增值税*2%");
        addRate(rates, "surcharge", "印花税", "0.03%", 0.0003, "购销合同");
        addRate(rates, "surcharge", "残保金", "1.5%", 0.015, "残疾人就业保障金");

        return Result.success(rates);
    }

    @Operation(summary = "按税种类型查询税率")
    @GetMapping("/type/{taxType}")
    public Result<List<Map<String, Object>>> listByType(@PathVariable String taxType) {
        List<Map<String, Object>> allRates = list().getData();
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> rate : allRates) {
            if (taxType.equals(rate.get("taxType"))) {
                filtered.add(rate);
            }
        }
        return Result.success(filtered);
    }

    private void addRate(List<Map<String, Object>> rates, String taxType, String taxName,
                         String rateLabel, double rateValue, String description) {
        Map<String, Object> rate = new HashMap<>();
        rate.put("taxType", taxType);
        rate.put("taxName", taxName);
        rate.put("rateLabel", rateLabel);
        rate.put("rateValue", rateValue);
        rate.put("description", description);
        rates.add(rate);
    }
}
