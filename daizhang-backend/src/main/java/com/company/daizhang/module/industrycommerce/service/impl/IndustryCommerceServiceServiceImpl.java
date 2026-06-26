package com.company.daizhang.module.industrycommerce.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.customer.entity.Customer;
import com.company.daizhang.module.customer.mapper.CustomerMapper;
import com.company.daizhang.module.industrycommerce.dto.IndustryCommerceServiceCreateRequest;
import com.company.daizhang.module.industrycommerce.dto.IndustryCommerceServiceQueryRequest;
import com.company.daizhang.module.industrycommerce.dto.IndustryCommerceServiceUpdateRequest;
import com.company.daizhang.module.industrycommerce.entity.IndustryCommerceService;
import com.company.daizhang.module.industrycommerce.entity.IndustryCommerceTask;
import com.company.daizhang.module.industrycommerce.mapper.IndustryCommerceServiceMapper;
import com.company.daizhang.module.industrycommerce.mapper.IndustryCommerceTaskMapper;
import com.company.daizhang.module.industrycommerce.service.IndustryCommerceServiceService;
import com.company.daizhang.module.industrycommerce.vo.IndustryCommerceServiceVO;
import com.company.daizhang.module.industrycommerce.vo.IndustryCommerceTaskVO;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工商服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndustryCommerceServiceServiceImpl implements IndustryCommerceServiceService {

    private final IndustryCommerceServiceMapper industryCommerceServiceMapper;
    private final IndustryCommerceTaskMapper industryCommerceTaskMapper;
    private final CustomerMapper customerMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<IndustryCommerceServiceVO> pageServices(IndustryCommerceServiceQueryRequest request) {
        Integer pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        Integer pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();

        Page<IndustryCommerceService> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<IndustryCommerceService> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getCustomerId() != null, IndustryCommerceService::getCustomerId, request.getCustomerId())
               .eq(request.getServiceType() != null, IndustryCommerceService::getServiceType, request.getServiceType())
               .eq(request.getServiceStatus() != null, IndustryCommerceService::getServiceStatus, request.getServiceStatus())
               .eq(request.getAssigneeId() != null, IndustryCommerceService::getAssigneeId, request.getAssigneeId())
               .orderByDesc(IndustryCommerceService::getCreateTime);

        Page<IndustryCommerceService> result = industryCommerceServiceMapper.selectPage(page, wrapper);
        List<IndustryCommerceServiceVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public IndustryCommerceServiceVO getServiceById(Long id) {
        IndustryCommerceService entity = industryCommerceServiceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工商服务不存在");
        }
        IndustryCommerceServiceVO vo = convertToVO(entity);

        // 查询并组装外勤任务列表
        LambdaQueryWrapper<IndustryCommerceTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IndustryCommerceTask::getServiceId, id)
               .orderByDesc(IndustryCommerceTask::getCreateTime);
        List<IndustryCommerceTask> tasks = industryCommerceTaskMapper.selectList(wrapper);
        List<IndustryCommerceTaskVO> taskVOList = tasks.stream()
                .map(this::convertTaskToVO)
                .collect(Collectors.toList());
        vo.setTaskList(taskVOList);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createService(IndustryCommerceServiceCreateRequest request) {
        // 校验客户是否存在
        Customer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "客户不存在");
        }

        IndustryCommerceService entity = new IndustryCommerceService();
        BeanUtil.copyProperties(request, entity);
        // 新建工商服务默认状态为待派工
        entity.setServiceStatus(0);
        // 金额默认值处理
        if (entity.getCostAmount() == null) {
            entity.setCostAmount(BigDecimal.ZERO);
        }
        if (entity.getServiceAmount() == null) {
            entity.setServiceAmount(BigDecimal.ZERO);
        }
        industryCommerceServiceMapper.insert(entity);
        log.info("创建工商服务成功，服务ID: {}, 客户ID: {}", entity.getId(), entity.getCustomerId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateService(Long id, IndustryCommerceServiceUpdateRequest request) {
        IndustryCommerceService entity = industryCommerceServiceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工商服务不存在");
        }
        BeanUtil.copyProperties(request, entity);
        entity.setId(id);
        industryCommerceServiceMapper.updateById(entity);
        log.info("更新工商服务成功，服务ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteService(Long id) {
        IndustryCommerceService entity = industryCommerceServiceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工商服务不存在");
        }
        industryCommerceServiceMapper.deleteById(id);
        log.info("删除工商服务成功，服务ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignService(Long id, Long assigneeId) {
        IndustryCommerceService entity = industryCommerceServiceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工商服务不存在");
        }
        if (assigneeId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "经办人ID不能为空");
        }
        entity.setAssigneeId(assigneeId);
        // 派工后状态置为进行中
        entity.setServiceStatus(1);
        industryCommerceServiceMapper.updateById(entity);
        log.info("工商服务派工成功，服务ID: {}, 经办人ID: {}", id, assigneeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeService(Long id) {
        IndustryCommerceService entity = industryCommerceServiceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工商服务不存在");
        }
        // 不能重复完成
        if (entity.getServiceStatus() != null && entity.getServiceStatus() == 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工商服务已完成，不能重复完成");
        }
        entity.setServiceStatus(2);
        entity.setActualCompleteDate(LocalDate.now());
        industryCommerceServiceMapper.updateById(entity);
        log.info("完成工商服务成功，服务ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelService(Long id) {
        IndustryCommerceService entity = industryCommerceServiceMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工商服务不存在");
        }
        entity.setServiceStatus(3);
        industryCommerceServiceMapper.updateById(entity);
        log.info("取消工商服务成功，服务ID: {}", id);
    }

    /**
     * 工商服务实体转VO，并填充客户名称和经办人名称
     */
    private IndustryCommerceServiceVO convertToVO(IndustryCommerceService entity) {
        IndustryCommerceServiceVO vo = new IndustryCommerceServiceVO();
        BeanUtil.copyProperties(entity, vo);
        // 填充客户名称
        if (entity.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(entity.getCustomerId());
            if (customer != null) {
                vo.setCustomerName(customer.getCustomerName());
            }
        }
        // 填充经办人名称
        if (entity.getAssigneeId() != null) {
            SysUser user = sysUserMapper.selectById(entity.getAssigneeId());
            if (user != null) {
                vo.setAssigneeName(user.getRealName());
            }
        }
        return vo;
    }

    /**
     * 外勤任务实体转VO，并填充经办人名称
     */
    private IndustryCommerceTaskVO convertTaskToVO(IndustryCommerceTask entity) {
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
