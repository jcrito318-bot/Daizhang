package com.company.daizhang.module.salary.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.salary.dto.SpecialDeductionQueryRequest;
import com.company.daizhang.module.salary.dto.SpecialDeductionRequest;
import com.company.daizhang.module.salary.entity.Employee;
import com.company.daizhang.module.salary.entity.SpecialDeduction;
import com.company.daizhang.module.salary.mapper.EmployeeMapper;
import com.company.daizhang.module.salary.mapper.SpecialDeductionMapper;
import com.company.daizhang.module.salary.service.SpecialDeductionService;
import com.company.daizhang.module.salary.vo.SpecialDeductionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 个税专项附加扣除服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpecialDeductionServiceImpl implements SpecialDeductionService {

    private final SpecialDeductionMapper specialDeductionMapper;
    private final EmployeeMapper employeeMapper;
    private final AccountSetAccessService accountSetAccessService;

    @Override
    public PageResult<SpecialDeductionVO> pageDeductions(SpecialDeductionQueryRequest request) {
        Page<SpecialDeduction> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<SpecialDeduction> wrapper = new LambdaQueryWrapper<>();
        // IDOR治理:校验当前用户对该账套的访问权
        applyAccountSetFilter(wrapper, SpecialDeduction::getAccountSetId, request.getAccountSetId());
        wrapper.eq(request.getEmployeeId() != null, SpecialDeduction::getEmployeeId, request.getEmployeeId())
               .like(StrUtil.isNotBlank(request.getEmployeeName()), SpecialDeduction::getEmployeeName, request.getEmployeeName())
               .eq(StrUtil.isNotBlank(request.getDeductionType()), SpecialDeduction::getDeductionType, request.getDeductionType())
               .eq(request.getStatus() != null, SpecialDeduction::getStatus, request.getStatus())
               .orderByDesc(SpecialDeduction::getCreateTime);

        Page<SpecialDeduction> result = specialDeductionMapper.selectPage(page, wrapper);

        List<SpecialDeductionVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public SpecialDeductionVO getDeductionById(Long id) {
        SpecialDeduction deduction = specialDeductionMapper.selectById(id);
        if (deduction == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "专项附加扣除记录不存在");
        }
        return convertToVO(deduction);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDeduction(SpecialDeductionRequest request) {
        // 校验员工存在并填充员工姓名
        if (request.getEmployeeId() != null) {
            Employee employee = employeeMapper.selectById(request.getEmployeeId());
            if (employee == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "员工不存在");
            }
            if (StrUtil.isBlank(request.getEmployeeName())) {
                request.setEmployeeName(employee.getEmployeeName());
            }
        }

        SpecialDeduction deduction = new SpecialDeduction();
        BeanUtil.copyProperties(request, deduction);
        // 设置扣除项目名称
        if (StrUtil.isBlank(deduction.getDeductionName())) {
            deduction.setDeductionName(getDeductionTypeDesc(deduction.getDeductionType()));
        }
        // 默认生效
        if (deduction.getStatus() == null) {
            deduction.setStatus(1);
        }
        specialDeductionMapper.insert(deduction);
        log.info("创建专项附加扣除成功，ID: {}, 员工: {}, 项目: {}",
                deduction.getId(), deduction.getEmployeeName(), deduction.getDeductionName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDeduction(Long id, SpecialDeductionRequest request) {
        SpecialDeduction deduction = specialDeductionMapper.selectById(id);
        if (deduction == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "专项附加扣除记录不存在");
        }
        BeanUtil.copyProperties(request, deduction);
        // 重新填充扣除项目名称
        if (StrUtil.isBlank(deduction.getDeductionName())) {
            deduction.setDeductionName(getDeductionTypeDesc(deduction.getDeductionType()));
        }
        specialDeductionMapper.updateById(deduction);
        log.info("更新专项附加扣除成功，ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDeduction(Long id) {
        SpecialDeduction deduction = specialDeductionMapper.selectById(id);
        if (deduction == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "专项附加扣除记录不存在");
        }
        specialDeductionMapper.deleteById(id);
        log.info("删除专项附加扣除成功，ID: {}", id);
    }

    @Override
    public BigDecimal calculateMonthlyDeduction(Long employeeId, Integer year, Integer month) {
        if (employeeId == null) {
            return BigDecimal.ZERO;
        }
        LocalDate targetDate = LocalDate.of(year, month, 1);
        LocalDate periodEnd = YearMonth.of(year, month).atEndOfMonth();

        // 查询该员工该月生效的扣除项
        LambdaQueryWrapper<SpecialDeduction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpecialDeduction::getEmployeeId, employeeId)
               .eq(SpecialDeduction::getStatus, 1) // 生效中
               .le(SpecialDeduction::getEffectiveFrom, periodEnd);
        // effectiveTo 为空或 >= 该月起始
        wrapper.and(w -> w.isNull(SpecialDeduction::getEffectiveTo)
                          .or().ge(SpecialDeduction::getEffectiveTo, targetDate));

        List<SpecialDeduction> deductions = specialDeductionMapper.selectList(wrapper);

        // 累加月度扣除金额（大病医疗按月折算：年金额/12）
        BigDecimal total = BigDecimal.ZERO;
        for (SpecialDeduction d : deductions) {
            if ("SERIOUS_ILLNESS".equals(d.getDeductionType()) && d.getAnnualAmount() != null) {
                total = total.add(d.getAnnualAmount().divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP));
            } else if (d.getMonthlyAmount() != null) {
                total = total.add(d.getMonthlyAmount());
            }
        }
        return total;
    }

    // ==================== 辅助方法 ====================

    private SpecialDeductionVO convertToVO(SpecialDeduction deduction) {
        SpecialDeductionVO vo = new SpecialDeductionVO();
        BeanUtil.copyProperties(deduction, vo);
        vo.setDeductionTypeDesc(getDeductionTypeDesc(deduction.getDeductionType()));
        if (deduction.getStatus() != null) {
            vo.setStatusDesc(deduction.getStatus() == 1 ? "生效中" : "停用");
        }
        return vo;
    }

    /**
     * 获取扣除项目类型中文名称
     */
    private String getDeductionTypeDesc(String type) {
        if (type == null) return "";
        switch (type) {
            case "CHILDREN_EDUCATION": return "子女教育";
            case "CONTINUING_EDUCATION": return "继续教育";
            case "SERIOUS_ILLNESS": return "大病医疗";
            case "HOUSING_LOAN": return "住房贷款利息";
            case "HOUSING_RENT": return "住房租金";
            case "SUPPORT_ELDERLY": return "赡养老人";
            case "INFANT_CARE": return "3岁以下婴幼儿照护";
            default: return type;
        }
    }

    /**
     * 分页/列表查询的账套访问过滤(IDOR治理):
     * - accountSetId 非空: checkAccess 校验后按该账套精确过滤
     * - accountSetId 为空: 按当前用户可访问账套集合过滤(超级管理员返回null表示不限制;
     *   空集合表示无权限,注入永不命中条件避免 MyBatis-Plus 对空集合in跳过导致越权)
     */
    private <T> void applyAccountSetFilter(LambdaQueryWrapper<T> wrapper,
                                           SFunction<T, Long> accountSetIdColumn,
                                           Long accountSetId) {
        if (accountSetId != null) {
            accountSetAccessService.checkAccess(accountSetId);
            wrapper.eq(accountSetIdColumn, accountSetId);
            return;
        }
        Set<Long> accessibleIds = accountSetAccessService.listAccessibleAccountSetIds();
        if (accessibleIds == null) {
            return;
        }
        if (accessibleIds.isEmpty()) {
            wrapper.eq(accountSetIdColumn, -1L);
            return;
        }
        wrapper.in(accountSetIdColumn, accessibleIds);
    }
}
