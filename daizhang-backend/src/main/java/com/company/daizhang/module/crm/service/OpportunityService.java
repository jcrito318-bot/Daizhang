package com.company.daizhang.module.crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.crm.dto.OpportunityQueryRequest;
import com.company.daizhang.module.crm.dto.OpportunityRequest;
import com.company.daizhang.module.crm.entity.Opportunity;
import com.company.daizhang.module.crm.vo.OpportunityStatisticsVO;
import com.company.daizhang.module.crm.vo.OpportunityVO;

/**
 * 商机服务接口
 */
public interface OpportunityService extends IService<Opportunity> {

    /**
     * 分页查询商机
     */
    PageResult<OpportunityVO> pageOpportunities(OpportunityQueryRequest request);

    /**
     * 根据ID查询商机
     */
    OpportunityVO getOpportunityById(Long id);

    /**
     * 创建商机
     */
    void createOpportunity(OpportunityRequest request);

    /**
     * 更新商机
     */
    void updateOpportunity(Long id, OpportunityRequest request);

    /**
     * 删除商机
     */
    void deleteOpportunity(Long id);

    /**
     * 变更阶段
     */
    void changeStage(Long id, String stage);

    /**
     * 统计
     */
    OpportunityStatisticsVO getStatistics();
}
