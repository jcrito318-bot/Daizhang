package com.company.daizhang.module.biz.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.biz.entity.ServiceFlowNode;
import com.company.daizhang.module.biz.entity.ServiceTask;
import com.company.daizhang.module.biz.mapper.ServiceFlowNodeMapper;
import com.company.daizhang.module.biz.mapper.ServiceTaskMapper;
import com.company.daizhang.module.biz.service.ServiceFlowService;
import com.company.daizhang.module.biz.vo.EmployeeWorkloadVO;
import com.company.daizhang.module.biz.vo.ServiceFlowNodeVO;
import com.company.daizhang.module.biz.vo.ServiceTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 代账服务流程服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceFlowServiceImpl extends ServiceImpl<ServiceFlowNodeMapper, ServiceFlowNode> implements ServiceFlowService {

    private final ServiceTaskMapper serviceTaskMapper;
    private final AccountSetAccessService accountSetAccessService;

    @Override
    public List<ServiceFlowNodeVO> listNodes() {
        LambdaQueryWrapper<ServiceFlowNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ServiceFlowNode::getSortOrder);
        List<ServiceFlowNode> list = this.list(wrapper);
        return list.stream()
                .map(this::convertNodeToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNode(ServiceFlowNode entity) {
        // 业务校验：节点编码不能为空
        if (StrUtil.isBlank(entity.getNodeCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "节点编码不能为空");
        }
        // 业务校验：节点名称不能为空
        if (StrUtil.isBlank(entity.getNodeName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "节点名称不能为空");
        }
        // 检查节点编码是否已存在
        LambdaQueryWrapper<ServiceFlowNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ServiceFlowNode::getNodeCode, entity.getNodeCode());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "节点编码已存在");
        }
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        this.save(entity);
        log.info("创建流程节点成功，节点编码: {}, 节点名称: {}", entity.getNodeCode(), entity.getNodeName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNode(ServiceFlowNode entity) {
        if (entity.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "节点ID不能为空");
        }
        ServiceFlowNode existing = this.getById(entity.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "流程节点不存在");
        }
        // 如果修改了节点编码，检查是否与其他节点冲突
        if (StrUtil.isNotBlank(entity.getNodeCode()) && !entity.getNodeCode().equals(existing.getNodeCode())) {
            LambdaQueryWrapper<ServiceFlowNode> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ServiceFlowNode::getNodeCode, entity.getNodeCode());
            wrapper.ne(ServiceFlowNode::getId, entity.getId());
            if (this.count(wrapper) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "节点编码已存在");
            }
        }
        this.updateById(entity);
        log.info("更新流程节点成功，节点ID: {}", entity.getId());
    }

    @Override
    public PageResult<ServiceTaskVO> pageTasks(Long accountSetId, Integer year, Integer month, Integer taskStatus,
                                               Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        Page<ServiceTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ServiceTask> wrapper = new LambdaQueryWrapper<>();
        // IDOR治理:显式指定账套时校验访问权并按该账套过滤;未指定时按当前用户可见账套集合过滤
        // (admin返回null表示不限制;普通用户返回其可访问账套ID集合)
        Set<Long> accessibleIds = accountSetAccessService.listAccessibleAccountSetIds();
        if (accountSetId != null) {
            accountSetAccessService.checkAccess(accountSetId);
            wrapper.eq(ServiceTask::getAccountSetId, accountSetId);
        } else if (accessibleIds != null) {
            if (accessibleIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, pageNum, pageSize);
            }
            wrapper.in(ServiceTask::getAccountSetId, accessibleIds);
        }
        wrapper.eq(year != null, ServiceTask::getYear, year)
               .eq(month != null, ServiceTask::getMonth, month)
               .eq(taskStatus != null, ServiceTask::getTaskStatus, taskStatus)
               .orderByDesc(ServiceTask::getCreateTime);
        Page<ServiceTask> result = serviceTaskMapper.selectPage(page, wrapper);
        List<ServiceTaskVO> voList = result.getRecords().stream()
                .map(this::convertTaskToVO)
                .collect(Collectors.toList());
        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTask(ServiceTask entity) {
        // 业务校验：账套ID不能为空
        if (entity.getAccountSetId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }
        // IDOR治理:校验当前用户对该账套的所有者权限
        accountSetAccessService.checkOwner(entity.getAccountSetId());
        // 业务校验：年度不能为空
        if (entity.getYear() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "年度不能为空");
        }
        // 业务校验：月份不能为空
        if (entity.getMonth() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "月份不能为空");
        }
        // 业务校验：流程节点ID不能为空
        if (entity.getNodeId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "流程节点ID不能为空");
        }
        // 业务校验：年度必须合理
        if (entity.getYear() < 1900 || entity.getYear() > 2099) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "年度格式不正确");
        }
        // 业务校验：月份必须在1-12之间
        if (entity.getMonth() < 1 || entity.getMonth() > 12) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "月份必须在1-12之间");
        }
        // 校验流程节点是否存在，并填充节点名称
        ServiceFlowNode node = this.getById(entity.getNodeId());
        if (node == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "流程节点不存在");
        }
        if (StrUtil.isBlank(entity.getNodeName())) {
            entity.setNodeName(node.getNodeName());
        }
        if (entity.getTaskStatus() == null) {
            entity.setTaskStatus(0);
        }
        serviceTaskMapper.insert(entity);
        log.info("创建服务任务成功，账套ID: {}, 年度: {}, 月份: {}, 节点: {}",
                entity.getAccountSetId(), entity.getYear(), entity.getMonth(), entity.getNodeName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTask(ServiceTask entity) {
        if (entity.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }
        ServiceTask existing = serviceTaskMapper.selectById(entity.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "服务任务不存在");
        }
        // IDOR治理:校验当前用户对该任务所属账套的所有者权限
        accountSetAccessService.checkOwner(existing.getAccountSetId());
        // 保护状态字段:updateTask不允许直接修改taskStatus和完成时间,
        // 状态变更必须走 assignTask/completeTask 专用方法,否则可绕过状态机任意穿越状态
        entity.setTaskStatus(existing.getTaskStatus());
        entity.setCompleteTime(existing.getCompleteTime());
        // 如果更新了节点，同步更新节点名称
        if (entity.getNodeId() != null && !entity.getNodeId().equals(existing.getNodeId())) {
            ServiceFlowNode node = this.getById(entity.getNodeId());
            if (node == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "流程节点不存在");
            }
            if (StrUtil.isBlank(entity.getNodeName())) {
                entity.setNodeName(node.getNodeName());
            }
        }
        serviceTaskMapper.updateById(entity);
        log.info("更新服务任务成功，任务ID: {}", entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTask(Long id, Long assigneeId, String assigneeName) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }
        ServiceTask existing = serviceTaskMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "服务任务不存在");
        }
        // IDOR治理:校验当前用户对该任务所属账套的所有者权限
        accountSetAccessService.checkOwner(existing.getAccountSetId());
        // 已完成任务不允许重新指派,避免产生"已完成但assignee是新人"的矛盾数据影响工时统计
        if (existing.getTaskStatus() != null && existing.getTaskStatus() == 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已完成任务不允许重新指派");
        }
        existing.setAssigneeId(assigneeId);
        existing.setAssigneeName(assigneeName);
        // 分配后状态置为进行中
        if (existing.getTaskStatus() == null || existing.getTaskStatus() == 0) {
            existing.setTaskStatus(1);
        }
        serviceTaskMapper.updateById(existing);
        log.info("分配服务任务成功，任务ID: {}, 指派人: {}", id, assigneeName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }
        ServiceTask existing = serviceTaskMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "服务任务不存在");
        }
        // IDOR治理:校验当前用户对该任务所属账套的所有者权限
        accountSetAccessService.checkOwner(existing.getAccountSetId());
        // 不能重复完成
        if (existing.getTaskStatus() != null && existing.getTaskStatus() == 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务已完成，不能重复完成");
        }
        existing.setTaskStatus(2);
        existing.setCompleteTime(LocalDateTime.now());
        serviceTaskMapper.updateById(existing);
        log.info("完成服务任务成功，任务ID: {}", id);
    }

    @Override
    public ServiceTask getTaskById(Long id) {
        ServiceTask task = serviceTaskMapper.selectById(id);
        if (task != null) {
            // IDOR治理:校验当前用户对该任务所属账套的访问权
            accountSetAccessService.checkAccess(task.getAccountSetId());
        }
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务ID不能为空");
        }
        ServiceTask existing = serviceTaskMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "服务任务不存在");
        }
        // IDOR治理:校验当前用户对该任务所属账套的所有者权限
        accountSetAccessService.checkOwner(existing.getAccountSetId());
        serviceTaskMapper.deleteById(id);
        log.info("删除服务任务成功，任务ID: {}", id);
    }

    @Override
    public List<EmployeeWorkloadVO> getEmployeeWorkload(Integer year, Integer month) {
        // 查询任务（按年/月过滤）
        LambdaQueryWrapper<ServiceTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(year != null, ServiceTask::getYear, year)
                .eq(month != null, ServiceTask::getMonth, month)
                .isNotNull(ServiceTask::getAssigneeId);
        // IDOR治理:按当前用户可访问账套集合过滤(admin返回null表示不限制)
        Set<Long> accessibleIds = accountSetAccessService.listAccessibleAccountSetIds();
        if (accessibleIds != null) {
            if (accessibleIds.isEmpty()) {
                return new ArrayList<>();
            }
            wrapper.in(ServiceTask::getAccountSetId, accessibleIds);
        }
        List<ServiceTask> tasks = serviceTaskMapper.selectList(wrapper);

        if (tasks.isEmpty()) {
            return new ArrayList<>();
        }

        // 按assigneeId分组
        Map<Long, List<ServiceTask>> taskByAssignee = tasks.stream()
                .collect(Collectors.groupingBy(ServiceTask::getAssigneeId));

        // 逾期阈值：创建时间超过7天且未完成
        LocalDateTime overdueThreshold = LocalDateTime.now().minusDays(7);

        List<EmployeeWorkloadVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<ServiceTask>> entry : taskByAssignee.entrySet()) {
            Long assigneeId = entry.getKey();
            List<ServiceTask> taskList = entry.getValue();

            EmployeeWorkloadVO vo = new EmployeeWorkloadVO();
            vo.setEmployeeId(assigneeId);
            // 取员工姓名（从第一个任务）
            String name = taskList.stream()
                    .map(ServiceTask::getAssigneeName)
                    .filter(StrUtil::isNotBlank)
                    .findFirst()
                    .orElse("");
            vo.setEmployeeName(name);

            int total = taskList.size();
            int pending = 0;
            int inProgress = 0;
            int completed = 0;
            int overdue = 0;
            int overdueCompleted = 0; // 已完成但耗时超过阈值(影响按时完成率)
            long totalCompleteHours = 0;
            int completedWithTime = 0;

            for (ServiceTask t : taskList) {
                Integer status = t.getTaskStatus();
                if (status != null) {
                    switch (status) {
                        case 0: pending++; break;
                        case 1: inProgress++; break;
                        case 2: completed++; break;
                    }
                }
                // 逾期判定：未完成（status<2）且创建时间早于阈值
                if (status == null || status < 2) {
                    if (t.getCreateTime() != null && t.getCreateTime().isBefore(overdueThreshold)) {
                        overdue++;
                    }
                }
                // 完成时长统计
                if (status != null && status == 2 && t.getCompleteTime() != null && t.getCreateTime() != null) {
                    long hours = Duration.between(t.getCreateTime(), t.getCompleteTime()).toHours();
                    totalCompleteHours += hours;
                    completedWithTime++;
                    // 已完成但耗时超过7天阈值,计为逾期完成(影响按时完成率)
                    if (hours > 7 * 24) {
                        overdueCompleted++;
                    }
                }
            }

            vo.setTotalTaskCount(total);
            vo.setPendingTaskCount(pending);
            vo.setInProgressTaskCount(inProgress);
            vo.setCompletedTaskCount(completed);
            vo.setOverdueTaskCount(overdue);

            // 按时完成率 = (已完成 - 逾期完成) / 已完成 * 100
            // 原公式用overdue(仅含未完成逾期),导致overdue<=pending+inProgress恒成立,Math.max恒为0,onTimeRate恒100%
            if (completed > 0) {
                double onTimeRate = (double) (completed - overdueCompleted) / completed * 100;
                vo.setOnTimeRate(Math.max(0, Math.round(onTimeRate * 100) / 100.0));
            } else {
                vo.setOnTimeRate(0.0);
            }

            // 平均完成时长（小时）
            if (completedWithTime > 0) {
                vo.setAvgCompleteHours(Math.round((double) totalCompleteHours / completedWithTime * 100) / 100.0);
            } else {
                vo.setAvgCompleteHours(0.0);
            }

            result.add(vo);
        }

        // 按总任务数降序
        result.sort((a, b) -> Integer.compare(b.getTotalTaskCount(), a.getTotalTaskCount()));
        return result;
    }

    private ServiceFlowNodeVO convertNodeToVO(ServiceFlowNode node) {
        ServiceFlowNodeVO vo = new ServiceFlowNodeVO();
        BeanUtil.copyProperties(node, vo);
        return vo;
    }

    private ServiceTaskVO convertTaskToVO(ServiceTask task) {
        ServiceTaskVO vo = new ServiceTaskVO();
        BeanUtil.copyProperties(task, vo);
        return vo;
    }
}
