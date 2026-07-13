package com.company.daizhang.module.bank.service.impl;

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
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.bank.dto.*;
import com.company.daizhang.module.bank.entity.BankAccount;
import com.company.daizhang.module.bank.entity.BankReconciliation;
import com.company.daizhang.module.bank.entity.BankTransaction;
import com.company.daizhang.module.bank.mapper.BankAccountMapper;
import com.company.daizhang.module.bank.mapper.BankReconciliationMapper;
import com.company.daizhang.module.bank.mapper.BankTransactionMapper;
import com.company.daizhang.module.bank.service.BankService;
import com.company.daizhang.module.bank.service.BankVoucherService;
import com.company.daizhang.module.bank.vo.BankReconciliationVO;
import com.company.daizhang.module.bank.vo.BankTransactionVO;
import com.company.daizhang.module.bank.vo.UnmatchedItemVO;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.entity.VoucherDetail;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 银行对账服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankServiceImpl extends ServiceImpl<BankTransactionMapper, BankTransaction> implements BankService {

    private final BankReconciliationMapper bankReconciliationMapper;
    private final VoucherMapper voucherMapper;
    private final VoucherDetailMapper voucherDetailMapper;
    private final SysUserMapper sysUserMapper;
    private final BankVoucherService bankVoucherService;
    private final AccountSetAccessService accountSetAccessService;
    private final AccountPeriodMapper accountPeriodMapper;
    private final BankAccountMapper bankAccountMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer importBankTransactions(BankTransactionImportRequest request) {
        // IDOR治理:校验当前用户对该账套的访问权(Controller层@RequireAccountSetAccess已做一次,此处兜底)
        accountSetAccessService.checkAccess(request.getAccountSetId());
        // 校验银行账号归属:bankAccount 必须属于该账套,否则可向他账套的银行账号导入流水
        Long bankAccountCount = bankAccountMapper.selectCount(new LambdaQueryWrapper<BankAccount>()
                .eq(BankAccount::getAccountSetId, request.getAccountSetId())
                .eq(BankAccount::getAccountNumber, request.getBankAccount()));
        if (bankAccountCount == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "银行账号不属于该账套: " + request.getBankAccount());
        }

        List<BankTransactionImportRequest.BankTransactionItem> items = request.getTransactions();
        int count = 0;

        for (int index = 0; index < items.size(); index++) {
            BankTransactionImportRequest.BankTransactionItem item = items.get(index);

            // 金额非负校验，避免负数金额进入对账扭曲银行余额
            if (item.getAmount() != null && item.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                        "第" + (index + 1) + "行金额不能为负: " + item.getAmount());
            }

            // 校验交易日期对应的会计期间是否存在、是否已结账
            LocalDate txDate = item.getTransactionDate();
            if (txDate != null) {
                AccountPeriod period = accountPeriodMapper.selectOne(new LambdaQueryWrapper<AccountPeriod>()
                        .eq(AccountPeriod::getAccountSetId, request.getAccountSetId())
                        .eq(AccountPeriod::getYear, txDate.getYear())
                        .eq(AccountPeriod::getMonth, txDate.getMonthValue()));
                if (period == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                            "第" + (index + 1) + "行日期对应的会计期间不存在: " + txDate);
                }
                if (period.getStatus() != null && period.getStatus() == 1) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                            "第" + (index + 1) + "行日期对应的会计期间已结账: " + txDate.getYear() + "-" + txDate.getMonthValue());
                }
            }

            // 去重：有流水号按流水号去重，无流水号按 accountSetId+bankAccount+date+amount+counterparty 组合去重
            if (StrUtil.isNotBlank(item.getTransactionNo())) {
                LambdaQueryWrapper<BankTransaction> existWrapper = new LambdaQueryWrapper<>();
                existWrapper.eq(BankTransaction::getAccountSetId, request.getAccountSetId())
                            .eq(BankTransaction::getBankAccount, request.getBankAccount())
                            .eq(BankTransaction::getTransactionNo, item.getTransactionNo());
                if (this.count(existWrapper) > 0) {
                    continue;
                }
            } else {
                LambdaQueryWrapper<BankTransaction> existWrapper = new LambdaQueryWrapper<>();
                existWrapper.eq(BankTransaction::getAccountSetId, request.getAccountSetId())
                            .eq(BankTransaction::getBankAccount, request.getBankAccount())
                            .eq(BankTransaction::getTransactionDate, item.getTransactionDate())
                            .eq(BankTransaction::getAmount, item.getAmount())
                            .eq(item.getCounterparty() != null, BankTransaction::getCounterparty, item.getCounterparty());
                if (this.count(existWrapper) > 0) {
                    continue;
                }
            }

            BankTransaction transaction = new BankTransaction();
            BeanUtil.copyProperties(item, transaction);
            transaction.setAccountSetId(request.getAccountSetId());
            transaction.setBankAccount(request.getBankAccount());
            transaction.setMatchedStatus(0);
            this.save(transaction);
            count++;
        }

        return count;
    }

    @Override
    public PageResult<BankTransactionVO> pageBankTransactions(BankTransactionQueryRequest request) {
        Page<BankTransaction> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<BankTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankTransaction::getAccountSetId, request.getAccountSetId())
               .eq(StrUtil.isNotBlank(request.getBankAccount()), BankTransaction::getBankAccount, request.getBankAccount())
               .eq(request.getTransactionType() != null, BankTransaction::getTransactionType, request.getTransactionType())
               .eq(request.getMatchedStatus() != null, BankTransaction::getMatchedStatus, request.getMatchedStatus())
               .ge(request.getStartDate() != null, BankTransaction::getTransactionDate, request.getStartDate())
               .le(request.getEndDate() != null, BankTransaction::getTransactionDate, request.getEndDate())
               .like(StrUtil.isNotBlank(request.getCounterparty()), BankTransaction::getCounterparty, request.getCounterparty())
               .like(StrUtil.isNotBlank(request.getSummary()), BankTransaction::getSummary, request.getSummary())
               .like(StrUtil.isNotBlank(request.getTransactionNo()), BankTransaction::getTransactionNo, request.getTransactionNo())
               .orderByDesc(BankTransaction::getTransactionDate)
               .orderByDesc(BankTransaction::getCreateTime);

        Page<BankTransaction> result = this.page(page, wrapper);

        List<BankTransactionVO> voList = result.getRecords().stream()
                .map(this::convertTransactionToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public BankTransactionVO getTransactionById(Long id) {
        BankTransaction transaction = this.getById(id);
        if (transaction == null) {
            throw new BusinessException("银行流水不存在");
        }
        // IDOR治理:校验当前用户对该流水所属账套的访问权
        accountSetAccessService.checkAccess(transaction.getAccountSetId());
        return convertTransactionToVO(transaction);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer autoMatch(AutoMatchRequest request) {
        // 查询该月份未匹配的银行流水
        LambdaQueryWrapper<BankTransaction> txWrapper = new LambdaQueryWrapper<>();
        txWrapper.eq(BankTransaction::getAccountSetId, request.getAccountSetId())
                 .eq(BankTransaction::getBankAccount, request.getBankAccount())
                 .eq(BankTransaction::getMatchedStatus, 0)
                 .ge(BankTransaction::getTransactionDate, LocalDate.of(request.getYear(), request.getMonth(), 1))
                 .le(BankTransaction::getTransactionDate, LocalDate.of(request.getYear(), request.getMonth(), 1).plusMonths(1).minusDays(1));
        List<BankTransaction> unmatchedTransactions = this.list(txWrapper);

        if (unmatchedTransactions.isEmpty()) {
            return 0;
        }

        // 查询该月份已过账的凭证（状态=2）
        LambdaQueryWrapper<Voucher> vWrapper = new LambdaQueryWrapper<>();
        vWrapper.eq(Voucher::getAccountSetId, request.getAccountSetId())
                .eq(Voucher::getYear, request.getYear())
                .eq(Voucher::getMonth, request.getMonth())
                .eq(Voucher::getStatus, 2);
        List<Voucher> vouchers = voucherMapper.selectList(vWrapper);

        if (vouchers.isEmpty()) {
            return 0;
        }

        // 查询凭证明细
        List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(VoucherDetail::getVoucherId, voucherIds);
        List<VoucherDetail> details = voucherDetailMapper.selectList(detailWrapper);

        // 按凭证ID分组明细
        Map<Long, List<VoucherDetail>> detailsByVoucherId = details.stream()
                .collect(Collectors.groupingBy(VoucherDetail::getVoucherId));

        int matchCount = 0;
        // 记录已匹配的凭证ID,避免同一凭证被多条流水重复匹配
        java.util.Set<Long> matchedVoucherIds = new java.util.HashSet<>();

        for (BankTransaction transaction : unmatchedTransactions) {
            BigDecimal txAmount = transaction.getAmount();
            LocalDate txDate = transaction.getTransactionDate();
            Integer txType = transaction.getTransactionType();

            for (Voucher voucher : vouchers) {
                // 跳过已被其他流水匹配的凭证
                if (matchedVoucherIds.contains(voucher.getId())) {
                    continue;
                }
                List<VoucherDetail> voucherDetails = detailsByVoucherId.get(voucher.getId());
                if (voucherDetails == null) {
                    continue;
                }

                // 匹配规则：金额相同且日期相同;transactionType为空时跳过
                boolean matched = false;
                if (txType != null) {
                    for (VoucherDetail detail : voucherDetails) {
                        // 仅匹配银行存款(1002)科目明细,避免误匹配其他科目
                        if (!"1002".equals(detail.getSubjectCode())) {
                            continue;
                        }
                        BigDecimal detailAmount = txType == 1
                                ? detail.getDebit() : detail.getCredit();
                        if (detailAmount != null && detailAmount.compareTo(txAmount) == 0
                                && txDate != null && txDate.equals(voucher.getVoucherDate())) {
                            matched = true;
                            break;
                        }
                    }
                }

                if (matched) {
                    transaction.setMatchedStatus(1);
                    transaction.setVoucherId(voucher.getId());
                    this.updateById(transaction);
                    matchedVoucherIds.add(voucher.getId());
                    matchCount++;
                    break;
                }
            }
        }

        return matchCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualMatch(ManualMatchRequest request) {
        // 验证凭证是否存在
        Voucher voucher = voucherMapper.selectById(request.getVoucherId());
        if (voucher == null) {
            throw new BusinessException("凭证不存在");
        }
        // 凭证必须已过账(status=2)才能参与对账，与autoMatch/smartMatch保持一致，
        // 防止草稿/未过账/已作废凭证被对账造成账务数据脱节
        if (voucher.getStatus() == null || voucher.getStatus() != 2) {
            throw new BusinessException("凭证未过账，不可对账");
        }
        // 校验凭证归属当前账套
        if (!request.getAccountSetId().equals(voucher.getAccountSetId())) {
            throw new BusinessException("凭证不属于当前账套");
        }

        // 更新银行流水匹配状态
        List<BankTransaction> transactions = this.listByIds(request.getTransactionIds());
        for (BankTransaction transaction : transactions) {
            if (!transaction.getAccountSetId().equals(request.getAccountSetId())) {
                throw new BusinessException("银行流水不属于当前账套");
            }
            // 已勾对的流水不可重复勾对,否则原voucherId被静默覆盖,丢失原勾对关系且无审计痕迹
            if (transaction.getMatchedStatus() != null && transaction.getMatchedStatus() == 1) {
                throw new BusinessException("流水" + transaction.getTransactionNo() + "已勾对，请先取消匹配");
            }
            transaction.setMatchedStatus(1);
            transaction.setVoucherId(request.getVoucherId());
            this.updateById(transaction);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelMatch(Long transactionId) {
        BankTransaction transaction = this.getById(transactionId);
        if (transaction == null) {
            throw new BusinessException("银行流水不存在");
        }
        // IDOR治理:校验当前用户对该流水所属账套的所有者权限
        accountSetAccessService.checkOwner(transaction.getAccountSetId());
        if (transaction.getMatchedStatus() == null || transaction.getMatchedStatus() != 1) {
            throw new BusinessException("该流水未匹配，无法取消");
        }

        transaction.setMatchedStatus(0);
        transaction.setVoucherId(null);
        // 使用LambdaUpdateWrapper显式set null，避免MyBatis-Plus默认NOT_NULL策略不更新null字段
        LambdaUpdateWrapper<BankTransaction> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BankTransaction::getId, transactionId)
                     .set(BankTransaction::getMatchedStatus, 0)
                     .set(BankTransaction::getVoucherId, null);
        this.update(updateWrapper);

        // 同步更新对应月份对账单(BankReconciliation)的未对账项数与状态
        // 否则取消勾对后,流水未匹配数已增加,但对账单仍显示已对账(status=1),造成账实不符
        syncReconciliationAfterCancel(transaction);
    }

    /**
     * 取消匹配后同步更新对应月份对账单的未对账项数与状态
     */
    private void syncReconciliationAfterCancel(BankTransaction transaction) {
        if (transaction.getTransactionDate() == null) {
            return;
        }
        int year = transaction.getTransactionDate().getYear();
        int month = transaction.getTransactionDate().getMonthValue();

        LambdaQueryWrapper<BankReconciliation> reconWrapper = new LambdaQueryWrapper<>();
        reconWrapper.eq(BankReconciliation::getAccountSetId, transaction.getAccountSetId())
                    .eq(BankReconciliation::getBankAccount, transaction.getBankAccount())
                    .eq(BankReconciliation::getYear, year)
                    .eq(BankReconciliation::getMonth, month);
        BankReconciliation reconciliation = bankReconciliationMapper.selectOne(reconWrapper);
        if (reconciliation == null) {
            // 当月未生成对账单,无需同步
            return;
        }

        // 重新统计当月未匹配流水数
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        LambdaQueryWrapper<BankTransaction> txWrapper = new LambdaQueryWrapper<>();
        txWrapper.eq(BankTransaction::getAccountSetId, transaction.getAccountSetId())
                 .eq(BankTransaction::getBankAccount, transaction.getBankAccount())
                 .ge(BankTransaction::getTransactionDate, startDate)
                 .le(BankTransaction::getTransactionDate, endDate);
        List<BankTransaction> monthTransactions = this.list(txWrapper);
        long unreconciledCount = monthTransactions.stream()
                .filter(t -> t.getMatchedStatus() == null || t.getMatchedStatus() == 0)
                .count();

        reconciliation.setUnreconciledItems((int) unreconciledCount);
        // 存在未匹配项时状态置为未对账(0),全部匹配时为已对账(1)
        reconciliation.setStatus(unreconciledCount == 0 ? 1 : 0);
        bankReconciliationMapper.updateById(reconciliation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BankReconciliationVO generateReconciliation(ReconciliationGenerateRequest request) {
        // 检查是否已存在对账单
        LambdaQueryWrapper<BankReconciliation> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(BankReconciliation::getAccountSetId, request.getAccountSetId())
                    .eq(BankReconciliation::getBankAccount, request.getBankAccount())
                    .eq(BankReconciliation::getYear, request.getYear())
                    .eq(BankReconciliation::getMonth, request.getMonth());
        BankReconciliation existing = bankReconciliationMapper.selectOne(existWrapper);
        if (existing != null) {
            throw new BusinessException("该月份对账单已存在，请勿重复生成");
        }

        LocalDate startDate = LocalDate.of(request.getYear(), request.getMonth(), 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // 查询该月银行流水
        LambdaQueryWrapper<BankTransaction> txWrapper = new LambdaQueryWrapper<>();
        txWrapper.eq(BankTransaction::getAccountSetId, request.getAccountSetId())
                 .eq(BankTransaction::getBankAccount, request.getBankAccount())
                 .ge(BankTransaction::getTransactionDate, startDate)
                 .le(BankTransaction::getTransactionDate, endDate)
                 .orderByAsc(BankTransaction::getTransactionDate);
        List<BankTransaction> transactions = this.list(txWrapper);

        // 查询上月对账单获取期初余额（无上月则期初为0），月度对账需叠加期初余额才有意义
        int prevYear = request.getMonth() == 1 ? request.getYear() - 1 : request.getYear();
        int prevMonth = request.getMonth() == 1 ? 12 : request.getMonth() - 1;
        LambdaQueryWrapper<BankReconciliation> prevWrapper = new LambdaQueryWrapper<>();
        prevWrapper.eq(BankReconciliation::getAccountSetId, request.getAccountSetId())
                   .eq(BankReconciliation::getBankAccount, request.getBankAccount())
                   .eq(BankReconciliation::getYear, prevYear)
                   .eq(BankReconciliation::getMonth, prevMonth);
        BankReconciliation prevReconciliation = bankReconciliationMapper.selectOne(prevWrapper);
        BigDecimal prevBankBalance = prevReconciliation != null && prevReconciliation.getBankBalance() != null
                ? prevReconciliation.getBankBalance() : BigDecimal.ZERO;
        BigDecimal prevBookBalance = prevReconciliation != null && prevReconciliation.getBookBalance() != null
                ? prevReconciliation.getBookBalance() : BigDecimal.ZERO;

        // 计算银行余额 = 期初余额 + 收入合计 - 支出合计（amount为null时跳过避免NPE）
        BigDecimal bankIncome = transactions.stream()
                .filter(t -> t.getTransactionType() != null && t.getTransactionType() == 1)
                .filter(t -> t.getAmount() != null)
                .map(BankTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bankExpense = transactions.stream()
                .filter(t -> t.getTransactionType() != null && t.getTransactionType() == 2)
                .filter(t -> t.getAmount() != null)
                .map(BankTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bankBalance = prevBankBalance.add(bankIncome).subtract(bankExpense);

        // 查询账簿余额（从凭证明细中计算）,只统计银行存款(1002)科目的借贷
        LambdaQueryWrapper<Voucher> vWrapper = new LambdaQueryWrapper<>();
        vWrapper.eq(Voucher::getAccountSetId, request.getAccountSetId())
                .eq(Voucher::getYear, request.getYear())
                .eq(Voucher::getMonth, request.getMonth())
                .eq(Voucher::getStatus, 2);
        List<Voucher> vouchers = voucherMapper.selectList(vWrapper);

        // 账簿余额 = 期初余额 + 借方合计 - 贷方合计（与银行余额口径保持一致）
        BigDecimal bookBalance = prevBookBalance;
        if (!vouchers.isEmpty()) {
            List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());
            LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                    .eq(VoucherDetail::getSubjectCode, "1002");
            List<VoucherDetail> bankDetails = voucherDetailMapper.selectList(detailWrapper);

            // 只计算银行存款(1002)科目的借贷合计,否则复式记账借贷相等会导致账面余额恒为0
            BigDecimal bookDebit = bankDetails.stream()
                    .map(d -> d.getDebit() != null ? d.getDebit() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal bookCredit = bankDetails.stream()
                    .map(d -> d.getCredit() != null ? d.getCredit() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            bookBalance = prevBookBalance.add(bookDebit).subtract(bookCredit);
        }

        // 统计未匹配项数
        long unreconciledCount = transactions.stream()
                .filter(t -> t.getMatchedStatus() == null || t.getMatchedStatus() == 0)
                .count();

        // 保存对账结果
        BankReconciliation reconciliation = new BankReconciliation();
        reconciliation.setAccountSetId(request.getAccountSetId());
        reconciliation.setBankAccount(request.getBankAccount());
        reconciliation.setYear(request.getYear());
        reconciliation.setMonth(request.getMonth());
        reconciliation.setBankBalance(bankBalance);
        reconciliation.setBookBalance(bookBalance);
        reconciliation.setUnreconciledItems((int) unreconciledCount);
        reconciliation.setReconciledDate(LocalDate.now());
        reconciliation.setReconciledBy(SecurityUtils.getCurrentUserId());
        reconciliation.setStatus(unreconciledCount == 0 ? 1 : 0);
        reconciliation.setRemark(request.getRemark());
        bankReconciliationMapper.insert(reconciliation);

        return convertReconciliationToVO(reconciliation, transactions);
    }

    @Override
    public BankReconciliationVO getReconciliation(Long id) {
        BankReconciliation reconciliation = bankReconciliationMapper.selectById(id);
        if (reconciliation == null) {
            throw new BusinessException("对账单不存在");
        }
        // IDOR治理:校验当前用户对该对账单所属账套的访问权
        accountSetAccessService.checkAccess(reconciliation.getAccountSetId());

        // 查询该月银行流水
        LocalDate startDate = LocalDate.of(reconciliation.getYear(), reconciliation.getMonth(), 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        LambdaQueryWrapper<BankTransaction> txWrapper = new LambdaQueryWrapper<>();
        txWrapper.eq(BankTransaction::getAccountSetId, reconciliation.getAccountSetId())
                 .eq(BankTransaction::getBankAccount, reconciliation.getBankAccount())
                 .ge(BankTransaction::getTransactionDate, startDate)
                 .le(BankTransaction::getTransactionDate, endDate)
                 .orderByAsc(BankTransaction::getTransactionDate);
        List<BankTransaction> transactions = this.list(txWrapper);

        return convertReconciliationToVO(reconciliation, transactions);
    }

    @Override
    public PageResult<BankReconciliationVO> pageReconciliations(BankTransactionQueryRequest request) {
        Page<BankReconciliation> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<BankReconciliation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankReconciliation::getAccountSetId, request.getAccountSetId())
               .eq(StrUtil.isNotBlank(request.getBankAccount()), BankReconciliation::getBankAccount, request.getBankAccount())
               .orderByDesc(BankReconciliation::getYear)
               .orderByDesc(BankReconciliation::getMonth);

        Page<BankReconciliation> result = bankReconciliationMapper.selectPage(page, wrapper);

        List<BankReconciliationVO> voList = result.getRecords().stream()
                .map(r -> convertReconciliationToVO(r, new ArrayList<>()))
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public List<Map<String, Object>> smartMatch(Long accountSetId) {
        // 1. 查询未匹配的银行流水
        LambdaQueryWrapper<BankTransaction> txWrapper = new LambdaQueryWrapper<>();
        txWrapper.eq(BankTransaction::getAccountSetId, accountSetId)
                 .eq(BankTransaction::getMatchedStatus, 0);
        List<BankTransaction> unmatchedTransactions = this.list(txWrapper);

        if (unmatchedTransactions.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 查询未匹配的已过账凭证（状态=2）
        LambdaQueryWrapper<Voucher> vWrapper = new LambdaQueryWrapper<>();
        vWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getStatus, 2);
        List<Voucher> vouchers = voucherMapper.selectList(vWrapper);

        if (vouchers.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询凭证明细
        List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(VoucherDetail::getVoucherId, voucherIds);
        List<VoucherDetail> details = voucherDetailMapper.selectList(detailWrapper);

        // 按凭证ID分组明细
        Map<Long, List<VoucherDetail>> detailsByVoucherId = details.stream()
                .collect(Collectors.groupingBy(VoucherDetail::getVoucherId));

        List<Map<String, Object>> suggestions = new ArrayList<>();

        // 3. 对每条流水进行匹配
        for (BankTransaction transaction : unmatchedTransactions) {
            BigDecimal txAmount = transaction.getAmount();
            LocalDate txDate = transaction.getTransactionDate();
            String txSummary = transaction.getSummary() != null ? transaction.getSummary() : "";
            Integer txType = transaction.getTransactionType();

            // transactionType为null无法判断借贷方向,跳过
            if (txType == null) {
                continue;
            }

            Voucher matchedVoucher = null;
            String matchType = null;

            // 先尝试精确匹配（金额相同+日期相同）,仅匹配银行存款(1002)明细
            for (Voucher voucher : vouchers) {
                List<VoucherDetail> voucherDetails = detailsByVoucherId.get(voucher.getId());
                if (voucherDetails == null) {
                    continue;
                }

                for (VoucherDetail detail : voucherDetails) {
                    if (!"1002".equals(detail.getSubjectCode())) {
                        continue;
                    }
                    BigDecimal detailAmount = txType == 1
                            ? detail.getDebit() : detail.getCredit();
                    if (detailAmount != null && detailAmount.compareTo(txAmount) == 0
                            && txDate != null && txDate.equals(voucher.getVoucherDate())) {
                        matchedVoucher = voucher;
                        matchType = "exact";
                        break;
                    }
                }
                if (matchedVoucher != null) {
                    break;
                }
            }

            // 再尝试模糊匹配（金额相同+摘要包含关键词）
            if (matchedVoucher == null) {
                for (Voucher voucher : vouchers) {
                    List<VoucherDetail> voucherDetails = detailsByVoucherId.get(voucher.getId());
                    if (voucherDetails == null) {
                        continue;
                    }

                    for (VoucherDetail detail : voucherDetails) {
                        if (!"1002".equals(detail.getSubjectCode())) {
                            continue;
                        }
                        BigDecimal detailAmount = txType == 1
                                ? detail.getDebit() : detail.getCredit();
                        if (detailAmount != null && detailAmount.compareTo(txAmount) == 0) {
                            String detailSummary = detail.getSummary() != null ? detail.getSummary() : "";
                            if (StrUtil.isNotBlank(txSummary) && StrUtil.isNotBlank(detailSummary)
                                    && (txSummary.contains(detailSummary) || detailSummary.contains(txSummary))) {
                                matchedVoucher = voucher;
                                matchType = "fuzzy";
                                break;
                            }
                        }
                    }
                    if (matchedVoucher != null) {
                        break;
                    }
                }
            }

            if (matchedVoucher != null) {
                Map<String, Object> suggestion = new HashMap<>();
                suggestion.put("transactionId", transaction.getId());
                suggestion.put("transactionDate", txDate);
                suggestion.put("transactionAmount", txAmount);
                suggestion.put("transactionSummary", transaction.getSummary());
                suggestion.put("transactionType", transaction.getTransactionType());
                suggestion.put("voucherId", matchedVoucher.getId());
                suggestion.put("voucherNo", matchedVoucher.getVoucherNo());
                suggestion.put("voucherDate", matchedVoucher.getVoucherDate());
                suggestion.put("matchType", matchType);
                suggestion.put("matchTypeName", "exact".equals(matchType) ? "精确匹配" : "模糊匹配");
                suggestions.add(suggestion);
            }
        }

        return suggestions;
    }

    @Override
    public byte[] exportReconciliation(Long reconciliationId) {
        BankReconciliationVO vo = getReconciliation(reconciliationId);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("余额调节表");

            // 创建标题行样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("余额调节表 " + vo.getYear() + "年" + vo.getMonth() + "月");
            titleCell.setCellStyle(headerStyle);

            // 表头
            Row headerRow = sheet.createRow(2);
            headerRow.createCell(0).setCellValue("项目");
            headerRow.createCell(1).setCellValue("金额");
            for (int i = 0; i < 2; i++) {
                headerRow.getCell(i).setCellStyle(headerStyle);
            }

            // 数据行
            int rowNum = 3;
            rowNum = writeReconciliationRow(sheet, rowNum, "银行存款余额", vo.getBankBalance());
            rowNum = writeReconciliationRow(sheet, rowNum, "账面余额", vo.getBookBalance());
            rowNum = writeReconciliationRow(sheet, rowNum, "差异", vo.getDifference());
            rowNum = writeReconciliationRow(sheet, rowNum, "未达账项数量",
                    vo.getUnreconciledItems() != null ? new BigDecimal(vo.getUnreconciledItems()) : BigDecimal.ZERO);
            rowNum = writeReconciliationRow(sheet, rowNum, "对账状态", vo.getStatusName());
            rowNum = writeReconciliationRow(sheet, rowNum, "对账日期",
                    vo.getReconciledDate() != null ? vo.getReconciledDate().toString() : "");
            rowNum = writeReconciliationRow(sheet, rowNum, "对账人",
                    vo.getReconciledByName() != null ? vo.getReconciledByName() : "");
            if (StrUtil.isNotBlank(vo.getRemark())) {
                rowNum = writeReconciliationRow(sheet, rowNum, "备注", vo.getRemark());
            }

            // 未达账项明细
            if (vo.getUnreconciledTransactions() != null && !vo.getUnreconciledTransactions().isEmpty()) {
                rowNum++;
                Row sectionRow = sheet.createRow(rowNum++);
                Cell sectionCell = sectionRow.createCell(0);
                sectionCell.setCellValue("未达账项明细");
                sectionCell.setCellStyle(headerStyle);

                Row detailHeaderRow = sheet.createRow(rowNum++);
                detailHeaderRow.createCell(0).setCellValue("交易日期");
                detailHeaderRow.createCell(1).setCellValue("交易类型");
                detailHeaderRow.createCell(2).setCellValue("金额");
                detailHeaderRow.createCell(3).setCellValue("摘要");
                detailHeaderRow.createCell(4).setCellValue("对方账户");
                for (int i = 0; i < 5; i++) {
                    detailHeaderRow.getCell(i).setCellStyle(headerStyle);
                }

                for (BankTransactionVO tx : vo.getUnreconciledTransactions()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(tx.getTransactionDate() != null ? tx.getTransactionDate().toString() : "");
                    row.createCell(1).setCellValue(tx.getTransactionTypeName() != null ? tx.getTransactionTypeName() : "");
                    row.createCell(2).setCellValue(tx.getAmount() != null ? tx.getAmount().doubleValue() : 0);
                    row.createCell(3).setCellValue(tx.getSummary() != null ? tx.getSummary() : "");
                    row.createCell(4).setCellValue(tx.getCounterparty() != null ? tx.getCounterparty() : "");
                }
            }

            // 设置列宽
            sheet.setColumnWidth(0, 25 * 256);
            sheet.setColumnWidth(1, 20 * 256);
            sheet.setColumnWidth(2, 15 * 256);
            sheet.setColumnWidth(3, 30 * 256);
            sheet.setColumnWidth(4, 20 * 256);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("导出余额调节表失败", e);
            throw new BusinessException("导出余额调节表失败");
        }
    }

    @Override
    public List<UnmatchedItemVO> listUnmatchedItems(Long accountSetId, Integer year, Integer month) {
        if (accountSetId == null) {
            throw new BusinessException("账套ID不能为空");
        }

        LambdaQueryWrapper<BankTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankTransaction::getAccountSetId, accountSetId)
               .eq(BankTransaction::getMatchedStatus, 0);

        // 按年月过滤
        if (year != null && month != null) {
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.plusMonths(1).minusDays(1);
            wrapper.ge(BankTransaction::getTransactionDate, startDate)
                   .le(BankTransaction::getTransactionDate, endDate);
        } else if (year != null) {
            LocalDate startDate = LocalDate.of(year, 1, 1);
            LocalDate endDate = LocalDate.of(year, 12, 31);
            wrapper.ge(BankTransaction::getTransactionDate, startDate)
                   .le(BankTransaction::getTransactionDate, endDate);
        }

        wrapper.orderByAsc(BankTransaction::getTransactionDate);
        List<BankTransaction> transactions = this.list(wrapper);

        return transactions.stream()
                .map(this::convertToUnmatchedItemVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateVoucherFromUnmatched(Long transactionId) {
        BankTransaction transaction = this.getById(transactionId);
        if (transaction == null) {
            throw new BusinessException("银行流水不存在");
        }
        // IDOR治理:校验当前用户对该流水所属账套的所有者权限(写凭证)
        accountSetAccessService.checkOwner(transaction.getAccountSetId());
        if (transaction.getMatchedStatus() != null && transaction.getMatchedStatus() == 1) {
            throw new BusinessException("该银行流水已匹配，无需生成凭证");
        }

        // 复用银行流水生成凭证的逻辑：
        // 收入: 借银行存款 贷应收账款/主营业务收入
        // 支出: 借应付账款/管理费用 贷银行存款
        Long voucherId = bankVoucherService.generateVoucher(transactionId);
        log.info("未达账项生成凭证成功，流水ID: {}, 凭证ID: {}", transactionId, voucherId);
        return voucherId;
    }

    /**
     * 银行流水转未达账项VO
     */
    private UnmatchedItemVO convertToUnmatchedItemVO(BankTransaction transaction) {
        UnmatchedItemVO vo = new UnmatchedItemVO();
        vo.setTransactionId(transaction.getId());
        vo.setTransactionDate(transaction.getTransactionDate());
        vo.setAmount(transaction.getAmount());
        vo.setSummary(transaction.getSummary());
        // transactionType为null时不误判为支出，null判断放在前面
        Integer txType = transaction.getTransactionType();
        if (txType != null) {
            vo.setType(txType == 1 ? "收入" : "支出");
        }
        return vo;
    }

    /**
     * 写入余额调节表数据行
     */
    private int writeReconciliationRow(Sheet sheet, int rowNum, String label, Object value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        Cell valueCell = row.createCell(1);
        if (value instanceof BigDecimal) {
            valueCell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof String) {
            valueCell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            valueCell.setCellValue((Integer) value);
        }
        return rowNum + 1;
    }

    /**
     * 银行流水实体转VO
     */
    private BankTransactionVO convertTransactionToVO(BankTransaction transaction) {
        BankTransactionVO vo = new BankTransactionVO();
        BeanUtil.copyProperties(transaction, vo);

        // 交易类型名称
        if (transaction.getTransactionType() != null) {
            vo.setTransactionTypeName(transaction.getTransactionType() == 1 ? "收入" : "支出");
        }

        // 匹配状态名称
        if (transaction.getMatchedStatus() != null) {
            vo.setMatchedStatusName(transaction.getMatchedStatus() == 1 ? "已匹配" : "未匹配");
        }

        // 凭证号
        if (transaction.getVoucherId() != null) {
            Voucher voucher = voucherMapper.selectById(transaction.getVoucherId());
            if (voucher != null) {
                vo.setVoucherNo(voucher.getVoucherNo());
            }
        }

        // 创建人名称
        if (transaction.getCreateBy() != null) {
            SysUser user = sysUserMapper.selectById(transaction.getCreateBy());
            if (user != null) {
                vo.setCreateByName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            }
        }

        return vo;
    }

    /**
     * 对账结果实体转VO
     */
    private BankReconciliationVO convertReconciliationToVO(BankReconciliation reconciliation,
                                                            List<BankTransaction> transactions) {
        BankReconciliationVO vo = new BankReconciliationVO();
        BeanUtil.copyProperties(reconciliation, vo);

        // 差异 = 银行余额 - 账簿余额（null安全，历史/异常数据可能为null）
        BigDecimal bankBalance = reconciliation.getBankBalance() != null ? reconciliation.getBankBalance() : BigDecimal.ZERO;
        BigDecimal bookBalance = reconciliation.getBookBalance() != null ? reconciliation.getBookBalance() : BigDecimal.ZERO;
        vo.setDifference(bankBalance.subtract(bookBalance));

        // 状态名称
        if (reconciliation.getStatus() != null) {
            vo.setStatusName(reconciliation.getStatus() == 1 ? "已对账" : "未对账");
        }

        // 对账人名称
        if (reconciliation.getReconciledBy() != null) {
            SysUser user = sysUserMapper.selectById(reconciliation.getReconciledBy());
            if (user != null) {
                vo.setReconciledByName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            }
        }

        // 创建人名称
        if (reconciliation.getCreateBy() != null) {
            SysUser user = sysUserMapper.selectById(reconciliation.getCreateBy());
            if (user != null) {
                vo.setCreateByName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            }
        }

        // 未达账项列表
        List<BankTransactionVO> unreconciledList = transactions.stream()
                .filter(t -> t.getMatchedStatus() == null || t.getMatchedStatus() == 0)
                .map(this::convertTransactionToVO)
                .collect(Collectors.toList());
        vo.setUnreconciledTransactions(unreconciledList);

        return vo;
    }
}
