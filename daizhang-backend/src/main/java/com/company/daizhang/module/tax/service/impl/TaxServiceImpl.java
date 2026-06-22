package com.company.daizhang.module.tax.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import com.company.daizhang.module.tax.dto.*;
import com.company.daizhang.module.tax.entity.TaxCalculation;
import com.company.daizhang.module.tax.entity.TaxDeclaration;
import com.company.daizhang.module.tax.mapper.TaxCalculationMapper;
import com.company.daizhang.module.tax.mapper.TaxDeclarationMapper;
import com.company.daizhang.module.tax.service.TaxService;
import com.company.daizhang.module.tax.vo.TaxCalculationVO;
import com.company.daizhang.module.tax.vo.TaxDeclarationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 税务服务实现
 */
@Service
@RequiredArgsConstructor
public class TaxServiceImpl extends ServiceImpl<TaxDeclarationMapper, TaxDeclaration> implements TaxService {

    private final TaxCalculationMapper taxCalculationMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<TaxDeclarationVO> pageDeclarations(TaxDeclarationQueryRequest request) {
        Page<TaxDeclaration> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<TaxDeclaration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaxDeclaration::getAccountSetId, request.getAccountSetId())
               .eq(request.getYear() != null, TaxDeclaration::getYear, request.getYear())
               .eq(request.getMonth() != null, TaxDeclaration::getMonth, request.getMonth())
               .eq(request.getTaxType() != null, TaxDeclaration::getTaxType, request.getTaxType())
               .eq(request.getStatus() != null, TaxDeclaration::getStatus, request.getStatus())
               .orderByDesc(TaxDeclaration::getCreateTime);

        Page<TaxDeclaration> result = this.page(page, wrapper);

        List<TaxDeclarationVO> voList = result.getRecords().stream()
                .map(this::convertDeclarationToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public TaxDeclarationVO getDeclarationById(Long id) {
        TaxDeclaration declaration = this.getById(id);
        if (declaration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "税务申报不存在");
        }
        return convertDeclarationToVO(declaration);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDeclaration(TaxDeclarationCreateRequest request) {
        TaxDeclaration declaration = new TaxDeclaration();
        BeanUtil.copyProperties(request, declaration);
        declaration.setStatus(0); // 未申报
        this.save(declaration);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDeclaration(Long id, TaxDeclarationUpdateRequest request) {
        TaxDeclaration declaration = this.getById(id);
        if (declaration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "税务申报不存在");
        }

        // 已缴纳的申报不能修改
        if (declaration.getStatus() != null && declaration.getStatus() == 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "已缴纳的申报不能修改");
        }

        BeanUtil.copyProperties(request, declaration);
        this.updateById(declaration);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDeclaration(Long id) {
        TaxDeclaration declaration = this.getById(id);
        if (declaration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "税务申报不存在");
        }

        // 只有未申报的才能删除
        if (declaration.getStatus() != null && declaration.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "只能删除未申报的记录");
        }

        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void declare(Long id) {
        TaxDeclaration declaration = this.getById(id);
        if (declaration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "税务申报不存在");
        }

        if (declaration.getStatus() != null && declaration.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "只能申报未申报状态的记录");
        }

        declaration.setStatus(1); // 已申报
        declaration.setDeclarationDate(LocalDate.now());
        declaration.setDeclaredAmount(declaration.getTaxAmount());
        this.updateById(declaration);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long id) {
        TaxDeclaration declaration = this.getById(id);
        if (declaration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "税务申报不存在");
        }

        if (declaration.getStatus() == null || declaration.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "只能缴纳已申报状态的记录");
        }

        declaration.setStatus(2); // 已缴纳
        declaration.setPaymentDate(LocalDate.now());
        declaration.setActualAmount(declaration.getDeclaredAmount());
        this.updateById(declaration);
    }

    @Override
    public PageResult<TaxCalculationVO> pageCalculations(TaxCalculationQueryRequest request) {
        Page<TaxCalculation> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<TaxCalculation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaxCalculation::getAccountSetId, request.getAccountSetId())
               .eq(request.getYear() != null, TaxCalculation::getYear, request.getYear())
               .eq(request.getMonth() != null, TaxCalculation::getMonth, request.getMonth())
               .eq(request.getTaxType() != null, TaxCalculation::getTaxType, request.getTaxType())
               .like(request.getCalculationItem() != null, TaxCalculation::getCalculationItem, request.getCalculationItem())
               .orderByDesc(TaxCalculation::getCreateTime);

        Page<TaxCalculation> result = taxCalculationMapper.selectPage(page, wrapper);

        List<TaxCalculationVO> voList = result.getRecords().stream()
                .map(this::convertCalculationToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public TaxCalculationVO getCalculationById(Long id) {
        TaxCalculation calculation = taxCalculationMapper.selectById(id);
        if (calculation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "税务计算不存在");
        }
        return convertCalculationToVO(calculation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCalculation(TaxCalculationCreateRequest request) {
        TaxCalculation calculation = new TaxCalculation();
        BeanUtil.copyProperties(request, calculation);
        taxCalculationMapper.insert(calculation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCalculation(Long id, TaxCalculationUpdateRequest request) {
        TaxCalculation calculation = taxCalculationMapper.selectById(id);
        if (calculation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "税务计算不存在");
        }

        BeanUtil.copyProperties(request, calculation);
        taxCalculationMapper.updateById(calculation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCalculation(Long id) {
        TaxCalculation calculation = taxCalculationMapper.selectById(id);
        if (calculation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "税务计算不存在");
        }
        taxCalculationMapper.deleteById(id);
    }

    @Override
    public BigDecimal calculateTax(Long accountSetId, Integer year, Integer month, String taxType) {
        // 查询该期间的税务计算记录
        LambdaQueryWrapper<TaxCalculation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaxCalculation::getAccountSetId, accountSetId)
               .eq(TaxCalculation::getYear, year)
               .eq(TaxCalculation::getMonth, month)
               .eq(TaxCalculation::getTaxType, taxType);

        List<TaxCalculation> calculations = taxCalculationMapper.selectList(wrapper);

        // 汇总税额
        BigDecimal totalTax = calculations.stream()
                .map(TaxCalculation::getTaxAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalTax;
    }

    /**
     * 税务申报实体转VO
     */
    private TaxDeclarationVO convertDeclarationToVO(TaxDeclaration declaration) {
        TaxDeclarationVO vo = new TaxDeclarationVO();
        BeanUtil.copyProperties(declaration, vo);

        // 查询创建人名称
        if (declaration.getCreateBy() != null) {
            SysUser createUser = sysUserMapper.selectById(declaration.getCreateBy());
            if (createUser != null) {
                vo.setCreateByName(createUser.getRealName() != null ? createUser.getRealName() : createUser.getUsername());
            }
        }

        return vo;
    }

    /**
     * 税务计算实体转VO
     */
    private TaxCalculationVO convertCalculationToVO(TaxCalculation calculation) {
        TaxCalculationVO vo = new TaxCalculationVO();
        BeanUtil.copyProperties(calculation, vo);

        // 查询创建人名称
        if (calculation.getCreateBy() != null) {
            SysUser createUser = sysUserMapper.selectById(calculation.getCreateBy());
            if (createUser != null) {
                vo.setCreateByName(createUser.getRealName() != null ? createUser.getRealName() : createUser.getUsername());
            }
        }

        return vo;
    }
}
