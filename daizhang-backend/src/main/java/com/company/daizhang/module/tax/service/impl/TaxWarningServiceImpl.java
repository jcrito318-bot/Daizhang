package com.company.daizhang.module.tax.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.accountset.mapper.AccountSetMapper;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.tax.dto.TaxBenchmarkUpdateRequest;
import com.company.daizhang.module.tax.entity.TaxBenchmark;
import com.company.daizhang.module.tax.entity.TaxDeclaration;
import com.company.daizhang.module.tax.mapper.TaxBenchmarkMapper;
import com.company.daizhang.module.tax.mapper.TaxDeclarationMapper;
import com.company.daizhang.module.tax.service.TaxWarningService;
import com.company.daizhang.module.tax.vo.TaxBenchmarkVO;
import com.company.daizhang.module.tax.vo.TaxTrendVO;
import com.company.daizhang.module.tax.vo.TaxWarningVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 税负预警服务实现
 * <p>
 * 增值税税负率 = 实际缴纳增值税 / 不含税销售收入 × 100%
 * 企业所得税税负率 = 实际缴纳所得税 / 营业收入 × 100%
 * <p>
 * 实际缴纳增值税取自 tax_declaration.actual_amount (tax_type=VAT/SmallScaleVAT/增值税)
 * 不含税销售收入取自 acc_account_balance.period_credit (科目编码前缀 5001 主营业务收入,兼容 6001)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxWarningServiceImpl implements TaxWarningService {

    private final AccountSetMapper accountSetMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final SubjectMapper subjectMapper;
    private final TaxDeclarationMapper taxDeclarationMapper;
    private final TaxBenchmarkMapper taxBenchmarkMapper;

    /**
     * 默认行业代码(无法匹配具体行业时使用)
     */
    private static final String DEFAULT_INDUSTRY_CODE = "DEFAULT";

    /**
     * 增值税税种代码(申报表 tax_type 字段值)
     * 兼容英文代码(VAT/SmallScaleVAT)与中文(增值税)两种写入方式
     */
    private static final Set<String> VAT_TAX_TYPES = Set.of("VAT", "SmallScaleVAT", "增值税");

    /**
     * 企业所得税税种代码
     */
    private static final Set<String> EIT_TAX_TYPES = Set.of("IncomeTax", "企业所得税");

    /**
     * 主营业务收入科目编码前缀(企业会计准则 6001 / 小企业会计准则 5001,本项目种子数据使用 5001)
     */
    private static final String[] REVENUE_SUBJECT_PREFIXES = {"5001", "6001"};

    /**
     * 税负率保留小数位数(0.0250 = 2.50%)
     */
    private static final int RATE_SCALE = 4;

    /**
     * 100% 的 BigDecimal 表示
     */
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Override
    @Transactional(readOnly = true)
    public TaxWarningVO getWarning(Long accountSetId, Integer year, Integer month) {
        AccountSet accountSet = loadAccountSet(accountSetId);
        TaxBenchmark benchmark = resolveBenchmark(accountSet.getIndustryType());

        // 实际缴纳增值税 / 企业所得税(取自申报表 actual_amount)
        BigDecimal vatActualAmount = sumActualAmount(accountSetId, year, month, VAT_TAX_TYPES);
        BigDecimal eitActualAmount = sumActualAmount(accountSetId, year, month, EIT_TAX_TYPES);

        // 不含税销售收入(主营业务收入,科目 5001/6001 贷方发生额)
        BigDecimal salesRevenue = sumRevenueCredit(accountSetId, year, month);

        // 计算税负率(保留4位小数)
        BigDecimal vatActualRate = computeRate(vatActualAmount, salesRevenue);
        BigDecimal eitActualRate = computeRate(eitActualAmount, salesRevenue);

        // 评定预警等级
        String vatLevel = evaluateLevel(vatActualRate, benchmark.getVatWarningLow(), benchmark.getVatWarningHigh());
        String eitLevel = evaluateLevel(eitActualRate, benchmark.getEitWarningLow(), benchmark.getEitWarningHigh());

        TaxWarningVO vo = new TaxWarningVO();
        vo.setAccountSetId(accountSetId);
        vo.setYear(year);
        vo.setMonth(month);
        vo.setIndustryCode(benchmark.getIndustryCode());
        vo.setIndustryName(benchmark.getIndustryName());

        vo.setVatActualRate(vatActualRate);
        vo.setVatBenchmarkRate(benchmark.getVatBenchmarkRate());
        vo.setVatWarningLow(benchmark.getVatWarningLow());
        vo.setVatWarningHigh(benchmark.getVatWarningHigh());
        vo.setVatWarningLevel(vatLevel);

        vo.setEitActualRate(eitActualRate);
        vo.setEitBenchmarkRate(benchmark.getEitBenchmarkRate());
        vo.setEitWarningLow(benchmark.getEitWarningLow());
        vo.setEitWarningHigh(benchmark.getEitWarningHigh());
        vo.setEitWarningLevel(eitLevel);

        vo.setVatActualAmount(vatActualAmount);
        vo.setEitActualAmount(eitActualAmount);
        vo.setSalesRevenue(salesRevenue);

        // 生成建议和预警明细
        List<String> suggestions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        buildAdvice(vatLevel, "VAT", vatActualRate, benchmark.getVatBenchmarkRate(),
                benchmark.getVatWarningLow(), benchmark.getVatWarningHigh(), suggestions, warnings);
        buildAdvice(eitLevel, "EIT", eitActualRate, benchmark.getEitBenchmarkRate(),
                benchmark.getEitWarningLow(), benchmark.getEitWarningHigh(), suggestions, warnings);

        // 销售收入为0时给出额外提示
        if (salesRevenue.compareTo(BigDecimal.ZERO) == 0) {
            warnings.add(String.format("%d年%d月主营业务收入为0,无法准确计算税负率,请先完成当月账务处理",
                    year, month));
        }

        vo.setSuggestions(suggestions);
        vo.setWarnings(warnings);
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxTrendVO> getTrend(Long accountSetId, Integer year) {
        loadAccountSet(accountSetId);

        // 一次性查询全年(1-12月)的增值税 / 企业所得税实际缴纳额
        LambdaQueryWrapper<TaxDeclaration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaxDeclaration::getAccountSetId, accountSetId)
                .eq(TaxDeclaration::getYear, year)
                .in(TaxDeclaration::getTaxType, unionTaxTypes());
        List<TaxDeclaration> declarations = taxDeclarationMapper.selectList(wrapper);
        Map<Integer, BigDecimal> vatByMonth = declarations.stream()
                .filter(d -> VAT_TAX_TYPES.contains(d.getTaxType()))
                .filter(d -> d.getActualAmount() != null)
                .collect(Collectors.groupingBy(TaxDeclaration::getMonth,
                        Collectors.reducing(BigDecimal.ZERO, TaxDeclaration::getActualAmount, BigDecimal::add)));
        Map<Integer, BigDecimal> eitByMonth = declarations.stream()
                .filter(d -> EIT_TAX_TYPES.contains(d.getTaxType()))
                .filter(d -> d.getActualAmount() != null)
                .collect(Collectors.groupingBy(TaxDeclaration::getMonth,
                        Collectors.reducing(BigDecimal.ZERO, TaxDeclaration::getActualAmount, BigDecimal::add)));

        // 一次性查询全年(1-12月)的科目余额,按月聚合主营业务收入
        LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
        balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getYear, year);
        List<AccountBalance> balances = accountBalanceMapper.selectList(balanceWrapper);

        // 仅取主营业务收入科目(编码前缀 5001/6001)
        Set<Long> revenueSubjectIds = collectRevenueSubjectIds(accountSetId);
        Map<Integer, BigDecimal> revenueByMonth = balances.stream()
                .filter(b -> revenueSubjectIds.contains(b.getSubjectId()))
                .filter(b -> b.getPeriodCredit() != null)
                .collect(Collectors.groupingBy(AccountBalance::getMonth,
                        Collectors.reducing(BigDecimal.ZERO, AccountBalance::getPeriodCredit, BigDecimal::add)));

        // 组装12个月趋势
        List<TaxTrendVO> trendList = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            TaxTrendVO vo = new TaxTrendVO();
            vo.setYear(year);
            vo.setMonth(month);
            BigDecimal revenue = revenueByMonth.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal vatAmount = vatByMonth.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal eitAmount = eitByMonth.getOrDefault(month, BigDecimal.ZERO);
            // 有销售收入才计算税负率;否则为 null(前端图表不绘制)
            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                vo.setVatRate(vatAmount.divide(revenue, RATE_SCALE, RoundingMode.HALF_UP));
                vo.setEitRate(eitAmount.divide(revenue, RATE_SCALE, RoundingMode.HALF_UP));
            }
            trendList.add(vo);
        }
        return trendList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxBenchmarkVO> listBenchmarks() {
        LambdaQueryWrapper<TaxBenchmark> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(TaxBenchmark::getIndustryCode);
        List<TaxBenchmark> benchmarks = taxBenchmarkMapper.selectList(wrapper);
        return benchmarks.stream().map(this::toBenchmarkVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBenchmark(Long id, TaxBenchmarkUpdateRequest request) {
        TaxBenchmark benchmark = taxBenchmarkMapper.selectById(id);
        if (benchmark == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "行业税负率基准不存在");
        }
        // 校验:下限 <= 基准 <= 上限(避免预警区间配置错误)
        validateRange(request.getVatWarningLow(), request.getVatBenchmarkRate(), request.getVatWarningHigh(),
                "增值税税负率");
        validateRange(request.getEitWarningLow(), request.getEitBenchmarkRate(), request.getEitWarningHigh(),
                "企业所得税税负率");

        benchmark.setVatBenchmarkRate(request.getVatBenchmarkRate());
        benchmark.setVatWarningLow(request.getVatWarningLow());
        benchmark.setVatWarningHigh(request.getVatWarningHigh());
        benchmark.setEitBenchmarkRate(request.getEitBenchmarkRate());
        benchmark.setEitWarningLow(request.getEitWarningLow());
        benchmark.setEitWarningHigh(request.getEitWarningHigh());
        taxBenchmarkMapper.updateById(benchmark);
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 加载账套并校验存在
     */
    private AccountSet loadAccountSet(Long accountSetId) {
        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_SET_NOT_FOUND);
        }
        return accountSet;
    }

    /**
     * 解析行业基准:先按行业代码精确匹配,再按行业名称模糊匹配,最终回退到 DEFAULT
     */
    private TaxBenchmark resolveBenchmark(String industryType) {
        if (industryType == null || industryType.isBlank()) {
            return loadBenchmarkByCode(DEFAULT_INDUSTRY_CODE);
        }
        String trimmed = industryType.trim();

        // 1. 精确匹配行业代码
        TaxBenchmark byCode = loadBenchmarkByCode(trimmed);
        if (byCode != null) {
            return byCode;
        }
        // 2. 按行业名称精确匹配
        LambdaQueryWrapper<TaxBenchmark> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(TaxBenchmark::getIndustryName, trimmed)
                .last("LIMIT 1");
        TaxBenchmark byName = taxBenchmarkMapper.selectOne(nameWrapper);
        if (byName != null) {
            return byName;
        }
        // 3. 关键词模糊匹配(例如 industryType="制造业-食品" 命中 "制造业")
        for (TaxBenchmark benchmark : listAllBenchmarks()) {
            if (benchmark.getIndustryCode().equals(DEFAULT_INDUSTRY_CODE)) {
                continue;
            }
            if (trimmed.contains(benchmark.getIndustryName())) {
                return benchmark;
            }
        }
        // 4. 回退 DEFAULT
        return loadBenchmarkByCode(DEFAULT_INDUSTRY_CODE);
    }

    private TaxBenchmark loadBenchmarkByCode(String code) {
        LambdaQueryWrapper<TaxBenchmark> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaxBenchmark::getIndustryCode, code).last("LIMIT 1");
        return taxBenchmarkMapper.selectOne(wrapper);
    }

    private List<TaxBenchmark> listAllBenchmarks() {
        return taxBenchmarkMapper.selectList(null);
    }

    /**
     * 汇总指定税种的实际缴纳金额(从 tax_declaration.actual_amount 累加)
     */
    private BigDecimal sumActualAmount(Long accountSetId, Integer year, Integer month, Set<String> taxTypes) {
        LambdaQueryWrapper<TaxDeclaration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaxDeclaration::getAccountSetId, accountSetId)
                .eq(TaxDeclaration::getYear, year)
                .eq(TaxDeclaration::getMonth, month)
                .in(TaxDeclaration::getTaxType, taxTypes);
        List<TaxDeclaration> declarations = taxDeclarationMapper.selectList(wrapper);
        return declarations.stream()
                .map(d -> d.getActualAmount() != null ? d.getActualAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 汇总主营业务收入(科目 5001/6001 系列的 period_credit 贷方发生额)
     */
    private BigDecimal sumRevenueCredit(Long accountSetId, Integer year, Integer month) {
        // 1. 一次性查回该账套该期间的所有科目余额
        LambdaQueryWrapper<AccountBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getYear, year)
                .eq(AccountBalance::getMonth, month);
        List<AccountBalance> balances = accountBalanceMapper.selectList(wrapper);

        // 2. 查找主营业务收入科目ID集合
        Set<Long> revenueSubjectIds = collectRevenueSubjectIds(accountSetId);
        if (revenueSubjectIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 3. 累加这些科目的贷方发生额
        return balances.stream()
                .filter(b -> revenueSubjectIds.contains(b.getSubjectId()))
                .map(b -> b.getPeriodCredit() != null ? b.getPeriodCredit() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 收集主营业务收入科目ID(编码前缀 5001 或 6001 的所有末级科目)
     */
    private Set<Long> collectRevenueSubjectIds(Long accountSetId) {
        LambdaQueryWrapper<Subject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Subject::getAccountSetId, accountSetId);
        List<Subject> subjects = subjectMapper.selectList(wrapper);
        Set<Long> ids = new java.util.HashSet<>();
        for (Subject subject : subjects) {
            if (subject.getCode() == null) {
                continue;
            }
            for (String prefix : REVENUE_SUBJECT_PREFIXES) {
                if (subject.getCode().startsWith(prefix)) {
                    ids.add(subject.getId());
                    break;
                }
            }
        }
        return ids;
    }

    /**
     * 计算税负率 = 分子 / 分母,保留4位小数;分母为0时返回0
     */
    private BigDecimal computeRate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal safeNumerator = numerator != null ? numerator : BigDecimal.ZERO;
        return safeNumerator.divide(denominator, RATE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 评定预警等级:
     * <ul>
     *   <li>低于下限: danger(涉嫌异常低)</li>
     *   <li>高于上限: warning(可能多交税)</li>
     *   <li>区间内: normal</li>
     * </ul>
     */
    private String evaluateLevel(BigDecimal actualRate, BigDecimal low, BigDecimal high) {
        if (actualRate == null) {
            return "normal";
        }
        if (low != null && actualRate.compareTo(low) < 0) {
            return "danger";
        }
        if (high != null && actualRate.compareTo(high) > 0) {
            return "warning";
        }
        return "normal";
    }

    /**
     * 根据预警等级生成建议与预警明细
     */
    private void buildAdvice(String level, String taxKind, BigDecimal actualRate, BigDecimal benchmarkRate,
                             BigDecimal low, BigDecimal high,
                             List<String> suggestions, List<String> warnings) {
        String ratePercent = formatPercent(actualRate);
        String benchmarkPercent = formatPercent(benchmarkRate);
        if ("danger".equals(level)) {
            // 低于下限:涉嫌异常低
            if ("VAT".equals(taxKind)) {
                warnings.add(String.format("增值税税负率 %s 低于行业预警下限 %s,涉嫌异常低,存在少缴税款风险",
                        ratePercent, formatPercent(low)));
                suggestions.add("增值税税负率低于行业基准,可能存在进项抵扣异常,建议核查本月采购发票");
            } else {
                warnings.add(String.format("企业所得税税负率 %s 低于行业预警下限 %s,涉嫌异常低",
                        ratePercent, formatPercent(low)));
                suggestions.add("企业所得税税负率偏低,建议核查成本费用合理性,关注税前扣除凭证");
            }
        } else if ("warning".equals(level)) {
            // 高于上限:可能多交税
            if ("VAT".equals(taxKind)) {
                warnings.add(String.format("增值税税负率 %s 高于行业预警上限 %s,可能存在多缴税",
                        ratePercent, formatPercent(high)));
                suggestions.add("增值税税负率高于行业基准,可考虑合理安排采购时点");
            } else {
                warnings.add(String.format("企业所得税税负率 %s 高于行业预警上限 %s,可能存在多缴税",
                        ratePercent, formatPercent(high)));
                suggestions.add("企业所得税税负率偏高,可考虑利用小型微利企业优惠");
            }
        } else {
            // 正常区间
            if ("VAT".equals(taxKind)) {
                suggestions.add(String.format("增值税税负率 %s 处于行业正常区间(%s ~ %s),税负水平合理",
                        ratePercent, formatPercent(low), formatPercent(high)));
            } else {
                suggestions.add(String.format("企业所得税税负率 %s 处于行业正常区间(%s ~ %s),税负水平合理",
                        ratePercent, formatPercent(low), formatPercent(high)));
            }
            // benchmark 变量保留用于未来扩展(偏离基准但仍在区间内时给出提醒)
            if (benchmarkRate != null && actualRate != null) {
                log.debug("税负率 {} 与基准 {} 偏差在容忍区间内", actualRate, benchmarkRate);
            }
        }
    }

    /**
     * 将税负率(0.0250)格式化为百分比字符串(2.50%)
     */
    private String formatPercent(BigDecimal rate) {
        if (rate == null) {
            return "0.00%";
        }
        return rate.multiply(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    /**
     * 校验预警区间:下限 <= 基准 <= 上限
     */
    private void validateRange(BigDecimal low, BigDecimal benchmark, BigDecimal high, String label) {
        if (low != null && benchmark != null && low.compareTo(benchmark) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    label + "下限预警不能大于基准");
        }
        if (high != null && benchmark != null && high.compareTo(benchmark) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    label + "上限预警不能小于基准");
        }
        if (low != null && high != null && low.compareTo(high) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    label + "预警下限不能大于上限");
        }
    }

    /**
     * 合并所有需要查询的税种代码(VAT + EIT),用于一次性查全年趋势
     */
    private Set<String> unionTaxTypes() {
        Set<String> all = new java.util.HashSet<>(VAT_TAX_TYPES);
        all.addAll(EIT_TAX_TYPES);
        return all;
    }

    private TaxBenchmarkVO toBenchmarkVO(TaxBenchmark benchmark) {
        TaxBenchmarkVO vo = new TaxBenchmarkVO();
        vo.setId(benchmark.getId());
        vo.setIndustryCode(benchmark.getIndustryCode());
        vo.setIndustryName(benchmark.getIndustryName());
        vo.setVatBenchmarkRate(benchmark.getVatBenchmarkRate());
        vo.setVatWarningLow(benchmark.getVatWarningLow());
        vo.setVatWarningHigh(benchmark.getVatWarningHigh());
        vo.setEitBenchmarkRate(benchmark.getEitBenchmarkRate());
        vo.setEitWarningLow(benchmark.getEitWarningLow());
        vo.setEitWarningHigh(benchmark.getEitWarningHigh());
        vo.setCreateTime(benchmark.getCreateTime());
        vo.setUpdateTime(benchmark.getUpdateTime());
        return vo;
    }
}
