package com.company.daizhang.module.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.asset.dto.*;
import com.company.daizhang.module.asset.entity.FixedAsset;
import com.company.daizhang.module.asset.vo.*;

import java.math.BigDecimal;

/**
 * 资产服务接口
 */
public interface AssetService extends IService<FixedAsset> {

    // ==================== 资产分类管理 ====================

    /**
     * 分页查询资产分类
     */
    PageResult<AssetCategoryVO> pageCategories(AssetCategoryQueryRequest request);

    /**
     * 根据ID查询资产分类
     */
    AssetCategoryVO getCategoryById(Long id);

    /**
     * 创建资产分类
     */
    void createCategory(AssetCategoryCreateRequest request);

    /**
     * 更新资产分类
     */
    void updateCategory(Long id, AssetCategoryUpdateRequest request);

    /**
     * 删除资产分类
     */
    void deleteCategory(Long id);

    /**
     * 查询所有资产分类（树形结构）
     */
    java.util.List<AssetCategoryVO> listCategoryTree(Long accountSetId);

    // ==================== 固定资产管理 ====================

    /**
     * 分页查询固定资产
     */
    PageResult<FixedAssetVO> pageAssets(FixedAssetQueryRequest request);

    /**
     * 根据ID查询固定资产
     */
    FixedAssetVO getAssetById(Long id);

    /**
     * 创建固定资产
     */
    void createAsset(FixedAssetCreateRequest request);

    /**
     * 更新固定资产
     */
    void updateAsset(Long id, FixedAssetUpdateRequest request);

    /**
     * 删除固定资产
     */
    void deleteAsset(Long id);

    /**
     * 变更资产状态
     */
    void changeAssetStatus(AssetStatusChangeRequest request);

    /**
     * 资产处置（清理/报废/出售/捐赠）
     * <p>
     * 生成处置凭证：借 固定资产清理(净值)、累计折旧；贷 固定资产(原值)；
     * 若有处置收入：借 银行存款，贷 固定资产清理；差额计入资产处置损益。
     * 处置后资产状态置为"已处置"。
     *
     * @param id             资产ID
     * @param disposeType    处置类型：1=清理 2=报废 3=出售 4=捐赠
     * @param disposeAmount  处置收入金额（出售/清理收入，无收入传0或null）
     * @param remark         备注
     * @return 生成的凭证ID
     */
    Long disposeAsset(Long id, Integer disposeType, BigDecimal disposeAmount, String remark);

    // ==================== 折旧管理 ====================

    /**
     * 分页查询折旧记录
     */
    PageResult<DepreciationRecordVO> pageDepreciationRecords(DepreciationRecordQueryRequest request);

    /**
     * 根据ID查询折旧记录
     */
    DepreciationRecordVO getDepreciationRecordById(Long id);

    /**
     * 计提折旧（批量处理所有在用资产）
     */
    void calculateDepreciation(DepreciationRequest request);

    /**
     * 生成折旧凭证
     */
    void generateDepreciationVoucher(Long recordId);

    /**
     * 批量生成折旧凭证
     */
    void batchGenerateDepreciationVoucher(DepreciationRequest request);

    /**
     * 获取资产报表
     *
     * @param accountSetId 账套ID
     * @param year          年度
     */
    AssetReportVO getAssetReport(Long accountSetId, Integer year);
}
