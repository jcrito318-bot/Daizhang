package com.company.daizhang.module.industrycommerce.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.industrycommerce.dto.IndustryCommerceServiceCreateRequest;
import com.company.daizhang.module.industrycommerce.dto.IndustryCommerceServiceQueryRequest;
import com.company.daizhang.module.industrycommerce.dto.IndustryCommerceServiceUpdateRequest;
import com.company.daizhang.module.industrycommerce.vo.IndustryCommerceServiceVO;

/**
 * 工商服务接口
 */
public interface IndustryCommerceServiceService {

    /**
     * 分页查询工商服务
     */
    PageResult<IndustryCommerceServiceVO> pageServices(IndustryCommerceServiceQueryRequest request);

    /**
     * 根据ID查询工商服务详情（含外勤任务列表）
     */
    IndustryCommerceServiceVO getServiceById(Long id);

    /**
     * 创建工商服务（serviceStatus默认0待派工）
     *
     * @return 工商服务ID
     */
    Long createService(IndustryCommerceServiceCreateRequest request);

    /**
     * 更新工商服务
     */
    void updateService(Long id, IndustryCommerceServiceUpdateRequest request);

    /**
     * 删除工商服务
     */
    void deleteService(Long id);

    /**
     * 派工（serviceStatus置为1进行中）
     */
    void assignService(Long id, Long assigneeId);

    /**
     * 完成服务（serviceStatus置为2已完成，actualCompleteDate=LocalDate.now()）
     */
    void completeService(Long id);

    /**
     * 取消服务（serviceStatus置为3已取消）
     */
    void cancelService(Long id);
}
