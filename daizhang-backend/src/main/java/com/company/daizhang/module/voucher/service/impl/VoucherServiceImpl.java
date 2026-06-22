package com.company.daizhang.module.voucher.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import com.company.daizhang.module.voucher.dto.VoucherCreateRequest;
import com.company.daizhang.module.voucher.dto.VoucherDetailRequest;
import com.company.daizhang.module.voucher.dto.VoucherQueryRequest;
import com.company.daizhang.module.voucher.dto.VoucherUpdateRequest;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.entity.VoucherDetail;
import com.company.daizhang.module.voucher.entity.VoucherWord;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import com.company.daizhang.module.voucher.mapper.VoucherWordMapper;
import com.company.daizhang.module.voucher.service.VoucherService;
import com.company.daizhang.module.voucher.vo.VoucherDetailVO;
import com.company.daizhang.module.voucher.vo.VoucherVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 凭证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements VoucherService {

    private final VoucherDetailMapper voucherDetailMapper;
    private final VoucherWordMapper voucherWordMapper;
    private final AccountPeriodMapper accountPeriodMapper;
    private final SysUserMapper sysUserMapper;
    private final SubjectMapper subjectMapper;

    @Override
    public PageResult<VoucherVO> pageVouchers(VoucherQueryRequest request) {
        Page<Voucher> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Voucher::getAccountSetId, request.getAccountSetId())
               .eq(request.getYear() != null, Voucher::getYear, request.getYear())
               .eq(request.getMonth() != null, Voucher::getMonth, request.getMonth())
               .eq(request.getStatus() != null, Voucher::getStatus, request.getStatus())
               .like(StrUtil.isNotBlank(request.getVoucherNo()), Voucher::getVoucherNo, request.getVoucherNo())
               .ge(request.getStartDate() != null, Voucher::getVoucherDate, request.getStartDate())
               .le(request.getEndDate() != null, Voucher::getVoucherDate, request.getEndDate())
               .orderByDesc(Voucher::getCreateTime);

        Page<Voucher> result = this.page(page, wrapper);

        List<VoucherVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public VoucherVO getVoucherById(Long id) {
        Voucher voucher = this.getById(id);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        VoucherVO vo = convertToVO(voucher);

        // 查询凭证明细
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(VoucherDetail::getVoucherId, id)
                     .orderByAsc(VoucherDetail::getSortOrder);
        List<VoucherDetail> details = voucherDetailMapper.selectList(detailWrapper);

        List<VoucherDetailVO> detailVOs = details.stream()
                .map(this::convertDetailToVO)
                .collect(Collectors.toList());
        vo.setDetails(detailVOs);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVoucher(VoucherCreateRequest request) {
        // 业务校验：凭证日期不能为空
        if (request.getVoucherDate() == null) {
            throw new BusinessException(ErrorCode.VOUCHER_DATE_BLANK);
        }
        
        // 业务校验：年度不能为空
        if (request.getYear() == null) {
            throw new BusinessException(ErrorCode.VOUCHER_YEAR_BLANK);
        }
        
        // 业务校验：月份不能为空
        if (request.getMonth() == null) {
            throw new BusinessException(ErrorCode.VOUCHER_MONTH_BLANK);
        }
        
        // 业务校验：年度必须合理（1900-2099）
        if (request.getYear() < 1900 || request.getYear() > 2099) {
            throw new BusinessException(ErrorCode.VOUCHER_YEAR_INVALID);
        }
        
        // 业务校验：月份必须在1-12之间
        if (request.getMonth() < 1 || request.getMonth() > 12) {
            throw new BusinessException(ErrorCode.VOUCHER_MONTH_INVALID);
        }
        
        // 业务校验：凭证明细不能为空
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new BusinessException(ErrorCode.VOUCHER_DETAIL_EMPTY);
        }
        
        // 检查会计期间是否存在
        AccountPeriod period = checkPeriodExists(request.getAccountSetId(), request.getYear(), request.getMonth());
        
        // 业务校验：凭证日期必须在会计期间范围内
        validateVoucherDateInRange(request.getVoucherDate(), period.getStartDate(), period.getEndDate());
        
        // 检查会计期间是否已结账
        checkPeriodNotClosed(period);
        
        // 校验凭证明细并计算借贷合计
        BigDecimal[] totals = validateDetailsAndGetTotals(request.getDetails());
        BigDecimal totalDebit = totals[0];
        BigDecimal totalCredit = totals[1];

        // 验证借贷平衡
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessException(ErrorCode.VOUCHER_BALANCE_ERROR);
        }
        
        // 验证借贷合计不能为零
        if (totalDebit.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(ErrorCode.VOUCHER_DEBIT_CREDIT_BOTH_ZERO);
        }

        // 生成凭证号
        String voucherNo = generateVoucherNo(request.getAccountSetId(), request.getYear(), request.getMonth());

        // 保存凭证
        Voucher voucher = new Voucher();
        BeanUtil.copyProperties(request, voucher);
        voucher.setVoucherNo(voucherNo);
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucher.setStatus(0);
        voucher.setSource(0);
        this.save(voucher);

        // 保存凭证明细
        saveDetails(voucher.getId(), request.getDetails());
        
        log.info("创建凭证成功，凭证号: {}, 借贷合计: {}", voucherNo, totalDebit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVoucher(Long id, VoucherUpdateRequest request) {
        Voucher voucher = this.getById(id);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        // 只有未审核的凭证才能修改
        if (voucher.getStatus() != 0) {
            throw new BusinessException(ErrorCode.VOUCHER_ALREADY_AUDITED);
        }
        
        // 业务校验：凭证明细不能为空
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new BusinessException(ErrorCode.VOUCHER_DETAIL_EMPTY);
        }
        
        // 如果更新了日期或期间，需要校验
        Integer year = request.getYear() != null ? request.getYear() : voucher.getYear();
        Integer month = request.getMonth() != null ? request.getMonth() : voucher.getMonth();
        LocalDate voucherDate = request.getVoucherDate() != null ? request.getVoucherDate() : voucher.getVoucherDate();
        
        // 业务校验：年度必须合理
        if (year < 1900 || year > 2099) {
            throw new BusinessException(ErrorCode.VOUCHER_YEAR_INVALID);
        }
        
        // 业务校验：月份必须在1-12之间
        if (month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.VOUCHER_MONTH_INVALID);
        }
        
        // 检查会计期间是否存在
        AccountPeriod period = checkPeriodExists(voucher.getAccountSetId(), year, month);
        
        // 业务校验：凭证日期必须在会计期间范围内
        validateVoucherDateInRange(voucherDate, period.getStartDate(), period.getEndDate());
        
        // 检查会计期间是否已结账
        checkPeriodNotClosed(period);

        // 校验凭证明细并计算借贷合计
        BigDecimal[] totals = validateDetailsAndGetTotals(request.getDetails());
        BigDecimal totalDebit = totals[0];
        BigDecimal totalCredit = totals[1];

        // 验证借贷平衡
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessException(ErrorCode.VOUCHER_BALANCE_ERROR);
        }
        
        // 验证借贷合计不能为零
        if (totalDebit.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(ErrorCode.VOUCHER_DEBIT_CREDIT_BOTH_ZERO);
        }

        // 更新凭证
        if (request.getVoucherWordId() != null) {
            voucher.setVoucherWordId(request.getVoucherWordId());
        }
        if (request.getVoucherDate() != null) {
            voucher.setVoucherDate(request.getVoucherDate());
        }
        if (request.getYear() != null) {
            voucher.setYear(request.getYear());
        }
        if (request.getMonth() != null) {
            voucher.setMonth(request.getMonth());
        }
        if (request.getAttachmentCount() != null) {
            voucher.setAttachmentCount(request.getAttachmentCount());
        }
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        this.updateById(voucher);

        // 删除旧明细
        LambdaQueryWrapper<VoucherDetail> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(VoucherDetail::getVoucherId, id);
        voucherDetailMapper.delete(deleteWrapper);

        // 保存新明细
        saveDetails(id, request.getDetails());
        
        log.info("更新凭证成功，凭证ID: {}, 凭证号: {}", id, voucher.getVoucherNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVoucher(Long id) {
        Voucher voucher = this.getById(id);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        // 只有未审核的凭证才能删除
        if (voucher.getStatus() != 0) {
            throw new BusinessException(ErrorCode.VOUCHER_ALREADY_AUDITED);
        }

        // 删除凭证明细
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(VoucherDetail::getVoucherId, id);
        voucherDetailMapper.delete(detailWrapper);

        // 删除凭证
        this.removeById(id);
        
        // 重新编号该期间的凭证（保证连续性）
        renumberVouchers(voucher.getAccountSetId(), voucher.getYear(), voucher.getMonth());
        
        log.info("删除凭证成功，凭证ID: {}, 凭证号: {}", id, voucher.getVoucherNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditVoucher(Long id) {
        Voucher voucher = this.getById(id);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        // 不能重复审核
        if (voucher.getStatus() != 0) {
            throw new BusinessException(ErrorCode.VOUCHER_ALREADY_AUDITED);
        }
        
        // 业务校验：审核前再次验证借贷平衡
        if (voucher.getTotalDebit().compareTo(voucher.getTotalCredit()) != 0) {
            throw new BusinessException(ErrorCode.VOUCHER_BALANCE_ERROR);
        }

        voucher.setStatus(1);
        voucher.setAuditBy(SecurityUtils.getCurrentUserId());
        voucher.setAuditTime(LocalDateTime.now());
        this.updateById(voucher);
        
        log.info("审核凭证成功，凭证ID: {}, 凭证号: {}", id, voucher.getVoucherNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unauditVoucher(Long id) {
        Voucher voucher = this.getById(id);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        // 只有已审核且未过账的凭证才能反审核
        if (voucher.getStatus() != 1) {
            throw new BusinessException(ErrorCode.VOUCHER_ALREADY_POSTED);
        }

        voucher.setStatus(0);
        voucher.setAuditBy(null);
        voucher.setAuditTime(null);
        this.updateById(voucher);
        
        log.info("反审核凭证成功，凭证ID: {}, 凭证号: {}", id, voucher.getVoucherNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postVoucher(Long id) {
        Voucher voucher = this.getById(id);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        // 只有已审核的凭证才能过账
        if (voucher.getStatus() != 1) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_AUDITED);
        }

        voucher.setStatus(2);
        voucher.setPostBy(SecurityUtils.getCurrentUserId());
        voucher.setPostTime(LocalDateTime.now());
        this.updateById(voucher);
        
        log.info("过账凭证成功，凭证ID: {}, 凭证号: {}", id, voucher.getVoucherNo());
    }

    /**
     * 检查会计期间是否存在
     */
    private AccountPeriod checkPeriodExists(Long accountSetId, Integer year, Integer month) {
        LambdaQueryWrapper<AccountPeriod> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountPeriod::getAccountSetId, accountSetId)
               .eq(AccountPeriod::getYear, year)
               .eq(AccountPeriod::getMonth, month);
        AccountPeriod period = accountPeriodMapper.selectOne(wrapper);
        if (period == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_SET_PERIOD_NOT_FOUND);
        }
        return period;
    }

    /**
     * 校验凭证日期是否在会计期间范围内
     */
    private void validateVoucherDateInRange(LocalDate voucherDate, LocalDate startDate, LocalDate endDate) {
        if (voucherDate.isBefore(startDate) || voucherDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.VOUCHER_DATE_INVALID);
        }
    }

    /**
     * 检查会计期间是否已结账
     */
    private void checkPeriodNotClosed(AccountPeriod period) {
        if (period.getStatus() != null && period.getStatus() == 1) {
            throw new BusinessException(ErrorCode.PERIOD_CLOSED);
        }
    }
    
    /**
     * 校验凭证明细并计算借贷合计
     */
    private BigDecimal[] validateDetailsAndGetTotals(List<VoucherDetailRequest> details) {
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        
        for (VoucherDetailRequest detail : details) {
            // 业务校验：摘要不能为空
            if (StrUtil.isBlank(detail.getSummary())) {
                throw new BusinessException(ErrorCode.VOUCHER_SUMMARY_BLANK);
            }
            
            // 业务校验：科目ID不能为空
            if (detail.getSubjectId() == null) {
                throw new BusinessException(ErrorCode.VOUCHER_SUBJECT_ID_BLANK);
            }
            
            // 业务校验：借方金额不能为空
            if (detail.getDebit() == null) {
                throw new BusinessException(ErrorCode.VOUCHER_DEBIT_BLANK);
            }
            
            // 业务校验：贷方金额不能为空
            if (detail.getCredit() == null) {
                throw new BusinessException(ErrorCode.VOUCHER_CREDIT_BLANK);
            }
            
            // 业务校验：金额不能为负数
            if (detail.getDebit().compareTo(BigDecimal.ZERO) < 0 || 
                detail.getCredit().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.VOUCHER_AMOUNT_NEGATIVE);
            }
            
            // 业务校验：借贷方金额不能同时为零
            if (detail.getDebit().compareTo(BigDecimal.ZERO) == 0 && 
                detail.getCredit().compareTo(BigDecimal.ZERO) == 0) {
                throw new BusinessException(ErrorCode.VOUCHER_DEBIT_CREDIT_BOTH_ZERO);
            }
            
            // 业务校验：同一行借贷方金额不能同时有值
            if (detail.getDebit().compareTo(BigDecimal.ZERO) > 0 && 
                detail.getCredit().compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessException(ErrorCode.VOUCHER_DEBIT_CREDIT_BOTH_NONZERO);
            }
            
            // 业务校验：科目必须存在且启用
            Subject subject = subjectMapper.selectById(detail.getSubjectId());
            if (subject == null || subject.getStatus() != 1) {
                throw new BusinessException(ErrorCode.VOUCHER_SUBJECT_INVALID);
            }
            
            totalDebit = totalDebit.add(detail.getDebit());
            totalCredit = totalCredit.add(detail.getCredit());
        }
        
        return new BigDecimal[]{totalDebit, totalCredit};
    }

    /**
     * 生成凭证号：格式 year-month-sequence，如 2026-06-001
     */
    private String generateVoucherNo(Long accountSetId, Integer year, Integer month) {
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Voucher::getAccountSetId, accountSetId)
               .eq(Voucher::getYear, year)
               .eq(Voucher::getMonth, month)
               .orderByDesc(Voucher::getVoucherNo);
        List<Voucher> vouchers = this.list(wrapper);

        int sequence = 1;
        if (!vouchers.isEmpty()) {
            String lastVoucherNo = vouchers.get(0).getVoucherNo();
            if (StrUtil.isNotBlank(lastVoucherNo)) {
                String[] parts = lastVoucherNo.split("-");
                if (parts.length == 3) {
                    try {
                        sequence = Integer.parseInt(parts[2]) + 1;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        return String.format("%d-%02d-%03d", year, month, sequence);
    }
    
    /**
     * 重新编号该期间的凭证（保证凭证号连续性）
     */
    private void renumberVouchers(Long accountSetId, Integer year, Integer month) {
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Voucher::getAccountSetId, accountSetId)
               .eq(Voucher::getYear, year)
               .eq(Voucher::getMonth, month)
               .orderByAsc(Voucher::getVoucherDate, Voucher::getVoucherNo);
        List<Voucher> vouchers = this.list(wrapper);
        
        int sequence = 1;
        for (Voucher voucher : vouchers) {
            String newVoucherNo = String.format("%d-%02d-%03d", year, month, sequence);
            if (!newVoucherNo.equals(voucher.getVoucherNo())) {
                voucher.setVoucherNo(newVoucherNo);
                this.updateById(voucher);
            }
            sequence++;
        }
        
        log.info("重新编号凭证成功，账套ID: {}, 年度: {}, 月份: {}, 凭证数量: {}", 
                accountSetId, year, month, vouchers.size());
    }

    /**
     * 保存凭证明细
     */
    private void saveDetails(Long voucherId, List<VoucherDetailRequest> detailRequests) {
        for (int i = 0; i < detailRequests.size(); i++) {
            VoucherDetailRequest request = detailRequests.get(i);
            VoucherDetail detail = new VoucherDetail();
            detail.setVoucherId(voucherId);
            detail.setLineNo(request.getLineNo() != null ? request.getLineNo() : i + 1);
            detail.setSummary(request.getSummary());
            detail.setSubjectId(request.getSubjectId());
            detail.setAuxiliaryId(request.getAuxiliaryId());
            detail.setDebit(request.getDebit() != null ? request.getDebit() : BigDecimal.ZERO);
            detail.setCredit(request.getCredit() != null ? request.getCredit() : BigDecimal.ZERO);
            detail.setQuantity(request.getQuantity());
            detail.setUnitPrice(request.getUnitPrice());
            detail.setSortOrder(i + 1);
            voucherDetailMapper.insert(detail);
        }
    }

    /**
     * 凭证实体转VO
     */
    private VoucherVO convertToVO(Voucher voucher) {
        VoucherVO vo = new VoucherVO();
        BeanUtil.copyProperties(voucher, vo);

        // 查询凭证字名称
        if (voucher.getVoucherWordId() != null) {
            VoucherWord voucherWord = voucherWordMapper.selectById(voucher.getVoucherWordId());
            if (voucherWord != null) {
                vo.setVoucherWordName(voucherWord.getName());
            }
        }

        // 查询创建人名称
        if (voucher.getCreateBy() != null) {
            SysUser createUser = sysUserMapper.selectById(voucher.getCreateBy());
            if (createUser != null) {
                vo.setCreateByName(createUser.getRealName() != null ? createUser.getRealName() : createUser.getUsername());
            }
        }

        // 查询审核人名称
        if (voucher.getAuditBy() != null) {
            SysUser auditUser = sysUserMapper.selectById(voucher.getAuditBy());
            if (auditUser != null) {
                vo.setAuditByName(auditUser.getRealName() != null ? auditUser.getRealName() : auditUser.getUsername());
            }
        }

        // 查询过账人名称
        if (voucher.getPostBy() != null) {
            SysUser postUser = sysUserMapper.selectById(voucher.getPostBy());
            if (postUser != null) {
                vo.setPostByName(postUser.getRealName() != null ? postUser.getRealName() : postUser.getUsername());
            }
        }

        return vo;
    }

    /**
     * 凭证明细实体转VO
     */
    private VoucherDetailVO convertDetailToVO(VoucherDetail detail) {
        VoucherDetailVO vo = new VoucherDetailVO();
        BeanUtil.copyProperties(detail, vo);
        return vo;
    }
}
