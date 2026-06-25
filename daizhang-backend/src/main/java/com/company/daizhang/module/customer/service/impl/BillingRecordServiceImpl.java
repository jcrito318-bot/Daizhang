package com.company.daizhang.module.customer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.customer.dto.BillingRecordCreateRequest;
import com.company.daizhang.module.customer.dto.BillingRecordQueryRequest;
import com.company.daizhang.module.customer.dto.BillingRecordUpdateRequest;
import com.company.daizhang.module.customer.entity.BillingRecord;
import com.company.daizhang.module.customer.mapper.BillingRecordMapper;
import com.company.daizhang.module.customer.service.BillingRecordService;
import com.company.daizhang.module.customer.vo.BillingRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 客户开票记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRecordServiceImpl implements BillingRecordService {

    private final BillingRecordMapper billingRecordMapper;

    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.06");

    @Override
    public PageResult<BillingRecordVO> pageBillingRecords(BillingRecordQueryRequest request) {
        Page<BillingRecord> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<BillingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getCustomerId() != null, BillingRecord::getCustomerId, request.getCustomerId())
                .eq(request.getContractId() != null, BillingRecord::getContractId, request.getContractId())
                .eq(request.getInvoiceType() != null, BillingRecord::getInvoiceType, request.getInvoiceType())
                .eq(request.getStatus() != null, BillingRecord::getStatus, request.getStatus())
                .like(StrUtil.isNotBlank(request.getInvoiceNo()), BillingRecord::getInvoiceNo, request.getInvoiceNo())
                .orderByDesc(BillingRecord::getBillingDate);

        Page<BillingRecord> result = billingRecordMapper.selectPage(page, wrapper);
        List<BillingRecordVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public BillingRecordVO getBillingRecordById(Long id) {
        BillingRecord record = billingRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "开票记录不存在");
        }
        return convertToVO(record);
    }

    @Override
    public List<BillingRecordVO> listBillingRecordsByCustomerId(Long customerId) {
        LambdaQueryWrapper<BillingRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BillingRecord::getCustomerId, customerId)
                .orderByDesc(BillingRecord::getBillingDate);
        return billingRecordMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBillingRecord(BillingRecordCreateRequest request) {
        BillingRecord record = new BillingRecord();
        BeanUtil.copyProperties(request, record);

        // 计算税额和不含税金额
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;
        BigDecimal taxRate = request.getTaxRate() != null ? request.getTaxRate() : DEFAULT_TAX_RATE;
        record.setTaxRate(taxRate);

        BigDecimal taxAmount = amount.multiply(taxRate)
                .divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.HALF_UP);
        BigDecimal amountWithoutTax = amount.subtract(taxAmount);

        record.setTaxAmount(taxAmount);
        record.setAmountWithoutTax(amountWithoutTax);
        record.setStatus(0); // 已开票未收款

        billingRecordMapper.insert(record);
        log.info("创建客户开票记录成功，客户ID={}, 发票号={}, 金额={}", request.getCustomerId(), request.getInvoiceNo(), amount);
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBillingRecord(Long id, BillingRecordUpdateRequest request) {
        BillingRecord record = billingRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "开票记录不存在");
        }
        if (record.getStatus() != null && record.getStatus() == 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "已作废的开票记录不能更新");
        }

        BeanUtil.copyProperties(request, record);

        // 如果金额或税率变更，重新计算税额
        if (request.getAmount() != null || request.getTaxRate() != null) {
            BigDecimal amount = request.getAmount() != null ? request.getAmount() : record.getAmount();
            BigDecimal taxRate = request.getTaxRate() != null ? request.getTaxRate() : record.getTaxRate();
            if (taxRate == null) {
                taxRate = DEFAULT_TAX_RATE;
            }
            record.setTaxRate(taxRate);
            BigDecimal taxAmount = amount.multiply(taxRate)
                    .divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.HALF_UP);
            record.setTaxAmount(taxAmount);
            record.setAmountWithoutTax(amount.subtract(taxAmount));
        }

        billingRecordMapper.updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBillingRecord(Long id) {
        BillingRecord record = billingRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "开票记录不存在");
        }
        if (record.getStatus() != null && record.getStatus() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "已收款的开票记录不能删除");
        }
        billingRecordMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidBillingRecord(Long id) {
        BillingRecord record = billingRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "开票记录不存在");
        }
        if (record.getStatus() != null && record.getStatus() == 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "开票记录已作废");
        }
        record.setStatus(2); // 已作废
        billingRecordMapper.updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsPaid(Long id, Long paymentRecordId) {
        BillingRecord record = billingRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "开票记录不存在");
        }
        if (record.getStatus() != null && record.getStatus() == 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "已作废的开票记录不能标记为已收款");
        }
        record.setStatus(1); // 已收款
        record.setPaymentRecordId(paymentRecordId);
        billingRecordMapper.updateById(record);
    }

    private BillingRecordVO convertToVO(BillingRecord record) {
        BillingRecordVO vo = new BillingRecordVO();
        BeanUtil.copyProperties(record, vo);
        vo.setInvoiceTypeDesc(convertInvoiceTypeDesc(record.getInvoiceType()));
        vo.setStatusDesc(convertStatusDesc(record.getStatus()));
        return vo;
    }

    private String convertInvoiceTypeDesc(Integer type) {
        if (type == null) return "";
        switch (type) {
            case 1: return "增值税专用发票";
            case 2: return "增值税普通发票";
            case 3: return "电子普通发票";
            default: return String.valueOf(type);
        }
    }

    private String convertStatusDesc(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "已开票未收款";
            case 1: return "已收款";
            case 2: return "已作废";
            default: return String.valueOf(status);
        }
    }
}
