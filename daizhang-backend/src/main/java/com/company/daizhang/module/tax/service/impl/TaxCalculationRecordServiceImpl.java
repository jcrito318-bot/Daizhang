package com.company.daizhang.module.tax.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import com.company.daizhang.module.tax.dto.TaxCalculationCreateRequest;
import com.company.daizhang.module.tax.dto.TaxCalculationQueryRequest;
import com.company.daizhang.module.tax.dto.TaxCalculationUpdateRequest;
import com.company.daizhang.module.tax.entity.TaxCalculation;
import com.company.daizhang.module.tax.mapper.TaxCalculationMapper;
import com.company.daizhang.module.tax.service.TaxCalculationRecordService;
import com.company.daizhang.module.tax.service.TaxCalculateService;
import com.company.daizhang.module.tax.vo.TaxCalculationDetailVO;
import com.company.daizhang.module.tax.vo.TaxCalculationResultVO;
import com.company.daizhang.module.tax.vo.TaxCalculationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 税务计算记录服务实现（管理tax_calculation表）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxCalculationRecordServiceImpl implements TaxCalculationRecordService {

    private final TaxCalculationMapper taxCalculationMapper;
    private final SysUserMapper sysUserMapper;
    private final TaxCalculateService taxCalculateService;

    @Override
    public PageResult<TaxCalculationVO> pageCalculations(TaxCalculationQueryRequest request) {
        Page<TaxCalculation> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<TaxCalculation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getAccountSetId() != null, TaxCalculation::getAccountSetId, request.getAccountSetId())
               .eq(request.getYear() != null, TaxCalculation::getYear, request.getYear())
               .eq(request.getMonth() != null, TaxCalculation::getMonth, request.getMonth())
               .like(request.getTaxType() != null, TaxCalculation::getTaxType, request.getTaxType())
               .like(request.getCalculationItem() != null, TaxCalculation::getCalculationItem, request.getCalculationItem())
               .orderByDesc(TaxCalculation::getCreateTime);

        Page<TaxCalculation> result = taxCalculationMapper.selectPage(page, wrapper);

        List<TaxCalculationVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public TaxCalculationVO getCalculationById(Long id) {
        TaxCalculation calculation = taxCalculationMapper.selectById(id);
        if (calculation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "税务计算记录不存在");
        }
        return convertToVO(calculation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCalculation(TaxCalculationCreateRequest request) {
        TaxCalculation calculation = new TaxCalculation();
        BeanUtil.copyProperties(request, calculation);
        taxCalculationMapper.insert(calculation);
        log.info("创建税务计算记录成功，ID: {}, 税种: {}, 项目: {}", calculation.getId(), calculation.getTaxType(), calculation.getCalculationItem());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCalculation(Long id, TaxCalculationUpdateRequest request) {
        TaxCalculation calculation = taxCalculationMapper.selectById(id);
        if (calculation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "税务计算记录不存在");
        }
        if (request.getTaxType() != null) {
            calculation.setTaxType(request.getTaxType());
        }
        if (request.getCalculationItem() != null) {
            calculation.setCalculationItem(request.getCalculationItem());
        }
        if (request.getAmount() != null) {
            calculation.setAmount(request.getAmount());
        }
        if (request.getRate() != null) {
            calculation.setRate(request.getRate());
        }
        if (request.getTaxAmount() != null) {
            calculation.setTaxAmount(request.getTaxAmount());
        }
        if (request.getRemark() != null) {
            calculation.setRemark(request.getRemark());
        }
        taxCalculationMapper.updateById(calculation);
        log.info("更新税务计算记录成功，ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCalculation(Long id) {
        TaxCalculation calculation = taxCalculationMapper.selectById(id);
        if (calculation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "税务计算记录不存在");
        }
        taxCalculationMapper.deleteById(id);
        log.info("删除税务计算记录成功，ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TaxCalculationResultVO> calculateTax(Long accountSetId, Integer year, Integer month) {
        // 调用税种自动计算服务获取计算结果
        List<TaxCalculationResultVO> results = taxCalculateService.calculateAllTaxes(accountSetId, year, month);

        // 先删除该期间已有的计算记录，再持久化最新计算结果
        LambdaQueryWrapper<TaxCalculation> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(TaxCalculation::getAccountSetId, accountSetId)
                     .eq(TaxCalculation::getYear, year)
                     .eq(TaxCalculation::getMonth, month);
        taxCalculationMapper.delete(deleteWrapper);

        // 持久化计算明细到 tax_calculation 表
        List<TaxCalculation> records = new ArrayList<>();
        for (TaxCalculationResultVO result : results) {
            if (result.getDetails() != null && !result.getDetails().isEmpty()) {
                for (TaxCalculationDetailVO detail : result.getDetails()) {
                    TaxCalculation calc = new TaxCalculation();
                    calc.setAccountSetId(accountSetId);
                    calc.setYear(year);
                    calc.setMonth(month);
                    calc.setTaxType(result.getTaxType());
                    calc.setCalculationItem(detail.getItemName());
                    calc.setAmount(detail.getAmount());
                    calc.setRate(detail.getRate());
                    calc.setTaxAmount(detail.getTaxAmount());
                    records.add(calc);
                }
            } else {
                // 无明细时保存汇总记录
                TaxCalculation calc = new TaxCalculation();
                calc.setAccountSetId(accountSetId);
                calc.setYear(year);
                calc.setMonth(month);
                calc.setTaxType(result.getTaxType());
                calc.setCalculationItem(result.getTaxTypeName());
                calc.setAmount(result.getTaxableAmount());
                calc.setRate(result.getTaxRate());
                calc.setTaxAmount(result.getTaxAmount());
                records.add(calc);
            }
        }
        for (TaxCalculation record : records) {
            taxCalculationMapper.insert(record);
        }
        log.info("税额自动计算并持久化成功，账套ID: {}-{}年{}月，保存明细{}条", accountSetId, year, month, records.size());
        return results;
    }

    // ==================== 辅助方法 ====================

    private TaxCalculationVO convertToVO(TaxCalculation calculation) {
        TaxCalculationVO vo = new TaxCalculationVO();
        BeanUtil.copyProperties(calculation, vo);
        // 填充创建人姓名
        if (calculation.getCreateBy() != null) {
            SysUser createUser = sysUserMapper.selectById(calculation.getCreateBy());
            if (createUser != null) {
                vo.setCreateByName(createUser.getRealName() != null ? createUser.getRealName() : createUser.getUsername());
            }
        }
        return vo;
    }
}
