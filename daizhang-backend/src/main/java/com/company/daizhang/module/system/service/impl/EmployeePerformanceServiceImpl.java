package com.company.daizhang.module.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.biz.entity.ServiceTask;
import com.company.daizhang.module.biz.mapper.ServiceTaskMapper;
import com.company.daizhang.module.system.entity.EmployeePerformance;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.EmployeePerformanceMapper;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import com.company.daizhang.module.system.service.EmployeePerformanceService;
import com.company.daizhang.module.system.vo.EmployeePerformanceVO;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 员工绩效服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeePerformanceServiceImpl extends ServiceImpl<EmployeePerformanceMapper, EmployeePerformance> implements EmployeePerformanceService {

    private final SysUserMapper sysUserMapper;
    private final VoucherMapper voucherMapper;
    private final ServiceTaskMapper serviceTaskMapper;

    @Override
    public PageResult<EmployeePerformanceVO> pagePerformances(Long userId, Integer year, Integer month, int pageNum, int pageSize) {
        Page<EmployeePerformance> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EmployeePerformance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, EmployeePerformance::getUserId, userId)
               .eq(year != null, EmployeePerformance::getYear, year)
               .eq(month != null, EmployeePerformance::getMonth, month)
               .orderByDesc(EmployeePerformance::getYear)
               .orderByDesc(EmployeePerformance::getMonth)
               .orderByDesc(EmployeePerformance::getPerformanceScore);
        Page<EmployeePerformance> result = this.page(page, wrapper);
        List<EmployeePerformanceVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public EmployeePerformanceVO getPerformance(Long userId, Integer year, Integer month) {
        LambdaQueryWrapper<EmployeePerformance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmployeePerformance::getUserId, userId)
               .eq(EmployeePerformance::getYear, year)
               .eq(EmployeePerformance::getMonth, month);
        EmployeePerformance performance = this.getOne(wrapper);
        if (performance == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "绩效记录不存在");
        }
        return convertToVO(performance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generatePerformance(Integer year, Integer month) {
        // 查询所有在职用户
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getStatus, 1);
        List<SysUser> users = sysUserMapper.selectList(userWrapper);

        for (SysUser user : users) {
            // 凭证录入数：该用户该月录入的凭证数
            LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
            voucherWrapper.eq(Voucher::getCreateBy, user.getId())
                          .eq(Voucher::getYear, year)
                          .eq(Voucher::getMonth, month);
            Long voucherCount = voucherMapper.selectCount(voucherWrapper);

            // 审核数：该用户该月审核的凭证数
            LambdaQueryWrapper<Voucher> auditWrapper = new LambdaQueryWrapper<>();
            auditWrapper.eq(Voucher::getAuditBy, user.getId())
                        .eq(Voucher::getYear, year)
                        .eq(Voucher::getMonth, month);
            Long auditCount = voucherMapper.selectCount(auditWrapper);

            // 任务完成数：该用户该月完成的任务数（taskStatus=2表示已完成）
            LambdaQueryWrapper<ServiceTask> taskWrapper = new LambdaQueryWrapper<>();
            taskWrapper.eq(ServiceTask::getAssigneeId, user.getId())
                       .eq(ServiceTask::getYear, year)
                       .eq(ServiceTask::getMonth, month)
                       .eq(ServiceTask::getTaskStatus, 2);
            Long taskCompleteCount = serviceTaskMapper.selectCount(taskWrapper);

            // 服务客户数：该用户该月服务的不同客户数
            LambdaQueryWrapper<ServiceTask> customerWrapper = new LambdaQueryWrapper<>();
            customerWrapper.eq(ServiceTask::getAssigneeId, user.getId())
                           .eq(ServiceTask::getYear, year)
                           .eq(ServiceTask::getMonth, month)
                           .isNotNull(ServiceTask::getCustomerId);
            List<ServiceTask> tasks = serviceTaskMapper.selectList(customerWrapper);
            long customerCount = tasks.stream()
                    .map(ServiceTask::getCustomerId)
                    .filter(id -> id != null)
                    .distinct()
                    .count();

            // 绩效分数 = 凭证数*1 + 审核数*2 + 任务数*3
            BigDecimal score = BigDecimal.valueOf(voucherCount)
                    .add(BigDecimal.valueOf(auditCount).multiply(new BigDecimal("2")))
                    .add(BigDecimal.valueOf(taskCompleteCount).multiply(new BigDecimal("3")))
                    .setScale(2, RoundingMode.HALF_UP);

            // 保存或更新绩效记录
            LambdaQueryWrapper<EmployeePerformance> perfWrapper = new LambdaQueryWrapper<>();
            perfWrapper.eq(EmployeePerformance::getUserId, user.getId())
                        .eq(EmployeePerformance::getYear, year)
                        .eq(EmployeePerformance::getMonth, month);
            EmployeePerformance existing = this.getOne(perfWrapper);

            if (existing == null) {
                EmployeePerformance performance = new EmployeePerformance();
                performance.setUserId(user.getId());
                performance.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                performance.setYear(year);
                performance.setMonth(month);
                performance.setVoucherCount(voucherCount.intValue());
                performance.setAuditCount(auditCount.intValue());
                performance.setTaskCompleteCount(taskCompleteCount.intValue());
                performance.setCustomerCount((int) customerCount);
                performance.setPerformanceScore(score);
                this.save(performance);
            } else {
                existing.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                existing.setVoucherCount(voucherCount.intValue());
                existing.setAuditCount(auditCount.intValue());
                existing.setTaskCompleteCount(taskCompleteCount.intValue());
                existing.setCustomerCount((int) customerCount);
                existing.setPerformanceScore(score);
                this.updateById(existing);
            }
        }
        log.info("生成员工绩效完成，年度: {}, 月份: {}, 用户数: {}", year, month, users.size());
    }

    private EmployeePerformanceVO convertToVO(EmployeePerformance performance) {
        EmployeePerformanceVO vo = new EmployeePerformanceVO();
        BeanUtil.copyProperties(performance, vo);
        return vo;
    }
}
