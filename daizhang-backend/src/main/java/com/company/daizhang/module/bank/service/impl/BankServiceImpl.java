package com.company.daizhang.module.bank.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.bank.dto.*;
import com.company.daizhang.module.bank.entity.BankReconciliation;
import com.company.daizhang.module.bank.entity.BankTransaction;
import com.company.daizhang.module.bank.mapper.BankReconciliationMapper;
import com.company.daizhang.module.bank.mapper.BankTransactionMapper;
import com.company.daizhang.module.bank.service.BankService;
import com.company.daizhang.module.bank.vo.BankReconciliationVO;
import com.company.daizhang.module.bank.vo.BankTransactionVO;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.entity.VoucherDetail;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 银行对账服务实现
 */
@Service
@RequiredArgsConstructor
public class BankServiceImpl extends ServiceImpl<BankTransactionMapper, BankTransaction> implements BankService {

    private final BankReconciliationMapper bankReconciliationMapper;
    private final VoucherMapper voucherMapper;
    private final VoucherDetailMapper voucherDetailMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer importBankTransactions(BankTransactionImportRequest request) {
        List<BankTransactionImportRequest.BankTransactionItem> items = request.getTransactions();
        int count = 0;

        for (BankTransactionImportRequest.BankTransactionItem item : items) {
            // 根据交易流水号去重
            if (StrUtil.isNotBlank(item.getTransactionNo())) {
                LambdaQueryWrapper<BankTransaction> existWrapper = new LambdaQueryWrapper<>();
                existWrapper.eq(BankTransaction::getAccountSetId, request.getAccountSetId())
                            .eq(BankTransaction::getBankAccount, request.getBankAccount())
                            .eq(BankTransaction::getTransactionNo, item.getTransactionNo());
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

        for (BankTransaction transaction : unmatchedTransactions) {
            BigDecimal txAmount = transaction.getAmount();
            LocalDate txDate = transaction.getTransactionDate();

            for (Voucher voucher : vouchers) {
                List<VoucherDetail> voucherDetails = detailsByVoucherId.get(voucher.getId());
                if (voucherDetails == null) {
                    continue;
                }

                // 匹配规则：金额相同且日期相同
                boolean matched = false;
                for (VoucherDetail detail : voucherDetails) {
                    BigDecimal detailAmount = transaction.getTransactionType() == 1
                            ? detail.getDebit() : detail.getCredit();
                    if (detailAmount != null && detailAmount.compareTo(txAmount) == 0
                            && voucher.getVoucherDate().equals(txDate)) {
                        matched = true;
                        break;
                    }
                }

                if (matched) {
                    transaction.setMatchedStatus(1);
                    transaction.setVoucherId(voucher.getId());
                    this.updateById(transaction);
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

        // 更新银行流水匹配状态
        List<BankTransaction> transactions = this.listByIds(request.getTransactionIds());
        for (BankTransaction transaction : transactions) {
            if (!transaction.getAccountSetId().equals(request.getAccountSetId())) {
                throw new BusinessException("银行流水不属于当前账套");
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
        if (transaction.getMatchedStatus() != 1) {
            throw new BusinessException("该流水未匹配，无法取消");
        }

        transaction.setMatchedStatus(0);
        transaction.setVoucherId(null);
        this.updateById(transaction);
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

        // 计算银行余额 = 收入合计 - 支出合计
        BigDecimal bankIncome = transactions.stream()
                .filter(t -> t.getTransactionType() != null && t.getTransactionType() == 1)
                .map(BankTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bankExpense = transactions.stream()
                .filter(t -> t.getTransactionType() != null && t.getTransactionType() == 2)
                .map(BankTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bankBalance = bankIncome.subtract(bankExpense);

        // 查询账簿余额（从凭证明细中计算）
        LambdaQueryWrapper<Voucher> vWrapper = new LambdaQueryWrapper<>();
        vWrapper.eq(Voucher::getAccountSetId, request.getAccountSetId())
                .eq(Voucher::getYear, request.getYear())
                .eq(Voucher::getMonth, request.getMonth())
                .eq(Voucher::getStatus, 2);
        List<Voucher> vouchers = voucherMapper.selectList(vWrapper);

        BigDecimal bookBalance = BigDecimal.ZERO;
        if (!vouchers.isEmpty()) {
            List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());
            LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.in(VoucherDetail::getVoucherId, voucherIds);
            List<VoucherDetail> details = voucherDetailMapper.selectList(detailWrapper);

            // 计算银行科目相关的借贷合计
            BigDecimal bookDebit = details.stream()
                    .map(d -> d.getDebit() != null ? d.getDebit() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal bookCredit = details.stream()
                    .map(d -> d.getCredit() != null ? d.getCredit() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            bookBalance = bookDebit.subtract(bookCredit);
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

        // 差异 = 银行余额 - 账簿余额
        vo.setDifference(reconciliation.getBankBalance().subtract(reconciliation.getBookBalance()));

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
