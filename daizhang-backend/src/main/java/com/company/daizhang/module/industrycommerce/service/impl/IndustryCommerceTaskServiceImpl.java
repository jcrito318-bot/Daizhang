package com.company.daizhang.module.industrycommerce.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工商外勤任务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndustryCommerceTaskServiceImpl implements IndustryCommerceTaskService {

    private final IndustryCommerceTaskMapper industryCommerceTaskMapper;
    private final IndustryCommerceServiceMapper industryCommerceServiceMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public List<IndustryCommerceTaskVO> listTasksByServiceId(Long serviceId) {
        if (serviceId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工商服务ID不能为空");
        }
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
        BeanUtil.copyProperties(request, entity);
        entity.setId(id);
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
        // 不能重复完成
        if (entity.getTaskStatus() != null && entity.getTaskStatus() == 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "外勤任务已完成，不能重复完成");
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
        if (assigneeId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "经办人ID不能为空");
        }
        entity.setAssigneeId(assigneeId);
        // 派工后状态置为进行中
        entity.setTaskStatus(1);
        industryCommerceTaskMapper.updateById(entity);
        log.info("外勤任务派工成功，任务ID: {}, 经办人ID: {}", id, assigneeId);
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
