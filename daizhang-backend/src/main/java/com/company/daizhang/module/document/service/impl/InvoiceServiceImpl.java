package com.company.daizhang.module.document.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.document.dto.InputInvoiceRequest;
import com.company.daizhang.module.document.dto.InvoiceQueryRequest;
import com.company.daizhang.module.document.dto.OutputInvoiceRequest;
import com.company.daizhang.module.document.entity.InputInvoice;
import com.company.daizhang.module.document.entity.OutputInvoice;
import com.company.daizhang.module.document.mapper.InputInvoiceMapper;
import com.company.daizhang.module.document.mapper.OutputInvoiceMapper;
import com.company.daizhang.module.document.service.InvoiceService;
import com.company.daizhang.module.document.vo.InputInvoiceVO;
import com.company.daizhang.module.document.vo.InvoiceStatisticsVO;
import com.company.daizhang.module.document.vo.OutputInvoiceVO;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 发票服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InputInvoiceMapper inputInvoiceMapper;
    private final OutputInvoiceMapper outputInvoiceMapper;
    private final SysUserMapper sysUserMapper;
    private final AccountSetAccessService accountSetAccessService;

    // ==================== 进项发票 ====================

    @Override
    public PageResult<InputInvoiceVO> pageInputInvoices(InvoiceQueryRequest request) {
        Page<InputInvoice> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<InputInvoice> wrapper = new LambdaQueryWrapper<>();
        // IDOR治理:校验当前用户对该账套的访问权
        applyAccountSetFilter(wrapper, InputInvoice::getAccountSetId, request.getAccountSetId());
        wrapper.like(StrUtil.isNotBlank(request.getInvoiceNumber()), InputInvoice::getInvoiceNumber, request.getInvoiceNumber())
               .eq(StrUtil.isNotBlank(request.getInvoiceType()), InputInvoice::getInvoiceType, request.getInvoiceType())
               .eq(request.getAuthStatus() != null, InputInvoice::getAuthStatus, request.getAuthStatus())
               .ge(request.getStartDate() != null, InputInvoice::getInvoiceDate, request.getStartDate())
               .le(request.getEndDate() != null, InputInvoice::getInvoiceDate, request.getEndDate())
               .orderByDesc(InputInvoice::getInvoiceDate);

        Page<InputInvoice> result = inputInvoiceMapper.selectPage(page, wrapper);

        List<InputInvoiceVO> voList = result.getRecords().stream()
                .map(this::convertToInputVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public InputInvoiceVO getInputInvoiceById(Long id) {
        InputInvoice invoice = inputInvoiceMapper.selectById(id);
        if (invoice == null) {
            throw new BusinessException(ErrorCode.INPUT_INVOICE_NOT_FOUND);
        }
        // IDOR治理:校验当前用户对该发票所属账套的访问权
        accountSetAccessService.checkAccess(invoice.getAccountSetId());
        return convertToInputVO(invoice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createInputInvoice(InputInvoiceRequest request) {
        // IDOR治理:校验当前用户对该账套的所有者权限,防止向他账套创建进项发票
        // (BV-02 修复:与 update/delete/authenticate 保持一致,创建时也需 checkOwner)
        accountSetAccessService.checkOwner(request.getAccountSetId());
        // 校验发票号码唯一
        checkInputInvoiceNumberDuplicate(request.getAccountSetId(), request.getInvoiceNumber(), null);

        InputInvoice invoice = new InputInvoice();
        BeanUtil.copyProperties(request, invoice);
        invoice.setAuthStatus(0);
        inputInvoiceMapper.insert(invoice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInputInvoice(Long id, InputInvoiceRequest request) {
        InputInvoice invoice = inputInvoiceMapper.selectById(id);
        if (invoice == null) {
            throw new BusinessException(ErrorCode.INPUT_INVOICE_NOT_FOUND);
        }
        // IDOR治理:校验当前用户对该发票所属账套的所有者权限
        accountSetAccessService.checkOwner(invoice.getAccountSetId());

        // 已认证或已作废的发票不允许修改
        if (invoice.getAuthStatus() != null && invoice.getAuthStatus() != 0) {
            throw new BusinessException(ErrorCode.INPUT_INVOICE_ALREADY_AUTHENTICATED);
        }

        // 校验发票号码唯一
        checkInputInvoiceNumberDuplicate(request.getAccountSetId(), request.getInvoiceNumber(), id);

        BeanUtil.copyProperties(request, invoice, "id", "accountSetId", "authStatus", "authDate", "voucherId");
        inputInvoiceMapper.updateById(invoice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInputInvoice(Long id) {
        InputInvoice invoice = inputInvoiceMapper.selectById(id);
        if (invoice == null) {
            throw new BusinessException(ErrorCode.INPUT_INVOICE_NOT_FOUND);
        }
        // IDOR治理:校验当前用户对该发票所属账套的所有者权限
        accountSetAccessService.checkOwner(invoice.getAccountSetId());
        // 已生成凭证的发票不可删除，否则产生孤儿凭证
        if (invoice.getVoucherId() != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "发票已生成凭证，不可删除/作废，请先红冲原凭证");
        }
        inputInvoiceMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void authenticateInputInvoice(Long id) {
        InputInvoice invoice = inputInvoiceMapper.selectById(id);
        if (invoice == null) {
            throw new BusinessException(ErrorCode.INPUT_INVOICE_NOT_FOUND);
        }
        // IDOR治理:校验当前用户对该发票所属账套的所有者权限
        accountSetAccessService.checkOwner(invoice.getAccountSetId());

        if (invoice.getAuthStatus() != null && invoice.getAuthStatus() == 1) {
            throw new BusinessException(ErrorCode.INPUT_INVOICE_ALREADY_AUTHENTICATED);
        }
        if (invoice.getAuthStatus() != null && invoice.getAuthStatus() == 2) {
            throw new BusinessException(ErrorCode.INPUT_INVOICE_ALREADY_VOID);
        }

        invoice.setAuthStatus(1);
        invoice.setAuthDate(LocalDate.now());
        inputInvoiceMapper.updateById(invoice);
    }

    // ==================== 销项发票 ====================

    @Override
    public PageResult<OutputInvoiceVO> pageOutputInvoices(InvoiceQueryRequest request) {
        Page<OutputInvoice> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<OutputInvoice> wrapper = new LambdaQueryWrapper<>();
        // IDOR治理:校验当前用户对该账套的访问权
        applyAccountSetFilter(wrapper, OutputInvoice::getAccountSetId, request.getAccountSetId());
        wrapper.like(StrUtil.isNotBlank(request.getInvoiceNumber()), OutputInvoice::getInvoiceNumber, request.getInvoiceNumber())
               .eq(StrUtil.isNotBlank(request.getInvoiceType()), OutputInvoice::getInvoiceType, request.getInvoiceType())
               .eq(request.getInvoiceStatus() != null, OutputInvoice::getInvoiceStatus, request.getInvoiceStatus())
               .ge(request.getStartDate() != null, OutputInvoice::getInvoiceDate, request.getStartDate())
               .le(request.getEndDate() != null, OutputInvoice::getInvoiceDate, request.getEndDate())
               .orderByDesc(OutputInvoice::getInvoiceDate);

        Page<OutputInvoice> result = outputInvoiceMapper.selectPage(page, wrapper);

        List<OutputInvoiceVO> voList = result.getRecords().stream()
                .map(this::convertToOutputVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public OutputInvoiceVO getOutputInvoiceById(Long id) {
        OutputInvoice invoice = outputInvoiceMapper.selectById(id);
        if (invoice == null) {
            throw new BusinessException(ErrorCode.OUTPUT_INVOICE_NOT_FOUND);
        }
        // IDOR治理:校验当前用户对该发票所属账套的访问权
        accountSetAccessService.checkAccess(invoice.getAccountSetId());
        return convertToOutputVO(invoice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOutputInvoice(OutputInvoiceRequest request) {
        // IDOR治理:校验当前用户对该账套的所有者权限,防止向他账套创建销项发票
        // (BV-03 修复:销项发票涉及应纳税额计算,越权创建会扭曲 VAT 申报数据)
        accountSetAccessService.checkOwner(request.getAccountSetId());
        // 校验发票号码唯一
        checkOutputInvoiceNumberDuplicate(request.getAccountSetId(), request.getInvoiceNumber(), null);

        OutputInvoice invoice = new OutputInvoice();
        BeanUtil.copyProperties(request, invoice);
        invoice.setInvoiceStatus(0);
        outputInvoiceMapper.insert(invoice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOutputInvoice(Long id, OutputInvoiceRequest request) {
        OutputInvoice invoice = outputInvoiceMapper.selectById(id);
        if (invoice == null) {
            throw new BusinessException(ErrorCode.OUTPUT_INVOICE_NOT_FOUND);
        }
        // IDOR治理:校验当前用户对该发票所属账套的所有者权限
        accountSetAccessService.checkOwner(invoice.getAccountSetId());

        // 已作废或已红冲的发票不允许修改
        if (invoice.getInvoiceStatus() != null && invoice.getInvoiceStatus() != 0) {
            throw new BusinessException(ErrorCode.OUTPUT_INVOICE_ALREADY_VOID);
        }

        // 校验发票号码唯一
        checkOutputInvoiceNumberDuplicate(request.getAccountSetId(), request.getInvoiceNumber(), id);

        BeanUtil.copyProperties(request, invoice, "id", "accountSetId", "invoiceStatus", "voucherId");
        outputInvoiceMapper.updateById(invoice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOutputInvoice(Long id) {
        OutputInvoice invoice = outputInvoiceMapper.selectById(id);
        if (invoice == null) {
            throw new BusinessException(ErrorCode.OUTPUT_INVOICE_NOT_FOUND);
        }
        // IDOR治理:校验当前用户对该发票所属账套的所有者权限
        accountSetAccessService.checkOwner(invoice.getAccountSetId());
        // 已生成凭证的发票不可删除，否则产生孤儿凭证
        if (invoice.getVoucherId() != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "发票已生成凭证，不可删除/作废，请先红冲原凭证");
        }
        outputInvoiceMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidOutputInvoice(Long id) {
        OutputInvoice invoice = outputInvoiceMapper.selectById(id);
        if (invoice == null) {
            throw new BusinessException(ErrorCode.OUTPUT_INVOICE_NOT_FOUND);
        }
        // IDOR治理:校验当前用户对该发票所属账套的所有者权限
        accountSetAccessService.checkOwner(invoice.getAccountSetId());
        // 已生成凭证的发票不可作废，否则产生孤儿凭证
        if (invoice.getVoucherId() != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "发票已生成凭证，不可删除/作废，请先红冲原凭证");
        }

        if (invoice.getInvoiceStatus() != null && invoice.getInvoiceStatus() == 1) {
            throw new BusinessException(ErrorCode.OUTPUT_INVOICE_ALREADY_VOID);
        }
        if (invoice.getInvoiceStatus() != null && invoice.getInvoiceStatus() == 2) {
            throw new BusinessException(ErrorCode.OUTPUT_INVOICE_ALREADY_RED);
        }

        invoice.setInvoiceStatus(1);
        outputInvoiceMapper.updateById(invoice);
    }

    // ==================== 统计 ====================

    @Override
    public InvoiceStatisticsVO getInvoiceStatistics(Long accountSetId, Integer year, Integer month) {
        InvoiceStatisticsVO stats = new InvoiceStatisticsVO();
        stats.setAccountSetId(accountSetId);
        stats.setYear(year);
        stats.setMonth(month);

        // 计算月份起止日期
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        // 进项发票统计（仅统计已认证的）
        LambdaQueryWrapper<InputInvoice> inputWrapper = new LambdaQueryWrapper<>();
        inputWrapper.eq(InputInvoice::getAccountSetId, accountSetId)
                    .ge(InputInvoice::getInvoiceDate, startDate)
                    .le(InputInvoice::getInvoiceDate, endDate);
        List<InputInvoice> inputInvoices = inputInvoiceMapper.selectList(inputWrapper);

        BigDecimal inputAmount = BigDecimal.ZERO;
        BigDecimal inputTaxAmount = BigDecimal.ZERO;
        BigDecimal inputTotalAmount = BigDecimal.ZERO;
        long authenticatedCount = 0;

        for (InputInvoice invoice : inputInvoices) {
            // 仅已认证的进项发票参与税额抵扣统计
            if (invoice.getAuthStatus() != null && invoice.getAuthStatus() == 1) {
                authenticatedCount++;
                inputAmount = inputAmount.add(nullToZero(invoice.getAmount()));
                inputTaxAmount = inputTaxAmount.add(nullToZero(invoice.getTaxAmount()));
                inputTotalAmount = inputTotalAmount.add(nullToZero(invoice.getTotalAmount()));
            }
        }

        stats.setInputInvoiceCount((long) inputInvoices.size());
        stats.setAuthenticatedInputCount(authenticatedCount);
        stats.setInputAmount(inputAmount);
        stats.setInputTaxAmount(inputTaxAmount);
        stats.setInputTotalAmount(inputTotalAmount);

        // 销项发票统计（仅统计正常的）
        LambdaQueryWrapper<OutputInvoice> outputWrapper = new LambdaQueryWrapper<>();
        outputWrapper.eq(OutputInvoice::getAccountSetId, accountSetId)
                     .ge(OutputInvoice::getInvoiceDate, startDate)
                     .le(OutputInvoice::getInvoiceDate, endDate);
        List<OutputInvoice> outputInvoices = outputInvoiceMapper.selectList(outputWrapper);

        BigDecimal outputAmount = BigDecimal.ZERO;
        BigDecimal outputTaxAmount = BigDecimal.ZERO;
        BigDecimal outputTotalAmount = BigDecimal.ZERO;
        long normalCount = 0;

        for (OutputInvoice invoice : outputInvoices) {
            // 仅正常的销项发票参与统计
            if (invoice.getInvoiceStatus() != null && invoice.getInvoiceStatus() == 0) {
                normalCount++;
                outputAmount = outputAmount.add(nullToZero(invoice.getAmount()));
                outputTaxAmount = outputTaxAmount.add(nullToZero(invoice.getTaxAmount()));
                outputTotalAmount = outputTotalAmount.add(nullToZero(invoice.getTotalAmount()));
            }
        }

        stats.setOutputInvoiceCount((long) outputInvoices.size());
        stats.setNormalOutputCount(normalCount);
        stats.setOutputAmount(outputAmount);
        stats.setOutputTaxAmount(outputTaxAmount);
        stats.setOutputTotalAmount(outputTotalAmount);

        // 应纳增值税 = 销项税额 - 进项税额
        stats.setVatPayable(outputTaxAmount.subtract(inputTaxAmount));

        return stats;
    }

    // ==================== 私有方法 ====================

    /**
     * 校验进项发票号码唯一
     */
    private void checkInputInvoiceNumberDuplicate(Long accountSetId, String invoiceNumber, Long excludeId) {
        LambdaQueryWrapper<InputInvoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InputInvoice::getAccountSetId, accountSetId)
               .eq(InputInvoice::getInvoiceNumber, invoiceNumber)
               .ne(excludeId != null, InputInvoice::getId, excludeId);
        Long count = inputInvoiceMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.INVOICE_NUMBER_DUPLICATE);
        }
    }

    /**
     * 校验销项发票号码唯一
     */
    private void checkOutputInvoiceNumberDuplicate(Long accountSetId, String invoiceNumber, Long excludeId) {
        LambdaQueryWrapper<OutputInvoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OutputInvoice::getAccountSetId, accountSetId)
               .eq(OutputInvoice::getInvoiceNumber, invoiceNumber)
               .ne(excludeId != null, OutputInvoice::getId, excludeId);
        Long count = outputInvoiceMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.INVOICE_NUMBER_DUPLICATE);
        }
    }

    /**
     * 进项发票实体转VO
     */
    private InputInvoiceVO convertToInputVO(InputInvoice invoice) {
        InputInvoiceVO vo = new InputInvoiceVO();
        BeanUtil.copyProperties(invoice, vo);

        if (invoice.getCreateBy() != null) {
            SysUser createUser = sysUserMapper.selectById(invoice.getCreateBy());
            if (createUser != null) {
                vo.setCreateByName(createUser.getRealName() != null ? createUser.getRealName() : createUser.getUsername());
            }
        }

        return vo;
    }

    /**
     * 销项发票实体转VO
     */
    private OutputInvoiceVO convertToOutputVO(OutputInvoice invoice) {
        OutputInvoiceVO vo = new OutputInvoiceVO();
        BeanUtil.copyProperties(invoice, vo);

        if (invoice.getCreateBy() != null) {
            SysUser createUser = sysUserMapper.selectById(invoice.getCreateBy());
            if (createUser != null) {
                vo.setCreateByName(createUser.getRealName() != null ? createUser.getRealName() : createUser.getUsername());
            }
        }

        return vo;
    }

    /**
     * null转0
     */
    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
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
