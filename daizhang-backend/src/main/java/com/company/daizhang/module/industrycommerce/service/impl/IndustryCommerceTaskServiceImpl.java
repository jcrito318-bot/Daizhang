package com.company.daizhang.module.industrycommerce.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.customer.entity.Customer;
import com.company.daizhang.module.customer.mapper.CustomerMapper;
import com.company.daizhang.module.industrycommerce.dto.IndustryCommerceTaskCreateRequest;
import com.company.daizhang.module.industrycommerce.dto.IndustryCommerceTaskUpdateRequest;
import com.company.daizhang.module.industrycommerce.entity.IndustryCommerceService;
import com.company.daizhang.module.industrycommerce.entity.IndustryCommerceTask;
import com.company.daizhang.module.industrycommerce.mapper.IndustryCommerceServiceMapper;
import com.company.daizhang.module.industrycommerce.mapper.IndustryCommerceTaskMapper;
import com.company.daizhang.module.industrycommerce.service.IndustryCommerceTaskService;
import com.company.daizhang.module.industrycommerce.vo.IndustryCommerceTaskVO;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工商外勤任务实现
 * <p>
 * P5 代账定位精简:工商年报已对接外部系统,本系统内默认关闭。
 * 通过 {@code app.module.industry-commerce.enabled=true} 启用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.module.industry-commerce.enabled", havingValue = "true", matchIfMissing = false)
public class IndustryCommerceTaskServiceImpl implements IndustryCommerceTaskService {

    private final IndustryCommerceTaskMapper industryCommerceTaskMapper;
    private final IndustryCommerceServiceMapper industryCommerceServiceMapper;
    private final SysUserMapper sysUserMapper;
    private final CustomerMapper customerMapper;
    private final AccountSetAccessService accountSetAccessService;

    @Override
    public List<IndustryCommerceTaskVO> listTasksByServiceId(Long serviceId) {
        if (serviceId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工商服务ID不能为空");
        }
        // IDOR治理:校验当前用户对该外勤任务所属账套的访问权(经service->customer关联链)
        accountSetAccessService.checkAccess(resolveAccountSetIdByService(serviceId));
        LambdaQueryWrapper<IndustryCommerceTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IndustryCommerceTask::getServiceId, serviceId)
               .orderByDesc(IndustryCommerceTask::getCreateTime);
        List<IndustryCommerceTask> list = industryCommerceTaskMapper.selectList(wrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(IndustryCommerceTaskCreateRequest request) {
        // 校验工商服务是否存在
        IndustryCommerceService service = industryCommerceServiceMapper.selectById(request.getServiceId());
        if (service == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工商服务不存在");
        }
        // IDOR治理:校验当前用户对该外勤任务所属账套的所有者权限(经service->customer关联链)
        accountSetAccessService.checkOwner(resolveAccountSetIdByService(service.getId()));

        IndustryCommerceTask entity = new IndustryCommerceTask();
        BeanUtil.copyProperties(request, entity);
        // 新建任务默认状态为待处理
        entity.setTaskStatus(0);
        industryCommerceTaskMapper.insert(entity);
        log.info("创建外勤任务成功，任务ID: {}, 工商服务ID: {}", entity.getId(), entity.getServiceId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTask(Long id, IndustryCommerceTaskUpdateRequest request) {
        IndustryCommerceTask entity = industryCommerceTaskMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "外勤任务不存在");
        }
        // IDOR治理:校验当前用户对该外勤任务所属账套的所有者权限(经service->customer关联链)
        accountSetAccessService.checkOwner(resolveAccountSetIdByService(entity.getServiceId()));
        // 保存原状态,防止copyProperties覆盖taskStatus绕过状态机(状态变更只能走assignTask/completeTask)
        Integer originalStatus = entity.getTaskStatus();
        BeanUtil.copyProperties(request, entity);
        entity.setId(id);
        entity.setTaskStatus(originalStatus);
        industryCommerceTaskMapper.updateById(entity);
        log.info("更新外勤任务成功，任务ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long id) {
        IndustryCommerceTask entity = industryCommerceTaskMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "外勤任务不存在");
        }
        // IDOR治理:校验当前用户对该外勤任务所属账套的所有者权限(经service->customer关联链)
        accountSetAccessService.checkOwner(resolveAccountSetIdByService(entity.getServiceId()));
        industryCommerceTaskMapper.deleteById(id);
        log.info("删除外勤任务成功，任务ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long id) {
        IndustryCommerceTask entity = industryCommerceTaskMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "外勤任务不存在");
        }
        // IDOR治理:校验当前用户对该外勤任务所属账套的所有者权限(经service->customer关联链)
        accountSetAccessService.checkOwner(resolveAccountSetIdByService(entity.getServiceId()));
        // 必须为"进行中(1)"才能完成,防止从待处理直接跳到完成绕过派工流程
        if (entity.getTaskStatus() == null || entity.getTaskStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许完成，仅进行中状态可完成");
        }
        entity.setTaskStatus(2);
        entity.setCompleteTime(LocalDateTime.now());
        industryCommerceTaskMapper.updateById(entity);
        log.info("完成外勤任务成功，任务ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTask(Long id, Long assigneeId) {
        IndustryCommerceTask entity = industryCommerceTaskMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "外勤任务不存在");
        }
        // IDOR治理:校验当前用户对该外勤任务所属账套的所有者权限(经service->customer关联链)
        accountSetAccessService.checkOwner(resolveAccountSetIdByService(entity.getServiceId()));
        if (assigneeId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "经办人ID不能为空");
        }
        // 仅"待处理(0)"可派工,防止已完成/已取消的任务被重新激活
        if (entity.getTaskStatus() == null || entity.getTaskStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许派工，仅待处理状态可派工");
        }
        entity.setAssigneeId(assigneeId);
        // 派工后状态置为进行中
        entity.setTaskStatus(1);
        industryCommerceTaskMapper.updateById(entity);
        log.info("外勤任务派工成功，任务ID: {}, 经办人ID: {}", id, assigneeId);
    }

    /**
     * IDOR治理关联链: task.serviceId -> service.customerId -> customer.accountSetId
     * 通过工商服务ID解析其所属账套ID,用于账套级权限校验
     */
    private Long resolveAccountSetIdByService(Long serviceId) {
        IndustryCommerceService service = industryCommerceServiceMapper.selectById(serviceId);
        if (service == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工商服务不存在");
        }
        Customer customer = customerMapper.selectById(service.getCustomerId());
        if (customer == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "客户不存在");
        }
        return customer.getAccountSetId();
    }

    /**
     * 外勤任务实体转VO，并填充经办人名称
     */
    private IndustryCommerceTaskVO convertToVO(IndustryCommerceTask entity) {
        IndustryCommerceTaskVO vo = new IndustryCommerceTaskVO();
        BeanUtil.copyProperties(entity, vo);
        if (entity.getAssigneeId() != null) {
            SysUser user = sysUserMapper.selectById(entity.getAssigneeId());
            if (user != null) {
                vo.setAssigneeName(user.getRealName());
            }
        }
        return vo;
    }
}
