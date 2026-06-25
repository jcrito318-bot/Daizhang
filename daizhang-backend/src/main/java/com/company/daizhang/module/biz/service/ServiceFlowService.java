package com.company.daizhang.module.biz.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.biz.entity.ServiceFlowNode;
import com.company.daizhang.module.biz.entity.ServiceTask;
import com.company.daizhang.module.biz.vo.EmployeeWorkloadVO;
import com.company.daizhang.module.biz.vo.ServiceFlowNodeVO;
import com.company.daizhang.module.biz.vo.ServiceTaskVO;

import java.util.List;

/**
 * 代账服务流程服务接口
 */
public interface ServiceFlowService extends IService<ServiceFlowNode> {

    /**
     * 查询流程节点列表
     */
    List<ServiceFlowNodeVO> listNodes();

    /**
     * 创建流程节点
     */
    void createNode(ServiceFlowNode entity);

    /**
     * 更新流程节点
     */
    void updateNode(ServiceFlowNode entity);

    /**
     * 分页查询任务
     */
    PageResult<ServiceTaskVO> pageTasks(Long accountSetId, Integer year, Integer month, Integer taskStatus,
                                        Integer pageNum, Integer pageSize);

    /**
     * 创建任务
     */
    void createTask(ServiceTask entity);

    /**
     * 更新任务
     */
    void updateTask(ServiceTask entity);

    /**
     * 分配任务
     */
    void assignTask(Long id, Long assigneeId, String assigneeName);

    /**
     * 完成任务
     */
    void completeTask(Long id);

    /**
     * 根据ID获取任务
     */
    ServiceTask getTaskById(Long id);

    /**
     * 删除任务
     */
    void deleteTask(Long id);

    /**
     * 员工工作负荷统计
     * 按assigneeId分组统计任务数、逾期数、按时完成率、平均完成时长
     *
     * @param year  年（可选，null查全部）
     * @param month 月（可选，null查全年）
     * @return 各员工工作负荷统计列表
     */
    List<EmployeeWorkloadVO> getEmployeeWorkload(Integer year, Integer month);
}
