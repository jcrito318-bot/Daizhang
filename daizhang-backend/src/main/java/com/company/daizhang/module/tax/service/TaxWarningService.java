package com.company.daizhang.module.tax.service;

import com.company.daizhang.module.tax.dto.TaxBenchmarkUpdateRequest;
import com.company.daizhang.module.tax.vo.TaxBenchmarkVO;
import com.company.daizhang.module.tax.vo.TaxTrendVO;
import com.company.daizhang.module.tax.vo.TaxWarningVO;

import java.util.List;

/**
 * 税负预警服务接口
 * <p>
 * 单账套税负率异常预警:
 * <ul>
 *   <li>增值税税负率偏低/偏高预警</li>
 *   <li>企业所得税税负率偏低/偏高预警</li>
 * </ul>
 */
public interface TaxWarningService {

    /**
     * 查询指定账套某月的税负预警
     *
     * @param accountSetId 账套ID
     * @param year         年度
     * @param month        月份(1-12)
     * @return 税负预警视图对象
     */
    TaxWarningVO getWarning(Long accountSetId, Integer year, Integer month);

    /**
     * 查询指定账套全年税负趋势(12个月)
     *
     * @param accountSetId 账套ID
     * @param year         年度
     * @return 12个月趋势列表
     */
    List<TaxTrendVO> getTrend(Long accountSetId, Integer year);

    /**
     * 查询所有行业税负率基准
     *
     * @return 行业基准列表
     */
    List<TaxBenchmarkVO> listBenchmarks();

    /**
     * 更新行业税负率基准(ADMIN only)
     *
     * @param id      基准ID
     * @param request 更新请求
     */
    void updateBenchmark(Long id, TaxBenchmarkUpdateRequest request);
}
