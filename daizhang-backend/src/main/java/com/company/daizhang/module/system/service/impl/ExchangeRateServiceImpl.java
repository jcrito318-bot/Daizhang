package com.company.daizhang.module.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.dto.ExchangeRateRequest;
import com.company.daizhang.module.system.entity.ExchangeRate;
import com.company.daizhang.module.system.mapper.ExchangeRateMapper;
import com.company.daizhang.module.system.service.ExchangeRateService;
import com.company.daizhang.module.system.vo.ExchangeRateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 汇率服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl extends ServiceImpl<ExchangeRateMapper, ExchangeRate> implements ExchangeRateService {

    private static final String CURRENCY_CNY = "CNY";

    @Override
    public PageResult<ExchangeRateVO> pageRates(String currencyCode, LocalDate startDate, LocalDate endDate,
                                                 int pageNum, int pageSize) {
        Page<ExchangeRate> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(currencyCode), ExchangeRate::getCurrencyCode, currencyCode)
               .ge(startDate != null, ExchangeRate::getRateDate, startDate)
               .le(endDate != null, ExchangeRate::getRateDate, endDate)
               .orderByDesc(ExchangeRate::getRateDate);

        Page<ExchangeRate> result = this.page(page, wrapper);

        List<ExchangeRateVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public ExchangeRateVO getLatestRate(String currencyCode) {
        if (StrUtil.isBlank(currencyCode)) {
            throw new BusinessException("币种代码不能为空");
        }

        LambdaQueryWrapper<ExchangeRate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExchangeRate::getCurrencyCode, currencyCode)
               .orderByDesc(ExchangeRate::getRateDate)
               .last("LIMIT 1");
        ExchangeRate rate = this.getOne(wrapper);
        if (rate == null) {
            throw new BusinessException("未查询到币种[" + currencyCode + "]的汇率");
        }
        return convertToVO(rate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRate(ExchangeRateRequest request) {
        ExchangeRate rate = new ExchangeRate();
        BeanUtil.copyProperties(request, rate);
        if (StrUtil.isBlank(rate.getRateType())) {
            rate.setRateType("中间价");
        }
        this.save(rate);
        log.info("创建汇率成功，币种: {}, 汇率: {}, 日期: {}", request.getCurrencyCode(), request.getRate(), request.getRateDate());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRate(Long id, ExchangeRateRequest request) {
        ExchangeRate rate = this.getById(id);
        if (rate == null) {
            throw new BusinessException(404, "汇率不存在");
        }

        BeanUtil.copyProperties(request, rate);
        this.updateById(rate);
        log.info("更新汇率成功，id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRate(Long id) {
        ExchangeRate rate = this.getById(id);
        if (rate == null) {
            throw new BusinessException(404, "汇率不存在");
        }
        this.removeById(id);
        log.info("删除汇率成功，id: {}", id);
    }

    @Override
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (StrUtil.isBlank(fromCurrency) || StrUtil.isBlank(toCurrency)) {
            throw new BusinessException("币种代码不能为空");
        }
        // 相同币种直接返回
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        // 获取源币种对人民币的汇率
        BigDecimal fromRate = getRateValue(fromCurrency);
        // 获取目标币种对人民币的汇率
        BigDecimal toRate = getRateValue(toCurrency);

        if (fromRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("币种[" + fromCurrency + "]汇率无效");
        }
        if (toRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("币种[" + toCurrency + "]汇率无效");
        }

        // 先转换为人民币，再转换为目标币种
        BigDecimal cnyAmount = amount.multiply(fromRate);
        return cnyAmount.divide(toRate, 6, RoundingMode.HALF_UP);
    }

    /**
     * 获取币种对人民币的汇率值
     */
    private BigDecimal getRateValue(String currencyCode) {
        // 人民币汇率固定为1
        if (CURRENCY_CNY.equalsIgnoreCase(currencyCode)) {
            return BigDecimal.ONE;
        }
        ExchangeRateVO rate = getLatestRate(currencyCode);
        return rate.getRate();
    }

    private ExchangeRateVO convertToVO(ExchangeRate rate) {
        ExchangeRateVO vo = new ExchangeRateVO();
        BeanUtil.copyProperties(rate, vo);
        return vo;
    }
}
