package com.company.daizhang.module.industrycommerce.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
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
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    private final AccountSetAccessService accountSetAccessService;

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

        // IDOR治理:仅返回当前用户可访问账套下客户的工商服务(实体无accountSetId,通过customer关联链过滤)
        Set<Long> accessibleIds = accountSetAccessService.listAccessibleAccountSetIds();
        if (accessibleIds != null) {
            if (accessibleIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, pageNum, pageSize);
            }
            List<Long> customerIds = listAccessibleCustomerIds(accessibleIds);
            if (customerIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, pageNum, pageSize);
            }
            wrapper.in(IndustryCommerceService::getCustomerId, customerIds);
        }

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
        // IDOR治理:校验当前用户对该工商服务所属账套的访问权
        accountSetAccessService.checkAccess(resolveAccountSetIdByCustomer(entity.getCustomerId()));
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
        // IDOR治理:校验当前用户对该客户所属账套的所有者权限(防止越权为他人账套客户创建工商服务)
        accountSetAccessService.checkOwner(customer.getAccountSetId());

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
        // IDOR治理:校验当前用户对该工商服务所属账套的所有者权限
        accountSetAccessService.checkOwner(resolveAccountSetIdByCustomer(entity.getCustomerId()));
        // 保存原状态,防止copyProperties覆盖serviceStatus绕过状态机(状态变更只能走assignService/completeService/cancelService)
        Integer originalStatus = entity.getServiceStatus();
        BeanUtil.copyProperties(request, entity);
        entity.setId(id);
        entity.setServiceStatus(originalStatus);
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
        // IDOR治理:校验当前用户对该工商服务所属账套的所有者权限
        accountSetAccessService.checkOwner(resolveAccountSetIdByCustomer(entity.getCustomerId()));
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
        // IDOR治理:校验当前用户对该工商服务所属账套的所有者权限
        accountSetAccessService.checkOwner(resolveAccountSetIdByCustomer(entity.getCustomerId()));
        if (assigneeId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "经办人ID不能为空");
        }
        // 仅"待派工(0)"可派工,防止已完成/已取消的任务被重新激活
        if (entity.getServiceStatus() == null || entity.getServiceStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许派工，仅待派工状态可派工");
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
        // IDOR治理:校验当前用户对该工商服务所属账套的所有者权限
        accountSetAccessService.checkOwner(resolveAccountSetIdByCustomer(entity.getCustomerId()));
        // 必须为"进行中(1)"才能完成,防止从待派工/已取消直接跳到完成绕过派工流程
        if (entity.getServiceStatus() == null || entity.getServiceStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许完成，仅进行中状态可完成");
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
        // IDOR治理:校验当前用户对该工商服务所属账套的所有者权限
        accountSetAccessService.checkOwner(resolveAccountSetIdByCustomer(entity.getCustomerId()));
        // 仅"待派工(0)/进行中(1)"可取消,已完成或已取消不允许
        if (entity.getServiceStatus() == null
                || entity.getServiceStatus() == 2
                || entity.getServiceStatus() == 3) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许取消");
        }
        entity.setServiceStatus(3);
        industryCommerceServiceMapper.updateById(entity);
        log.info("取消工商服务成功，服务ID: {}", id);
    }

    /**
     * IDOR治理:查询指定账套集合下的客户ID集合(用于实体无accountSetId时按customerId过滤)
     */
    private List<Long> listAccessibleCustomerIds(Set<Long> accessibleIds) {
        LambdaQueryWrapper<Customer> customerWrapper = new LambdaQueryWrapper<>();
        customerWrapper.in(Customer::getAccountSetId, accessibleIds);
        return customerMapper.selectList(customerWrapper).stream()
                .map(Customer::getId)
                .collect(Collectors.toList());
    }

    /**
     * IDOR治理关联链: service.customerId -> customer.accountSetId
     * 通过客户ID解析其所属账套ID,用于账套级权限校验
     */
    private Long resolveAccountSetIdByCustomer(Long customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "客户不存在");
        }
        return customer.getAccountSetId();
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
