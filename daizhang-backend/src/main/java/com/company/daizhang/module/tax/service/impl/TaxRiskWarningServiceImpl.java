package com.company.daizhang.module.tax.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.document.entity.InputInvoice;
import com.company.daizhang.module.document.entity.OutputInvoice;
import com.company.daizhang.module.document.mapper.InputInvoiceMapper;
import com.company.daizhang.module.document.mapper.OutputInvoiceMapper;
import com.company.daizhang.module.tax.dto.TaxRiskWarningRequest;
import com.company.daizhang.module.tax.entity.TaxDeclaration;
import com.company.daizhang.module.tax.entity.TaxRiskWarning;
import com.company.daizhang.module.tax.mapper.TaxDeclarationMapper;
import com.company.daizhang.module.tax.mapper.TaxRiskWarningMapper;
import com.company.daizhang.module.tax.service.TaxCalculateService;
import com.company.daizhang.module.tax.service.TaxRiskWarningService;
import com.company.daizhang.module.tax.vo.TaxCalculationResultVO;
import com.company.daizhang.module.tax.vo.TaxRiskWarningVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 税务风险预警服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TaxRiskWarningServiceImpl implements TaxRiskWarningService {

    private final TaxRiskWarningMapper taxRiskWarningMapper;
    private final TaxCalculateService taxCalculateService;
    private final OutputInvoiceMapper outputInvoiceMapper;
    private final InputInvoiceMapper inputInvoiceMapper;
    private final TaxDeclarationMapper taxDeclarationMapper;
    private final AccountSetAccessService accountSetAccessService;

    /**
     * 税负率预警阈值
     */
    private static final BigDecimal BURDEN_RATE_LOW = new BigDecimal("0.03");
    private static final BigDecimal BURDEN_RATE_HIGH = new BigDecimal("0.06");
    /**
     * 进项发票认证率预警阈值
     */
    private static final BigDecimal AUTH_RATE_LOW = new BigDecimal("0.50");

    @Override
    public PageResult<TaxRiskWarningVO> pageWarnings(Long accountSetId, Integer year, Integer month,
                                                      Integer riskLevel, int pageNum, int pageSize) {
        Page<TaxRiskWarning> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TaxRiskWarning> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaxRiskWarning::getAccountSetId, accountSetId)
                .eq(year != null, TaxRiskWarning::getYear, year)
                .eq(month != null, TaxRiskWarning::getMonth, month)
                .eq(riskLevel != null, TaxRiskWarning::getRiskLevel, riskLevel)
                .orderByDesc(TaxRiskWarning::getCreateTime);

        Page<TaxRiskWarning> result = taxRiskWarningMapper.selectPage(page, wrapper);
        List<TaxRiskWarningVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createWarning(TaxRiskWarningRequest request) {
        TaxRiskWarning warning = new TaxRiskWarning();
        BeanUtil.copyProperties(request, warning);
        warning.setStatus(0);
        taxRiskWarningMapper.insert(warning);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleWarning(Long id, String handleRemark) {
        TaxRiskWarning warning = taxRiskWarningMapper.selectById(id);
        if (warning == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "风险预警不存在");
        }
        // Bug 6 修复:校验当前用户对该预警所属账套的所有者权限,防止越权处理他人账套预警
        accountSetAccessService.checkOwner(warning.getAccountSetId());
        warning.setStatus(1);
        warning.setHandleRemark(handleRemark);
        taxRiskWarningMapper.updateById(warning);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ignoreWarning(Long id) {
        TaxRiskWarning warning = taxRiskWarningMapper.selectById(id);
        if (warning == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "风险预警不存在");
        }
        // Bug 6 修复:校验当前用户对该预警所属账套的所有者权限,防止越权忽略他人账套预警
        accountSetAccessService.checkOwner(warning.getAccountSetId());
        warning.setStatus(2);
        taxRiskWarningMapper.updateById(warning);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scanRiskWarnings(Long accountSetId, Integer year, Integer month) {
        // 先删除该期间未处理的风险预警，避免重复
        LambdaQueryWrapper<TaxRiskWarning> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(TaxRiskWarning::getAccountSetId, accountSetId)
                .eq(TaxRiskWarning::getYear, year)
                .eq(TaxRiskWarning::getMonth, month)
                .eq(TaxRiskWarning::getStatus, 0);
        taxRiskWarningMapper.delete(deleteWrapper);

        // 1. 税负率预警
        scanTaxBurdenRate(accountSetId, year, month);

        // 2. 发票预警（进项发票认证率）
        scanInvoiceAuthRate(accountSetId, year, month);

        // 3. 申报预警（未申报税种）
        scanDeclaration(accountSetId, year, month);
    }

    /**
     * 税负率预警：增值税税负率=应纳增值税/销售收入，低于3%或高于6%预警
     */
    private void scanTaxBurdenRate(Long accountSetId, Integer year, Integer month) {
        TaxCalculationResultVO vatResult = taxCalculateService.calculateVAT(accountSetId, year, month);
        BigDecimal vatAmount = vatResult.getTaxAmount();

        // 查询销售收入（销项发票不含税金额合计）
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();
        LambdaQueryWrapper<OutputInvoice> outputWrapper = new LambdaQueryWrapper<>();
        outputWrapper.eq(OutputInvoice::getAccountSetId, accountSetId)
                .eq(OutputInvoice::getInvoiceStatus, 0)
                .ge(OutputInvoice::getInvoiceDate, startDate)
                .le(OutputInvoice::getInvoiceDate, endDate);
        List<OutputInvoice> outputInvoices = outputInvoiceMapper.selectList(outputWrapper);
        BigDecimal salesRevenue = outputInvoices.stream()
                .map(OutputInvoice::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (salesRevenue.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        BigDecimal burdenRate = vatAmount.divide(salesRevenue, 4, RoundingMode.HALF_UP);

        if (burdenRate.compareTo(BURDEN_RATE_LOW) < 0) {
            TaxRiskWarning warning = new TaxRiskWarning();
            warning.setAccountSetId(accountSetId);
            warning.setYear(year);
            warning.setMonth(month);
            warning.setRiskType("税负率");
            warning.setRiskLevel(3);
            warning.setRiskDescription("增值税税负率偏低，存在少缴税款风险");
            warning.setRiskValue(burdenRate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%");
            warning.setSuggestion("税负率低于3%，请核查销项发票是否全部入账、进项税额抵扣是否合理");
            warning.setStatus(0);
            taxRiskWarningMapper.insert(warning);
        } else if (burdenRate.compareTo(BURDEN_RATE_HIGH) > 0) {
            TaxRiskWarning warning = new TaxRiskWarning();
            warning.setAccountSetId(accountSetId);
            warning.setYear(year);
            warning.setMonth(month);
            warning.setRiskType("税负率");
            warning.setRiskLevel(2);
            warning.setRiskDescription("增值税税负率偏高，可能存在进项税额抵扣不足");
            warning.setRiskValue(burdenRate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%");
            warning.setSuggestion("税负率高于6%，请核查进项发票是否已全部认证抵扣");
            warning.setStatus(0);
            taxRiskWarningMapper.insert(warning);
        }
    }

    /**
     * 发票预警：进项发票认证率低
     */
    private void scanInvoiceAuthRate(Long accountSetId, Integer year, Integer month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();

        LambdaQueryWrapper<InputInvoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InputInvoice::getAccountSetId, accountSetId)
                .ge(InputInvoice::getInvoiceDate, startDate)
                .le(InputInvoice::getInvoiceDate, endDate);
        List<InputInvoice> inputInvoices = inputInvoiceMapper.selectList(wrapper);

        if (inputInvoices.isEmpty()) {
            return;
        }

        long totalCount = inputInvoices.size();
        long authCount = inputInvoices.stream()
                .filter(i -> i.getAuthStatus() != null && i.getAuthStatus() == 1)
                .count();
        BigDecimal authRate = new BigDecimal(authCount)
                .divide(new BigDecimal(totalCount), 4, RoundingMode.HALF_UP);

        if (authRate.compareTo(AUTH_RATE_LOW) < 0) {
            TaxRiskWarning warning = new TaxRiskWarning();
            warning.setAccountSetId(accountSetId);
            warning.setYear(year);
            warning.setMonth(month);
            warning.setRiskType("发票");
            warning.setRiskLevel(2);
            warning.setRiskDescription("进项发票认证率偏低，存在未认证发票");
            warning.setRiskValue(authCount + "/" + totalCount + "（" + authRate.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%）");
            warning.setSuggestion("进项发票认证率低于50%，请及时认证可抵扣的进项发票，避免税款多缴");
            warning.setStatus(0);
            taxRiskWarningMapper.insert(warning);
        }
    }

    /**
     * 申报预警：当月是否有未申报税种
     */
    private void scanDeclaration(Long accountSetId, Integer year, Integer month) {
        LambdaQueryWrapper<TaxDeclaration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaxDeclaration::getAccountSetId, accountSetId)
                .eq(TaxDeclaration::getYear, year)
                .eq(TaxDeclaration::getMonth, month)
                .eq(TaxDeclaration::getStatus, 0);
        List<TaxDeclaration> undeclared = taxDeclarationMapper.selectList(wrapper);

        if (!undeclared.isEmpty()) {
            String taxTypes = undeclared.stream()
                    .map(TaxDeclaration::getTaxType)
                    .distinct()
                    .collect(Collectors.joining("、"));
            TaxRiskWarning warning = new TaxRiskWarning();
            warning.setAccountSetId(accountSetId);
            warning.setYear(year);
            warning.setMonth(month);
            warning.setRiskType("申报");
            warning.setRiskLevel(3);
            warning.setRiskDescription("当月存在未申报税种：" + taxTypes);
            warning.setRiskValue(undeclared.size() + "条未申报记录");
            warning.setSuggestion("请尽快完成当月税务申报，避免产生滞纳金");
            warning.setStatus(0);
            taxRiskWarningMapper.insert(warning);
        }
    }

    /**
     * 实体转VO
     */
    private TaxRiskWarningVO convertToVO(TaxRiskWarning warning) {
        TaxRiskWarningVO vo = new TaxRiskWarningVO();
        BeanUtil.copyProperties(warning, vo);
        return vo;
    }
}
