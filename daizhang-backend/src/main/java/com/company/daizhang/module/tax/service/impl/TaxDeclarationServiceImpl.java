package com.company.daizhang.module.tax.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import com.company.daizhang.module.tax.dto.TaxDeclarationCreateRequest;
import com.company.daizhang.module.tax.dto.TaxDeclarationQueryRequest;
import com.company.daizhang.module.tax.dto.TaxDeclarationUpdateRequest;
import com.company.daizhang.module.tax.entity.TaxDeclaration;
import com.company.daizhang.module.tax.mapper.TaxDeclarationMapper;
import com.company.daizhang.module.tax.service.TaxDeclarationService;
import com.company.daizhang.module.tax.vo.TaxDeclarationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 税务申报记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxDeclarationServiceImpl implements TaxDeclarationService {

    private final TaxDeclarationMapper taxDeclarationMapper;
    private final SysUserMapper sysUserMapper;
    private final AccountSetAccessService accountSetAccessService;

    @Override
    public PageResult<TaxDeclarationVO> pageDeclarations(TaxDeclarationQueryRequest request) {
        Page<TaxDeclaration> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<TaxDeclaration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getAccountSetId() != null, TaxDeclaration::getAccountSetId, request.getAccountSetId())
               .eq(request.getYear() != null, TaxDeclaration::getYear, request.getYear())
               .eq(request.getMonth() != null, TaxDeclaration::getMonth, request.getMonth())
               .like(request.getTaxType() != null, TaxDeclaration::getTaxType, request.getTaxType())
               .eq(request.getStatus() != null, TaxDeclaration::getStatus, request.getStatus())
               .orderByDesc(TaxDeclaration::getCreateTime);

        Page<TaxDeclaration> result = taxDeclarationMapper.selectPage(page, wrapper);

        List<TaxDeclarationVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public TaxDeclarationVO getDeclarationById(Long id) {
        TaxDeclaration declaration = taxDeclarationMapper.selectById(id);
        if (declaration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "税务申报记录不存在");
        }
        // IDOR治理:校验当前用户对该申报记录所属账套的访问权
        accountSetAccessService.checkAccess(declaration.getAccountSetId());
        return convertToVO(declaration);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDeclaration(TaxDeclarationCreateRequest request) {
        TaxDeclaration declaration = new TaxDeclaration();
        BeanUtil.copyProperties(request, declaration);
        // 默认未申报状态
        if (declaration.getStatus() == null) {
            declaration.setStatus(0);
        }
        taxDeclarationMapper.insert(declaration);
        log.info("创建税务申报记录成功，ID: {}, 税种: {}", declaration.getId(), declaration.getTaxType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDeclaration(Long id, TaxDeclarationUpdateRequest request) {
        TaxDeclaration declaration = taxDeclarationMapper.selectById(id);
        if (declaration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "税务申报记录不存在");
        }
        // IDOR治理:校验当前用户对该申报记录所属账套的所有者权限
        accountSetAccessService.checkOwner(declaration.getAccountSetId());
        // 已缴纳的记录不允许修改
        if (declaration.getStatus() != null && declaration.getStatus() == 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "已缴纳的申报记录不允许修改");
        }
        if (request.getTaxType() != null) {
            declaration.setTaxType(request.getTaxType());
        }
        if (request.getTaxableAmount() != null) {
            declaration.setTaxableAmount(request.getTaxableAmount());
        }
        if (request.getTaxRate() != null) {
            declaration.setTaxRate(request.getTaxRate());
        }
        if (request.getTaxAmount() != null) {
            declaration.setTaxAmount(request.getTaxAmount());
        }
        if (request.getDeclaredAmount() != null) {
            declaration.setDeclaredAmount(request.getDeclaredAmount());
        }
        if (request.getActualAmount() != null) {
            declaration.setActualAmount(request.getActualAmount());
        }
        // 禁止通过通用更新接口修改status,状态变更必须走 declare()/pay() 专用方法,
        // 否则可绕过状态机直接把"未申报(0)"改为"已缴纳(2)",漏设申报/缴纳日期
        if (request.getDeclarationDate() != null) {
            declaration.setDeclarationDate(request.getDeclarationDate());
        }
        if (request.getPaymentDate() != null) {
            declaration.setPaymentDate(request.getPaymentDate());
        }
        if (request.getRemark() != null) {
            declaration.setRemark(request.getRemark());
        }
        taxDeclarationMapper.updateById(declaration);
        log.info("更新税务申报记录成功，ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDeclaration(Long id) {
        TaxDeclaration declaration = taxDeclarationMapper.selectById(id);
        if (declaration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "税务申报记录不存在");
        }
        // IDOR治理:校验当前用户对该申报记录所属账套的所有者权限
        accountSetAccessService.checkOwner(declaration.getAccountSetId());
        // 已申报或已缴纳的记录不允许删除
        if (declaration.getStatus() != null && declaration.getStatus() > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "已申报或已缴纳的记录不允许删除");
        }
        taxDeclarationMapper.deleteById(id);
        log.info("删除税务申报记录成功，ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void declare(Long id) {
        TaxDeclaration declaration = taxDeclarationMapper.selectById(id);
        if (declaration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "税务申报记录不存在");
        }
        // IDOR治理:校验当前用户对该申报记录所属账套的所有者权限(执行申报)
        accountSetAccessService.checkOwner(declaration.getAccountSetId());
        if (declaration.getStatus() != null && declaration.getStatus() >= 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该记录已申报，不能重复申报");
        }
        declaration.setStatus(1);
        declaration.setDeclarationDate(LocalDate.now());
        // 已申报金额默认等于应纳税额
        if (declaration.getDeclaredAmount() == null && declaration.getTaxAmount() != null) {
            declaration.setDeclaredAmount(declaration.getTaxAmount());
        }
        taxDeclarationMapper.updateById(declaration);
        log.info("执行申报成功，ID: {}, 税种: {}", id, declaration.getTaxType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long id, BigDecimal actualAmount) {
        TaxDeclaration declaration = taxDeclarationMapper.selectById(id);
        if (declaration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "税务申报记录不存在");
        }
        // IDOR治理:校验当前用户对该申报记录所属账套的所有者权限(缴款)
        accountSetAccessService.checkOwner(declaration.getAccountSetId());
        if (declaration.getStatus() == null || declaration.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该记录未申报，不能缴款");
        }
        if (declaration.getStatus() == 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该记录已缴纳");
        }
        declaration.setStatus(2);
        declaration.setPaymentDate(LocalDate.now());
        if (actualAmount != null) {
            declaration.setActualAmount(actualAmount);
        } else if (declaration.getActualAmount() == null && declaration.getTaxAmount() != null) {
            declaration.setActualAmount(declaration.getTaxAmount());
        }
        taxDeclarationMapper.updateById(declaration);
        log.info("执行缴款成功，ID: {}, 税种: {}, 实缴金额: {}", id, declaration.getTaxType(), declaration.getActualAmount());
    }

    // ==================== 辅助方法 ====================

    private TaxDeclarationVO convertToVO(TaxDeclaration declaration) {
        TaxDeclarationVO vo = new TaxDeclarationVO();
        BeanUtil.copyProperties(declaration, vo);
        // 填充创建人姓名
        if (declaration.getCreateBy() != null) {
            SysUser createUser = sysUserMapper.selectById(declaration.getCreateBy());
            if (createUser != null) {
                vo.setCreateByName(createUser.getRealName() != null ? createUser.getRealName() : createUser.getUsername());
            }
        }
        return vo;
    }
}
