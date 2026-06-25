package com.company.daizhang.module.asset.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.asset.dto.AssetStocktakeQueryRequest;
import com.company.daizhang.module.asset.dto.AssetStocktakeRequest;
import com.company.daizhang.module.asset.vo.AssetStocktakeVO;

/**
 * 资产盘点服务接口
 */
public interface AssetStocktakeService {

    /**
     * 分页查询盘点单
     */
    PageResult<AssetStocktakeVO> pageStocktakes(AssetStocktakeQueryRequest request);

    /**
     * 根据ID查询盘点单详情（含明细）
     */
    AssetStocktakeVO getStocktakeById(Long id);

    /**
     * 创建盘点单（自动生成盘点明细，账面数据带入）
     */
    Long createStocktake(AssetStocktakeRequest request);

    /**
     * 删除盘点单
     */
    void deleteStocktake(Long id);

    /**
     * 录入实盘数量（更新明细的实盘数量、差异、结果）
     *
     * @param detailId       盘点明细ID
     * @param actualQuantity 实盘数量
     * @param actualValue    实盘原值
     * @param handleOpinion  处理意见
     */
    void inputActualData(Long detailId, java.math.BigDecimal actualQuantity,
                         java.math.BigDecimal actualValue, String handleOpinion);

    /**
     * 完成盘点（汇总盘盈盘亏数量，更新盘点单状态为已完成）
     */
    void completeStocktake(Long id);

    /**
     * 生成盘点差异调账凭证
     * 盘亏：借-待处理财产损溢  贷-固定资产/累计折旧
     * 盘盈：借-固定资产  贷-待处理财产损溢
     *
     * @param id 盘点单ID
     * @return 生成的凭证ID
     */
    Long generateStocktakeVoucher(Long id);
}
