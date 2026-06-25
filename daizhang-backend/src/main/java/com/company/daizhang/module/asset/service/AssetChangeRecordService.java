package com.company.daizhang.module.asset.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.asset.dto.AssetChangeRecordRequest;
import com.company.daizhang.module.asset.vo.AssetChangeRecordVO;

import java.util.List;

/**
 * 资产变动记录服务接口
 */
public interface AssetChangeRecordService {

    /**
     * 分页查询资产变动记录
     */
    PageResult<AssetChangeRecordVO> pageRecords(Long accountSetId, Long assetId, String changeType, int pageNum, int pageSize);

    /**
     * 根据资产ID查询变动记录列表
     */
    List<AssetChangeRecordVO> listByAssetId(Long assetId);

    /**
     * 创建资产变动记录
     */
    void createRecord(AssetChangeRecordRequest request);

    /**
     * 删除资产变动记录
     */
    void deleteRecord(Long id);
}
