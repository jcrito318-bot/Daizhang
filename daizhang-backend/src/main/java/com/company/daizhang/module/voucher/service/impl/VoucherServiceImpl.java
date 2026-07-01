package com.company.daizhang.module.voucher.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.entity.SubjectBalance;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
import com.company.daizhang.module.accountset.mapper.SubjectBalanceMapper;
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
    private final AccountBalanceMapper accountBalanceMapper;
    private final SysUserMapper sysUserMapper;
    private final SubjectMapper subjectMapper;
    private final SubjectBalanceMapper subjectBalanceMapper;

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

        // 业务校验：制单人不能与审核人为同一人
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (voucher.getCreateBy() != null && voucher.getCreateBy().equals(currentUserId)) {
            throw new BusinessException("制单人与审核人不能为同一人");
        }
        
        // 业务校验：审核前再次验证借贷平衡
        if (voucher.getTotalDebit().compareTo(voucher.getTotalCredit()) != 0) {
            throw new BusinessException(ErrorCode.VOUCHER_BALANCE_ERROR);
        }

        voucher.setStatus(1);
        voucher.setAuditBy(currentUserId);
        voucher.setAuditTime(LocalDateTime.now());
        this.updateById(voucher);
        
        log.info("审核凭证成功，凭证ID: {}, 凭证号: {}, 审核人: {}", id, voucher.getVoucherNo(), currentUserId);
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
        // 使用LambdaUpdateWrapper显式set null，避免MyBatis-Plus默认NOT_NULL策略不更新null字段
        LambdaUpdateWrapper<Voucher> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Voucher::getId, id)
                     .set(Voucher::getStatus, 0)
                     .set(Voucher::getAuditBy, null)
                     .set(Voucher::getAuditTime, null);
        this.update(updateWrapper);

        log.info("反审核凭证成功，凭证ID: {}, 凭证号: {}", id, voucher.getVoucherNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAuditVoucher(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "凭证ID列表不能为空");
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        int success = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (Long id : ids) {
            Voucher voucher = this.getById(id);
            if (voucher == null) {
                errors.add("凭证ID=" + id + " 不存在");
                continue;
            }
            if (voucher.getStatus() != null && voucher.getStatus() != 0) {
                errors.add("凭证" + voucher.getVoucherNo() + " 非未审核状态，不能审核");
                continue;
            }
            // 制单人与审核人不能为同一人
            if (voucher.getCreateBy() != null && voucher.getCreateBy().equals(currentUserId)) {
                errors.add("凭证" + voucher.getVoucherNo() + " 制单人与审核人不能为同一人");
                continue;
            }
            // 借贷平衡校验
            if (voucher.getTotalDebit() != null && voucher.getTotalCredit() != null
                    && voucher.getTotalDebit().compareTo(voucher.getTotalCredit()) != 0) {
                errors.add("凭证" + voucher.getVoucherNo() + " 借贷不平衡");
                continue;
            }
            voucher.setStatus(1);
            voucher.setAuditBy(currentUserId);
            voucher.setAuditTime(now);
            this.updateById(voucher);
            success++;
        }

        if (!errors.isEmpty()) {
            log.warn("批量审核部分失败，成功{}张，失败{}张：{}", success, ids.size() - success, errors);
        } else {
            log.info("批量审核完成，成功{}张", success);
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUnauditVoucher(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "凭证ID列表不能为空");
        }
        int success = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (Long id : ids) {
            Voucher voucher = this.getById(id);
            if (voucher == null) {
                errors.add("凭证ID=" + id + " 不存在");
                continue;
            }
            // 只有已审核且未过账的凭证才能反审核
            if (voucher.getStatus() == null || voucher.getStatus() != 1) {
                errors.add("凭证" + voucher.getVoucherNo() + " 非已审核状态，不能反审核");
                continue;
            }
            voucher.setStatus(0);
            voucher.setAuditBy(null);
            voucher.setAuditTime(null);
            // 使用LambdaUpdateWrapper显式set null
            LambdaUpdateWrapper<Voucher> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Voucher::getId, voucher.getId())
                         .set(Voucher::getStatus, 0)
                         .set(Voucher::getAuditBy, null)
                         .set(Voucher::getAuditTime, null);
            this.update(updateWrapper);
            success++;
        }

        if (!errors.isEmpty()) {
            log.warn("批量反审核部分失败，成功{}张，失败{}张：{}", success, ids.size() - success, errors);
        } else {
            log.info("批量反审核完成，成功{}张", success);
        }
        return success;
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

        // 校验凭证所属期间未结账,防止向已结账期间写入余额破坏结账快照
        // (create/update/saveDraft/copy/reverse均做此校验,过账遗漏会导致已结账期间余额被改写)
        AccountPeriod period = checkPeriodExists(voucher.getAccountSetId(), voucher.getYear(), voucher.getMonth());
        checkPeriodNotClosed(period);

        // 查询凭证明细
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(VoucherDetail::getVoucherId, id)
                     .orderByAsc(VoucherDetail::getSortOrder);
        List<VoucherDetail> details = voucherDetailMapper.selectList(detailWrapper);

        // 更新各科目余额
        for (VoucherDetail detail : details) {
            updateAccountBalance(voucher, detail);
        }

        voucher.setStatus(2);
        voucher.setPostBy(SecurityUtils.getCurrentUserId());
        voucher.setPostTime(LocalDateTime.now());
        this.updateById(voucher);

        log.info("过账凭证成功，凭证ID: {}, 凭证号: {}", id, voucher.getVoucherNo());
    }

    /**
     * 过账时更新科目余额（本期发生额、期末余额、本年累计）
     */
    private void updateAccountBalance(Voucher voucher, VoucherDetail detail) {
        Long accountSetId = voucher.getAccountSetId();
        Long subjectId = detail.getSubjectId();
        Integer year = voucher.getYear();
        Integer month = voucher.getMonth();

        BigDecimal debit = detail.getDebit() != null ? detail.getDebit() : BigDecimal.ZERO;
        BigDecimal credit = detail.getCredit() != null ? detail.getCredit() : BigDecimal.ZERO;
        if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        // 查询科目以获取余额方向
        Subject subject = subjectMapper.selectById(subjectId);
        Integer balanceDirection = subject != null && subject.getBalanceDirection() != null
                ? subject.getBalanceDirection() : 1;

        // 查询或创建该期间该科目的余额记录
        LambdaQueryWrapper<AccountBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountBalance::getAccountSetId, accountSetId)
               .eq(AccountBalance::getSubjectId, subjectId)
               .eq(AccountBalance::getYear, year)
               .eq(AccountBalance::getMonth, month);
        AccountBalance balance = accountBalanceMapper.selectOne(wrapper);

        if (balance == null) {
            balance = new AccountBalance();
            balance.setAccountSetId(accountSetId);
            balance.setSubjectId(subjectId);
            balance.setYear(year);
            balance.setMonth(month);
            // 期初余额从上一期间期末结转：跨年则取上年12月，否则取本年上月。
            // 否则第2个月起期初恒为0，导致期末余额漏掉上月余额、试算不平衡、结账失败。
            int lastYear = (month == 1) ? year - 1 : year;
            int lastMonth = (month == 1) ? 12 : month - 1;
            LambdaQueryWrapper<AccountBalance> lastWrapper = new LambdaQueryWrapper<>();
            lastWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                       .eq(AccountBalance::getSubjectId, subjectId)
                       .eq(AccountBalance::getYear, lastYear)
                       .eq(AccountBalance::getMonth, lastMonth);
            AccountBalance lastBalance = accountBalanceMapper.selectOne(lastWrapper);
            BigDecimal carriedBeginDebit = BigDecimal.ZERO;
            BigDecimal carriedBeginCredit = BigDecimal.ZERO;
            BigDecimal carriedYearDebit = BigDecimal.ZERO;
            BigDecimal carriedYearCredit = BigDecimal.ZERO;
            if (lastBalance != null) {
                carriedBeginDebit = lastBalance.getEndDebit() != null ? lastBalance.getEndDebit() : BigDecimal.ZERO;
                carriedBeginCredit = lastBalance.getEndCredit() != null ? lastBalance.getEndCredit() : BigDecimal.ZERO;
                // 本年累计只在同一年内结转；1月（即上年12月结转）从0开始新一年累计
                if (lastYear == year) {
                    carriedYearDebit = lastBalance.getYearDebit() != null ? lastBalance.getYearDebit() : BigDecimal.ZERO;
                    carriedYearCredit = lastBalance.getYearCredit() != null ? lastBalance.getYearCredit() : BigDecimal.ZERO;
                }
            } else if (month == 1) {
                // 1月且无上年12月期末余额:从用户录入的期初余额(SubjectBalance)取值。
                // 否则用户在期初余额界面录入的数据不会进入 AccountBalance,
                // 导致月度余额表/资产负债表/试算平衡(走 AccountBalance 的路径)期初恒为0,
                // 首次过账即试算不平衡。
                SubjectBalance subjectBegin = subjectBalanceMapper.selectOne(new LambdaQueryWrapper<SubjectBalance>()
                        .eq(SubjectBalance::getAccountSetId, accountSetId)
                        .eq(SubjectBalance::getSubjectId, subjectId)
                        .eq(SubjectBalance::getYear, year)
                        .eq(SubjectBalance::getPeriod, 1));
                if (subjectBegin != null) {
                    carriedBeginDebit = subjectBegin.getBeginDebit() != null ? subjectBegin.getBeginDebit() : BigDecimal.ZERO;
                    carriedBeginCredit = subjectBegin.getBeginCredit() != null ? subjectBegin.getBeginCredit() : BigDecimal.ZERO;
                }
            }
            balance.setBeginDebit(carriedBeginDebit);
            balance.setBeginCredit(carriedBeginCredit);
            balance.setPeriodDebit(BigDecimal.ZERO);
            balance.setPeriodCredit(BigDecimal.ZERO);
            balance.setYearDebit(carriedYearDebit);
            balance.setYearCredit(carriedYearCredit);
            accountBalanceMapper.insert(balance);
        }

        // 累加本期发生额
        BigDecimal newPeriodDebit = (balance.getPeriodDebit() != null ? balance.getPeriodDebit() : BigDecimal.ZERO).add(debit);
        BigDecimal newPeriodCredit = (balance.getPeriodCredit() != null ? balance.getPeriodCredit() : BigDecimal.ZERO).add(credit);
        balance.setPeriodDebit(newPeriodDebit);
        balance.setPeriodCredit(newPeriodCredit);

        // 计算期末余额：按余额方向计算净额
        // balanceDirection=1（借方余额）：endDebit = beginDebit + periodDebit - periodCredit（若为正）
        // balanceDirection=2（贷方余额）：endCredit = beginCredit + periodCredit - periodDebit（若为正）
        BigDecimal beginDebit = balance.getBeginDebit() != null ? balance.getBeginDebit() : BigDecimal.ZERO;
        BigDecimal beginCredit = balance.getBeginCredit() != null ? balance.getBeginCredit() : BigDecimal.ZERO;
        if (balanceDirection == 2) {
            BigDecimal netCredit = beginCredit.add(newPeriodCredit).subtract(newPeriodDebit);
            if (netCredit.compareTo(BigDecimal.ZERO) >= 0) {
                balance.setEndCredit(netCredit);
                balance.setEndDebit(BigDecimal.ZERO);
            } else {
                balance.setEndCredit(BigDecimal.ZERO);
                balance.setEndDebit(netCredit.abs());
            }
        } else {
            BigDecimal netDebit = beginDebit.add(newPeriodDebit).subtract(newPeriodCredit);
            if (netDebit.compareTo(BigDecimal.ZERO) >= 0) {
                balance.setEndDebit(netDebit);
                balance.setEndCredit(BigDecimal.ZERO);
            } else {
                balance.setEndDebit(BigDecimal.ZERO);
                balance.setEndCredit(netDebit.abs());
            }
        }

        // 累加本年发生额
        BigDecimal newYearDebit = (balance.getYearDebit() != null ? balance.getYearDebit() : BigDecimal.ZERO).add(debit);
        BigDecimal newYearCredit = (balance.getYearCredit() != null ? balance.getYearCredit() : BigDecimal.ZERO).add(credit);
        balance.setYearDebit(newYearDebit);
        balance.setYearCredit(newYearCredit);

        // 检查乐观锁更新结果: BaseEntity带@Version,OptimisticLockerInnerInterceptor已启用。
        // 并发过账同一科目同一期间凭证时,version已被其他事务修改,updateById返回0。
        // 若不检查返回值,凭证状态变为已过账但余额未累加,造成静默数据损坏且试算不平衡。
        int updated = accountBalanceMapper.updateById(balance);
        if (updated == 0) {
            throw new BusinessException("科目余额更新失败(并发冲突)，请重试。科目ID：" + subjectId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rearrangeVoucherNo(Long accountSetId, Integer year, Integer month) {
        // 业务校验：年度必须合理
        if (year == null || year < 1900 || year > 2099) {
            throw new BusinessException(ErrorCode.VOUCHER_YEAR_INVALID);
        }
        // 业务校验：月份必须在1-12之间
        if (month == null || month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.VOUCHER_MONTH_INVALID);
        }

        // 仅重排未审核(status=0)凭证,已审核(1)/已过账(2)凭证号已固化,不可篡改,
        // 否则会破坏已打印/归档/对外引用的凭证号稳定性与审计轨迹
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Voucher::getAccountSetId, accountSetId)
               .eq(Voucher::getYear, year)
               .eq(Voucher::getMonth, month)
               .eq(Voucher::getStatus, 0)
               .orderByAsc(Voucher::getVoucherDate)
               .orderByAsc(Voucher::getCreateTime);
        List<Voucher> vouchers = this.list(wrapper);

        if (vouchers.isEmpty()) {
            log.info("凭证整理（断号重编）无未审核凭证可整理，账套ID: {}, 年度: {}, 月份: {}", accountSetId, year, month);
            return;
        }

        // 查询已审核/已过账凭证的最大序号,未审核凭证从maxSeq+1开始编号,避免与已固化号冲突
        LambdaQueryWrapper<Voucher> fixedWrapper = new LambdaQueryWrapper<>();
        fixedWrapper.eq(Voucher::getAccountSetId, accountSetId)
                    .eq(Voucher::getYear, year)
                    .eq(Voucher::getMonth, month)
                    .ne(Voucher::getStatus, 0)
                    .notLike(Voucher::getVoucherNo, "TMP-%")
                    // 按序号数值排序:序号超999时字符串排序会取错最大号,导致未审核凭证从错误序号开始编号
                    .last("ORDER BY CAST(SUBSTRING_INDEX(voucher_no, '-', -1) AS UNSIGNED) DESC LIMIT 1");
        List<Voucher> fixedVouchers = this.list(fixedWrapper);
        int sequence = 1;
        if (!fixedVouchers.isEmpty()) {
            String maxFixedNo = fixedVouchers.get(0).getVoucherNo();
            if (StrUtil.isNotBlank(maxFixedNo)) {
                String[] parts = maxFixedNo.split("-");
                if (parts.length == 3) {
                    try {
                        sequence = Integer.parseInt(parts[2]) + 1;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        // 第一步：把未审核凭证号改成临时号（避免唯一索引冲突）
        // 格式: TMP-{原ID}，保证全局唯一不会冲突
        for (Voucher voucher : vouchers) {
            Voucher update = new Voucher();
            update.setId(voucher.getId());
            update.setVoucherNo("TMP-" + voucher.getId());
            this.updateById(update);
        }

        // 第二步：重新生成连续的凭证号（格式: year-month-seq，如 2026-01-001）
        for (Voucher voucher : vouchers) {
            String newVoucherNo = String.format("%d-%02d-%03d", year, month, sequence);
            Voucher update = new Voucher();
            update.setId(voucher.getId());
            update.setVoucherNo(newVoucherNo);
            this.updateById(update);
            sequence++;
        }

        log.info("凭证整理（断号重编）成功，账套ID: {}, 年度: {}, 月份: {}, 未审核凭证数量: {}",
                accountSetId, year, month, vouchers.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyVoucher(Long id) {
        // 查询原凭证
        Voucher original = this.getById(id);
        if (original == null) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        // 校验目标期间存在且未结账（复制等同于在原期间新建凭证，须遵守期间约束）
        AccountPeriod period = checkPeriodExists(original.getAccountSetId(), original.getYear(), original.getMonth());
        checkPeriodNotClosed(period);

        // 查询原凭证明细
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(VoucherDetail::getVoucherId, id)
                     .orderByAsc(VoucherDetail::getSortOrder);
        List<VoucherDetail> originalDetails = voucherDetailMapper.selectList(detailWrapper);

        // 生成新凭证号
        String newVoucherNo = generateVoucherNo(original.getAccountSetId(), original.getYear(), original.getMonth());

        // 创建新凭证（status=0未审核，source=0手工录入）
        Voucher newVoucher = new Voucher();
        newVoucher.setAccountSetId(original.getAccountSetId());
        newVoucher.setVoucherWordId(original.getVoucherWordId());
        newVoucher.setVoucherNo(newVoucherNo);
        newVoucher.setVoucherDate(original.getVoucherDate());
        newVoucher.setYear(original.getYear());
        newVoucher.setMonth(original.getMonth());
        newVoucher.setTotalDebit(original.getTotalDebit());
        newVoucher.setTotalCredit(original.getTotalCredit());
        newVoucher.setAttachmentCount(original.getAttachmentCount());
        newVoucher.setStatus(0);
        newVoucher.setSource(0);
        this.save(newVoucher);

        // 复制所有明细行，金额相同
        for (int i = 0; i < originalDetails.size(); i++) {
            VoucherDetail originalDetail = originalDetails.get(i);
            VoucherDetail newDetail = new VoucherDetail();
            newDetail.setVoucherId(newVoucher.getId());
            newDetail.setLineNo(originalDetail.getLineNo() != null ? originalDetail.getLineNo() : i + 1);
            newDetail.setSummary(originalDetail.getSummary());
            newDetail.setSubjectId(originalDetail.getSubjectId());
            newDetail.setSubjectCode(originalDetail.getSubjectCode());
            newDetail.setSubjectName(originalDetail.getSubjectName());
            newDetail.setAuxiliaryId(originalDetail.getAuxiliaryId());
            newDetail.setDebit(originalDetail.getDebit() != null ? originalDetail.getDebit() : BigDecimal.ZERO);
            newDetail.setCredit(originalDetail.getCredit() != null ? originalDetail.getCredit() : BigDecimal.ZERO);
            newDetail.setQuantity(originalDetail.getQuantity());
            newDetail.setUnitPrice(originalDetail.getUnitPrice());
            newDetail.setSortOrder(i + 1);
            voucherDetailMapper.insert(newDetail);
        }

        log.info("复制凭证成功，原凭证ID: {}, 原凭证号: {}, 新凭证ID: {}, 新凭证号: {}",
                id, original.getVoucherNo(), newVoucher.getId(), newVoucherNo);

        return newVoucher.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reverseVoucher(Long id) {
        return reverseVoucher(id, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reverseVoucher(Long id, Integer targetYear, Integer targetMonth) {
        // 查询原凭证
        Voucher original = this.getById(id);
        if (original == null) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }
        // 红冲仅对已过账凭证有意义（status=2），未审核/未过账凭证不可红冲
        if (original.getStatus() == null || original.getStatus() != 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "只有已过账的凭证才能红冲");
        }

        // 确定红冲凭证的目标期间：未指定时默认取原凭证期间
        int revYear = targetYear != null ? targetYear : original.getYear();
        int revMonth = targetMonth != null ? targetMonth : original.getMonth();
        if (revMonth < 1 || revMonth > 12) {
            throw new BusinessException(ErrorCode.VOUCHER_MONTH_INVALID);
        }

        // 校验目标期间存在且未结账
        // (原实现固定用原期间,原期间结账后红冲彻底无法执行,缺少"跨期红冲"退路)
        AccountPeriod period = checkPeriodExists(original.getAccountSetId(), revYear, revMonth);
        checkPeriodNotClosed(period);

        // 查询原凭证明细
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(VoucherDetail::getVoucherId, id)
                     .orderByAsc(VoucherDetail::getSortOrder);
        List<VoucherDetail> originalDetails = voucherDetailMapper.selectList(detailWrapper);

        // 生成新凭证号（基于目标期间）
        String newVoucherNo = generateVoucherNo(original.getAccountSetId(), revYear, revMonth);

        // 创建红冲凭证：借贷方金额互换，相当于取负
        // 互换后：新借方合计 = 原贷方合计，新贷方合计 = 原借方合计
        Voucher newVoucher = new Voucher();
        newVoucher.setAccountSetId(original.getAccountSetId());
        newVoucher.setVoucherWordId(original.getVoucherWordId());
        newVoucher.setVoucherNo(newVoucherNo);
        // 跨期红冲时凭证日期调整为目标期间首日，避免凭证日期与期间不一致
        newVoucher.setVoucherDate(LocalDate.of(revYear, revMonth, 1));
        newVoucher.setYear(revYear);
        newVoucher.setMonth(revMonth);
        newVoucher.setTotalDebit(original.getTotalCredit() != null ? original.getTotalCredit() : BigDecimal.ZERO);
        newVoucher.setTotalCredit(original.getTotalDebit() != null ? original.getTotalDebit() : BigDecimal.ZERO);
        newVoucher.setAttachmentCount(original.getAttachmentCount());
        // 红冲凭证直接设为已过账(status=2)，使余额立即抵消。
        // 原实现只生成status=0凭证，不调用updateAccountBalance，原凭证余额原封不动，
        // 抵消依赖用户事后手动审核+过账，极易被遗漏导致余额持续错误且无告警。
        Long currentUserId = SecurityUtils.getCurrentUserId();
        newVoucher.setStatus(2);
        newVoucher.setAuditBy(currentUserId);
        newVoucher.setAuditTime(LocalDateTime.now());
        newVoucher.setPostBy(currentUserId);
        newVoucher.setPostTime(LocalDateTime.now());
        newVoucher.setSource(0);
        this.save(newVoucher);

        // 复制明细行，借方和贷方金额互换，摘要加"[红冲]原凭证号"
        String reversePrefix = "[红冲]" + original.getVoucherNo() + " ";
        for (int i = 0; i < originalDetails.size(); i++) {
            VoucherDetail originalDetail = originalDetails.get(i);
            VoucherDetail newDetail = new VoucherDetail();
            newDetail.setVoucherId(newVoucher.getId());
            newDetail.setLineNo(originalDetail.getLineNo() != null ? originalDetail.getLineNo() : i + 1);
            // 摘要加"[红冲]原凭证号"
            String originalSummary = originalDetail.getSummary() != null ? originalDetail.getSummary() : "";
            newDetail.setSummary(reversePrefix + originalSummary);
            newDetail.setSubjectId(originalDetail.getSubjectId());
            newDetail.setSubjectCode(originalDetail.getSubjectCode());
            newDetail.setSubjectName(originalDetail.getSubjectName());
            newDetail.setAuxiliaryId(originalDetail.getAuxiliaryId());
            // 借贷方金额互换
            BigDecimal originalDebit = originalDetail.getDebit() != null ? originalDetail.getDebit() : BigDecimal.ZERO;
            BigDecimal originalCredit = originalDetail.getCredit() != null ? originalDetail.getCredit() : BigDecimal.ZERO;
            newDetail.setDebit(originalCredit);
            newDetail.setCredit(originalDebit);
            newDetail.setQuantity(originalDetail.getQuantity());
            newDetail.setUnitPrice(originalDetail.getUnitPrice());
            newDetail.setSortOrder(i + 1);
            voucherDetailMapper.insert(newDetail);
        }

        // 立即更新各科目余额，使红冲立即抵消原凭证的发生额
        for (VoucherDetail detail : originalDetails) {
            VoucherDetail reversedDetail = new VoucherDetail();
            reversedDetail.setSubjectId(detail.getSubjectId());
            BigDecimal origDebit = detail.getDebit() != null ? detail.getDebit() : BigDecimal.ZERO;
            BigDecimal origCredit = detail.getCredit() != null ? detail.getCredit() : BigDecimal.ZERO;
            reversedDetail.setDebit(origCredit);   // 借贷互换
            reversedDetail.setCredit(origDebit);
            updateAccountBalance(newVoucher, reversedDetail);
        }

        log.info("红冲凭证成功（自动过账），原凭证ID: {}, 原凭证号: {}, 新凭证ID: {}, 新凭证号: {}, 目标期间: {}-{}",
                id, original.getVoucherNo(), newVoucher.getId(), newVoucherNo, revYear, revMonth);

        return newVoucher.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDraft(VoucherCreateRequest request) {
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

        // 草稿不校验借贷平衡，仅计算借贷合计
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (VoucherDetailRequest detail : request.getDetails()) {
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
            if (detail.getDebit().compareTo(BigDecimal.ZERO) < 0
                    || detail.getCredit().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.VOUCHER_AMOUNT_NEGATIVE);
            }
            // 业务校验：科目必须存在且启用
            Subject subject = subjectMapper.selectById(detail.getSubjectId());
            if (subject == null || subject.getStatus() != 1) {
                throw new BusinessException(ErrorCode.VOUCHER_SUBJECT_INVALID);
            }
            totalDebit = totalDebit.add(detail.getDebit());
            totalCredit = totalCredit.add(detail.getCredit());
        }

        // 生成凭证号
        String voucherNo = generateVoucherNo(request.getAccountSetId(), request.getYear(), request.getMonth());

        // 保存草稿凭证（status=0未审核，draftStatus=1草稿，source=0手工录入）
        Voucher voucher = new Voucher();
        BeanUtil.copyProperties(request, voucher);
        voucher.setVoucherNo(voucherNo);
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucher.setStatus(0);
        voucher.setSource(0);
        voucher.setDraftStatus(1);
        this.save(voucher);

        // 保存凭证明细
        saveDetails(voucher.getId(), request.getDetails());

        log.info("保存凭证草稿成功，凭证号: {}, 借方合计: {}, 贷方合计: {}", voucherNo, totalDebit, totalCredit);

        return voucher.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitDraft(Long id) {
        Voucher voucher = this.getById(id);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        // 只有草稿凭证才能提交
        if (voucher.getDraftStatus() == null || voucher.getDraftStatus() != 1) {
            throw new BusinessException("该凭证不是草稿，无法提交");
        }

        // 业务校验：提交前验证借贷平衡
        if (voucher.getTotalDebit() == null || voucher.getTotalCredit() == null
                || voucher.getTotalDebit().compareTo(voucher.getTotalCredit()) != 0) {
            throw new BusinessException(ErrorCode.VOUCHER_BALANCE_ERROR);
        }

        // 业务校验：借贷合计不能为零
        if (voucher.getTotalDebit().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(ErrorCode.VOUCHER_DEBIT_CREDIT_BOTH_ZERO);
        }

        // 转为正常凭证：draftStatus=0
        voucher.setDraftStatus(0);
        this.updateById(voucher);

        log.info("提交凭证草稿成功，凭证ID: {}, 凭证号: {}", id, voucher.getVoucherNo());
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
               .notLike(Voucher::getVoucherNo, "TMP-%")
               // 按序号数值排序,而非字符串排序:序号超999时"999"按字符串大于"1000",会取错最大号导致重号
               .last("ORDER BY CAST(SUBSTRING_INDEX(voucher_no, '-', -1) AS UNSIGNED) DESC LIMIT 1");
        Voucher lastVoucher = this.getOne(wrapper);

        int sequence = 1;
        if (lastVoucher != null && StrUtil.isNotBlank(lastVoucher.getVoucherNo())) {
            String lastVoucherNo = lastVoucher.getVoucherNo();
            String[] parts = lastVoucherNo.split("-");
            if (parts.length == 3) {
                try {
                    sequence = Integer.parseInt(parts[2]) + 1;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return String.format("%d-%02d-%03d", year, month, sequence);
    }
    
    /**
     * 重新编号该期间的凭证（保证凭证号连续性）
     * 使用两步更新避免唯一索引冲突：先将所有凭证号改为临时号，再分配连续正式号
     */
    private void renumberVouchers(Long accountSetId, Integer year, Integer month) {
        // 仅重编号未审核(status=0)的凭证,已审核(1)/已过账(2)凭证号已固化,不可篡改,
        // 否则会破坏已打印/归档/对外引用的凭证号稳定性与审计轨迹
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Voucher::getAccountSetId, accountSetId)
               .eq(Voucher::getYear, year)
               .eq(Voucher::getMonth, month)
               .eq(Voucher::getStatus, 0)
               // 按凭证日期、序号数值排序:序号超999时字符串排序会乱序,导致重新编号后凭证号顺序与日期错乱
               .last("ORDER BY voucher_date ASC, CAST(SUBSTRING_INDEX(voucher_no, '-', -1) AS UNSIGNED) ASC");
        List<Voucher> vouchers = this.list(wrapper);

        if (vouchers.isEmpty()) {
            return;
        }

        // 第一步：将未审核凭证号改为临时号 TMP-{id}，避免唯一索引冲突
        for (Voucher voucher : vouchers) {
            LambdaUpdateWrapper<Voucher> tempWrapper = new LambdaUpdateWrapper<>();
            tempWrapper.eq(Voucher::getId, voucher.getId())
                       .set(Voucher::getVoucherNo, "TMP-" + voucher.getId());
            this.update(tempWrapper);
        }

        // 第二步：按顺序分配连续正式号
        // 必须先查询已审核/已过账凭证的最大数值序号,从maxSeq+1开始编号,
        // 否则从1开始会与已固化的凭证号冲突,触发唯一索引违例致删除操作失败
        // (与 rearrangeVoucherNo 保持一致)
        LambdaQueryWrapper<Voucher> fixedWrapper = new LambdaQueryWrapper<>();
        fixedWrapper.eq(Voucher::getAccountSetId, accountSetId)
                    .eq(Voucher::getYear, year)
                    .eq(Voucher::getMonth, month)
                    .ne(Voucher::getStatus, 0)
                    .notLike(Voucher::getVoucherNo, "TMP-%")
                    .last("ORDER BY CAST(SUBSTRING_INDEX(voucher_no, '-', -1) AS UNSIGNED) DESC LIMIT 1");
        Voucher maxFixed = this.getOne(fixedWrapper);
        int sequence = 1;
        if (maxFixed != null && StrUtil.isNotBlank(maxFixed.getVoucherNo())) {
            String[] parts = maxFixed.getVoucherNo().split("-");
            if (parts.length == 3) {
                try {
                    sequence = Integer.parseInt(parts[2]) + 1;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        for (Voucher voucher : vouchers) {
            String newVoucherNo = String.format("%d-%02d-%03d", year, month, sequence);
            LambdaUpdateWrapper<Voucher> formalWrapper = new LambdaUpdateWrapper<>();
            formalWrapper.eq(Voucher::getId, voucher.getId())
                         .set(Voucher::getVoucherNo, newVoucherNo);
            this.update(formalWrapper);
            sequence++;
        }

        log.info("重新编号未审核凭证成功，账套ID: {}, 年度: {}, 月份: {}, 凭证数量: {}",
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

            // 根据科目ID查询科目编码和名称
            Subject subject = subjectMapper.selectById(request.getSubjectId());
            if (subject != null) {
                detail.setSubjectCode(subject.getCode());
                detail.setSubjectName(subject.getName());
            }

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
