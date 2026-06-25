package com.company.daizhang.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.dto.ExchangeRateRequest;
import com.company.daizhang.module.system.entity.ExchangeRate;
import com.company.daizhang.module.system.vo.ExchangeRateVO;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 汇率服务接口
 */
public interface ExchangeRateService extends IService<ExchangeRate> {

    /**
     * 分页查询汇率
     */
    PageResult<ExchangeRateVO> pageRates(String currencyCode, LocalDate startDate, LocalDate endDate,
                                          int pageNum, int pageSize);

    /**
     * 获取最新汇率
     */
    ExchangeRateVO getLatestRate(String currencyCode);

    /**
     * 创建汇率
     */
    void createRate(ExchangeRateRequest request);

    /**
     * 更新汇率
     */
    void updateRate(Long id, ExchangeRateRequest request);

    /**
     * 删除汇率
     */
    void deleteRate(Long id);

    /**
     * 货币转换
     */
    BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency);
}
