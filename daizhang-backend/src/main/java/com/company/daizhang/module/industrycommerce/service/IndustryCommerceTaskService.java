package com.company.daizhang.module.industrycommerce.service;

import com.company.daizhang.module.industrycommerce.dto.IndustryCommerceTaskCreateRequest;
import com.company.daizhang.module.industrycommerce.dto.IndustryCommerceTaskUpdateRequest;
import com.company.daizhang.module.industrycommerce.vo.IndustryCommerceTaskVO;

import java.util.List;

/**
 * 工商外勤任务接口
 */
public interface IndustryCommerceTaskService {

    /**
     * 根据工商服务ID查询外勤任务列表
     */
    List<IndustryCommerceTaskVO> listTasksByServiceId(Long serviceId);

    /**
     * 创建外勤任务（taskStatus默认0待处理）
     *
     * @return 外勤任务ID
     */
    Long createTask(IndustryCommerceTaskCreateRequest request);

    /**
     * 更新外勤任务
     */
    void updateTask(Long id, IndustryCommerceTaskUpdateRequest request);

    /**
     * 删除外勤任务
     */
    void deleteTask(Long id);

    /**
     * 完成外勤任务（taskStatus=2已完成, completeTime=LocalDateTime.now()）
     */
    void completeTask(Long id);

    /**
     * 派工（taskStatus置为1进行中）
     */
    void assignTask(Long id, Long assigneeId);
}
