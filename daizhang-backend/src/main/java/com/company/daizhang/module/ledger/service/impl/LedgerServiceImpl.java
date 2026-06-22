package com.company.daizhang.module.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.accountset.mapper.AccountSetMapper;
import com.company.daizhang.module.ledger.dto.LedgerQueryRequest;
import com.company.daizhang.module.ledger.dto.SubjectBalanceQueryRequest;
import com.company.daizhang.module.ledger.service.LedgerService;
import com.company.daizhang.module.ledger.vo.CashJournalVO;
import com.company.daizhang.module.ledger.vo.DetailLedgerVO;
import com.company.daizhang.module.ledger.vo.GeneralLedgerVO;
import com.company.daizhang.module.ledger.vo.SubjectBalanceVO;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.entity.VoucherDetail;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 账簿查询服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final VoucherMapper voucherMapper;
    private final VoucherDetailMapper voucherDetailMapper;
    private final SubjectMapper subjectMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final AccountSetMapper accountSetMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<DetailLedgerVO> detailLedger(LedgerQueryRequest request) {
        Long accountSetId = request.getAccountSetId();
        Long subjectId = request.getSubjectId();
        Integer year = request.getYear();
        Integer month = request.getMonth();

        // 业务校验：账套ID不能为空
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }

        // 业务校验：科目ID不能为空
        if (subjectId == null) {
            throw new BusinessException(ErrorCode.LEDGER_SUBJECT_ID_BLANK);
        }

        // 业务校验：年度不能为空
        if (year == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "年度不能为空");
        }

        // 业务校验：年度必须合理（1900-2099）
        if (year < 1900 || year > 2099) {
            throw new BusinessException(ErrorCode.LEDGER_YEAR_INVALID);
        }

        // 业务校验：月份必须在1-12之间
        if (month != null && (month < 1 || month > 12)) {
            throw new BusinessException(ErrorCode.LEDGER_MONTH_INVALID);
        }

        // 业务校验：开始日期不能大于结束日期
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new BusinessException(ErrorCode.LEDGER_DATE_RANGE_INVALID);
            }
        }

        // 业务校验：账套必须存在
        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }

        // 业务校验：科目必须存在
        Subject subject = subjectMapper.selectById(subjectId);
        if (subject == null) {
            throw new BusinessException(ErrorCode.LEDGER_SUBJECT_NOT_FOUND);
        }

        // 业务校验：科目必须属于该账套
        if (!accountSetId.equals(subject.getAccountSetId())) {
            throw new BusinessException(ErrorCode.LEDGER_SUBJECT_NOT_FOUND, "科目不属于该账套");
        }

        log.info("查询明细账，账套ID: {}, 科目ID: {}, 年度: {}, 月份: {}", accountSetId, subjectId, year, month);

        // 查询已过账的凭证
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getStatus, 2)
                .eq(Voucher::getYear, year);
        if (month != null) {
            voucherWrapper.eq(Voucher::getMonth, month);
        }
        if (request.getStartDate() != null) {
            voucherWrapper.ge(Voucher::getVoucherDate, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            voucherWrapper.le(Voucher::getVoucherDate, request.getEndDate());
        }
        voucherWrapper.orderByAsc(Voucher::getVoucherDate, Voucher::getVoucherNo);
        List<Voucher> vouchers = voucherMapper.selectList(voucherWrapper);

        if (vouchers.isEmpty()) {
            log.info("未找到已过账的凭证");
            return PageResult.of(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize());
        }

        List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());

        // 查询凭证明细
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                .eq(VoucherDetail::getSubjectId, subjectId);
        if (request.getAuxiliaryId() != null) {
            detailWrapper.eq(VoucherDetail::getAuxiliaryId, request.getAuxiliaryId());
        }
        detailWrapper.orderByAsc(VoucherDetail::getSortOrder);
        List<VoucherDetail> details = voucherDetailMapper.selectList(detailWrapper);

        // 构建凭证ID到凭证的映射
        Map<Long, Voucher> voucherMap = vouchers.stream()
                .collect(Collectors.toMap(Voucher::getId, v -> v));

        // 构建明细账VO列表
        int balanceDirection = (subject.getBalanceDirection() != null)
                ? subject.getBalanceDirection() : 1;

        // 获取期初余额
        BigDecimal beginBalance = BigDecimal.ZERO;
        if (month != null) {
            LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
            balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                    .eq(AccountBalance::getSubjectId, subjectId)
                    .eq(AccountBalance::getYear, year)
                    .eq(AccountBalance::getMonth, month);
            AccountBalance accountBalance = accountBalanceMapper.selectOne(balanceWrapper);
            if (accountBalance != null) {
                if (balanceDirection == 1) {
                    beginBalance = accountBalance.getBeginDebit().subtract(accountBalance.getBeginCredit());
                } else {
                    beginBalance = accountBalance.getBeginCredit().subtract(accountBalance.getBeginDebit());
                }
            }
        }

        List<DetailLedgerVO> allRows = new ArrayList<>();
        BigDecimal runningBalance = beginBalance;

        for (VoucherDetail detail : details) {
            Voucher voucher = voucherMap.get(detail.getVoucherId());
            if (voucher == null) {
                continue;
            }

            DetailLedgerVO vo = new DetailLedgerVO();
            vo.setVoucherDate(voucher.getVoucherDate());
            vo.setVoucherNo(String.valueOf(voucher.getVoucherNo()));
            vo.setSummary(detail.getSummary());
            vo.setSubjectCode(detail.getSubjectCode());
            vo.setSubjectName(detail.getSubjectName());
            vo.setDebit(detail.getDebit() != null ? detail.getDebit() : BigDecimal.ZERO);
            vo.setCredit(detail.getCredit() != null ? detail.getCredit() : BigDecimal.ZERO);

            // 计算方向和余额
            if (balanceDirection == 1) {
                runningBalance = runningBalance.add(vo.getDebit()).subtract(vo.getCredit());
            } else {
                runningBalance = runningBalance.add(vo.getCredit()).subtract(vo.getDebit());
            }
            vo.setDirection(runningBalance.compareTo(BigDecimal.ZERO) >= 0 ? "借" : "贷");
            vo.setBalance(runningBalance.abs());

            allRows.add(vo);
        }

        // 手动分页
        int total = allRows.size();
        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<DetailLedgerVO> pageList;
        if (fromIndex >= total) {
            pageList = Collections.emptyList();
        } else {
            pageList = allRows.subList(fromIndex, toIndex);
        }

        log.info("查询明细账成功，共 {} 条记录", total);
        return PageResult.of(pageList, (long) total, pageNum, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeneralLedgerVO> generalLedger(LedgerQueryRequest request) {
        Long accountSetId = request.getAccountSetId();
        Integer year = request.getYear();
        Integer month = request.getMonth() != null ? request.getMonth() : 1;

        // 业务校验：账套ID不能为空
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }

        // 业务校验：年度不能为空
        if (year == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "年度不能为空");
        }

        // 业务校验：年度必须合理（1900-2099）
        if (year < 1900 || year > 2099) {
            throw new BusinessException(ErrorCode.LEDGER_YEAR_INVALID);
        }

        // 业务校验：月份必须在1-12之间
        if (month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.LEDGER_MONTH_INVALID);
        }

        // 业务校验：账套必须存在
        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }

        log.info("查询总账，账套ID: {}, 年度: {}, 月份: {}", accountSetId, year, month);

        // 查询科目余额
        LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
        balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getYear, year)
                .eq(AccountBalance::getMonth, month);
        List<AccountBalance> balances = accountBalanceMapper.selectList(balanceWrapper);

        // 查询科目信息
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId);
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);
        Map<Long, Subject> subjectMap = subjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));

        // 构建总账VO
        List<GeneralLedgerVO> result = new ArrayList<>();
        for (AccountBalance balance : balances) {
            Subject subject = subjectMap.get(balance.getSubjectId());
            if (subject == null) {
                continue;
            }

            GeneralLedgerVO vo = new GeneralLedgerVO();
            vo.setSubjectCode(subject.getCode());
            vo.setSubjectName(subject.getName());
            vo.setBeginDebit(balance.getBeginDebit() != null ? balance.getBeginDebit() : BigDecimal.ZERO);
            vo.setBeginCredit(balance.getBeginCredit() != null ? balance.getBeginCredit() : BigDecimal.ZERO);
            vo.setPeriodDebit(balance.getPeriodDebit() != null ? balance.getPeriodDebit() : BigDecimal.ZERO);
            vo.setPeriodCredit(balance.getPeriodCredit() != null ? balance.getPeriodCredit() : BigDecimal.ZERO);
            vo.setEndDebit(balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO);
            vo.setEndCredit(balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO);

            result.add(vo);
        }

        // 按科目编码排序
        result.sort(Comparator.comparing(GeneralLedgerVO::getSubjectCode));

        log.info("查询总账成功，共 {} 条记录", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectBalanceVO> subjectBalance(SubjectBalanceQueryRequest request) {
        Long accountSetId = request.getAccountSetId();
        Integer year = request.getYear();
        Integer startMonth = request.getStartMonth() != null ? request.getStartMonth() : 1;
        Integer endMonth = request.getEndMonth() != null ? request.getEndMonth() : 12;

        // 业务校验：账套ID不能为空
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }

        // 业务校验：年度不能为空
        if (year == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "年度不能为空");
        }

        // 业务校验：年度必须合理（1900-2099）
        if (year < 1900 || year > 2099) {
            throw new BusinessException(ErrorCode.LEDGER_YEAR_INVALID);
        }

        // 业务校验：开始月份必须在1-12之间
        if (startMonth < 1 || startMonth > 12) {
            throw new BusinessException(ErrorCode.LEDGER_MONTH_INVALID, "开始月份必须在1-12之间");
        }

        // 业务校验：结束月份必须在1-12之间
        if (endMonth < 1 || endMonth > 12) {
            throw new BusinessException(ErrorCode.LEDGER_MONTH_INVALID, "结束月份必须在1-12之间");
        }

        // 业务校验：开始月份不能大于结束月份
        if (startMonth > endMonth) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始月份不能大于结束月份");
        }

        // 业务校验：账套必须存在
        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }

        log.info("查询科目余额表，账套ID: {}, 年度: {}, 开始月份: {}, 结束月份: {}", 
                accountSetId, year, startMonth, endMonth);

        // 查询科目余额
        LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
        balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getYear, year)
                .ge(AccountBalance::getMonth, startMonth)
                .le(AccountBalance::getMonth, endMonth);
        List<AccountBalance> balances = accountBalanceMapper.selectList(balanceWrapper);

        // 查询科目信息
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId);
        if (request.getLevel() != null) {
            subjectWrapper.eq(Subject::getLevel, request.getLevel());
        }
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);
        Map<Long, Subject> subjectMap = subjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));

        // 按科目汇总余额
        Map<Long, AccountBalance> summaryMap = new LinkedHashMap<>();
        for (AccountBalance balance : balances) {
            Long subjectId = balance.getSubjectId();
            if (!subjectMap.containsKey(subjectId)) {
                continue;
            }
            AccountBalance existing = summaryMap.get(subjectId);
            if (existing == null) {
                AccountBalance copy = new AccountBalance();
                copy.setSubjectId(subjectId);
                copy.setBeginDebit(balance.getBeginDebit() != null ? balance.getBeginDebit() : BigDecimal.ZERO);
                copy.setBeginCredit(balance.getBeginCredit() != null ? balance.getBeginCredit() : BigDecimal.ZERO);
                copy.setPeriodDebit(balance.getPeriodDebit() != null ? balance.getPeriodDebit() : BigDecimal.ZERO);
                copy.setPeriodCredit(balance.getPeriodCredit() != null ? balance.getPeriodCredit() : BigDecimal.ZERO);
                copy.setEndDebit(balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO);
                copy.setEndCredit(balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO);
                copy.setYearDebit(balance.getYearDebit() != null ? balance.getYearDebit() : BigDecimal.ZERO);
                copy.setYearCredit(balance.getYearCredit() != null ? balance.getYearCredit() : BigDecimal.ZERO);
                summaryMap.put(subjectId, copy);
            } else {
                existing.setPeriodDebit(existing.getPeriodDebit().add(balance.getPeriodDebit() != null ? balance.getPeriodDebit() : BigDecimal.ZERO));
                existing.setPeriodCredit(existing.getPeriodCredit().add(balance.getPeriodCredit() != null ? balance.getPeriodCredit() : BigDecimal.ZERO));
                existing.setYearDebit(existing.getYearDebit().add(balance.getYearDebit() != null ? balance.getYearDebit() : BigDecimal.ZERO));
                existing.setYearCredit(existing.getYearCredit().add(balance.getYearCredit() != null ? balance.getYearCredit() : BigDecimal.ZERO));
                // 使用最后一期的期末余额
                existing.setEndDebit(balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO);
                existing.setEndCredit(balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO);
            }
        }

        // 构建科目余额表VO
        List<SubjectBalanceVO> result = new ArrayList<>();
        for (Map.Entry<Long, AccountBalance> entry : summaryMap.entrySet()) {
            Subject subject = subjectMap.get(entry.getKey());
            AccountBalance balance = entry.getValue();

            SubjectBalanceVO vo = new SubjectBalanceVO();
            vo.setSubjectCode(subject.getCode());
            vo.setSubjectName(subject.getName());
            vo.setLevel(subject.getLevel());
            vo.setBeginDebit(balance.getBeginDebit());
            vo.setBeginCredit(balance.getBeginCredit());
            vo.setPeriodDebit(balance.getPeriodDebit());
            vo.setPeriodCredit(balance.getPeriodCredit());
            vo.setEndDebit(balance.getEndDebit());
            vo.setEndCredit(balance.getEndCredit());
            vo.setYearDebit(balance.getYearDebit());
            vo.setYearCredit(balance.getYearCredit());

            result.add(vo);
        }

        // 按科目编码排序
        result.sort(Comparator.comparing(SubjectBalanceVO::getSubjectCode));

        log.info("查询科目余额表成功，共 {} 条记录", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CashJournalVO> cashJournal(LedgerQueryRequest request) {
        return buildJournal(request, "cash");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CashJournalVO> bankJournal(LedgerQueryRequest request) {
        return buildJournal(request, "bank");
    }

    private PageResult<CashJournalVO> buildJournal(LedgerQueryRequest request, String journalType) {
        Long accountSetId = request.getAccountSetId();
        Integer year = request.getYear();
        Integer month = request.getMonth();

        // 业务校验：账套ID不能为空
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }

        // 业务校验：年度不能为空
        if (year == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "年度不能为空");
        }

        // 业务校验：年度必须合理（1900-2099）
        if (year < 1900 || year > 2099) {
            throw new BusinessException(ErrorCode.LEDGER_YEAR_INVALID);
        }

        // 业务校验：月份必须在1-12之间
        if (month != null && (month < 1 || month > 12)) {
            throw new BusinessException(ErrorCode.LEDGER_MONTH_INVALID);
        }

        // 业务校验：开始日期不能大于结束日期
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new BusinessException(ErrorCode.LEDGER_DATE_RANGE_INVALID);
            }
        }

        // 业务校验：账套必须存在
        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }

        log.info("查询{}日记账，账套ID: {}, 年度: {}, 月份: {}", 
                "cash".equals(journalType) ? "现金" : "银行", accountSetId, year, month);

        // 查询现金/银行科目
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId);
        if ("cash".equals(journalType)) {
            subjectWrapper.eq(Subject::getIsCash, 1);
        } else {
            subjectWrapper.eq(Subject::getIsBank, 1);
        }
        List<Subject> journalSubjects = subjectMapper.selectList(subjectWrapper);

        if (journalSubjects.isEmpty()) {
            log.info("未找到{}科目", "cash".equals(journalType) ? "现金" : "银行");
            return PageResult.of(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize());
        }

        List<Long> subjectIds = journalSubjects.stream().map(Subject::getId).collect(Collectors.toList());
        Map<Long, Subject> subjectMap = journalSubjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));

        // 查询已过账的凭证
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getStatus, 2)
                .eq(Voucher::getYear, year);
        if (month != null) {
            voucherWrapper.eq(Voucher::getMonth, month);
        }
        if (request.getStartDate() != null) {
            voucherWrapper.ge(Voucher::getVoucherDate, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            voucherWrapper.le(Voucher::getVoucherDate, request.getEndDate());
        }
        voucherWrapper.orderByAsc(Voucher::getVoucherDate, Voucher::getVoucherNo);
        List<Voucher> vouchers = voucherMapper.selectList(voucherWrapper);

        if (vouchers.isEmpty()) {
            log.info("未找到已过账的凭证");
            return PageResult.of(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize());
        }

        List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());

        // 查询凭证明细
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                .in(VoucherDetail::getSubjectId, subjectIds);
        detailWrapper.orderByAsc(VoucherDetail::getSortOrder);
        List<VoucherDetail> details = voucherDetailMapper.selectList(detailWrapper);

        // 构建凭证ID到凭证的映射
        Map<Long, Voucher> voucherMap = vouchers.stream()
                .collect(Collectors.toMap(Voucher::getId, v -> v));

        // 获取期初余额
        BigDecimal beginBalance = BigDecimal.ZERO;
        for (Subject subject : journalSubjects) {
            LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
            balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                    .eq(AccountBalance::getSubjectId, subject.getId())
                    .eq(AccountBalance::getYear, year);
            if (month != null) {
                balanceWrapper.eq(AccountBalance::getMonth, month);
            } else {
                balanceWrapper.eq(AccountBalance::getMonth, 1);
            }
            AccountBalance accountBalance = accountBalanceMapper.selectOne(balanceWrapper);
            if (accountBalance != null) {
                int balanceDirection = subject.getBalanceDirection() != null ? subject.getBalanceDirection() : 1;
                if (balanceDirection == 1) {
                    beginBalance = beginBalance.add(
                            (accountBalance.getBeginDebit() != null ? accountBalance.getBeginDebit() : BigDecimal.ZERO)
                                    .subtract(accountBalance.getBeginCredit() != null ? accountBalance.getBeginCredit() : BigDecimal.ZERO));
                } else {
                    beginBalance = beginBalance.add(
                            (accountBalance.getBeginCredit() != null ? accountBalance.getBeginCredit() : BigDecimal.ZERO)
                                    .subtract(accountBalance.getBeginDebit() != null ? accountBalance.getBeginDebit() : BigDecimal.ZERO));
                }
            }
        }

        // 构建日记账VO列表
        List<CashJournalVO> allRows = new ArrayList<>();
        BigDecimal runningBalance = beginBalance;

        for (VoucherDetail detail : details) {
            Voucher voucher = voucherMap.get(detail.getVoucherId());
            if (voucher == null) {
                continue;
            }

            Subject subject = subjectMap.get(detail.getSubjectId());
            int balanceDirection = (subject != null && subject.getBalanceDirection() != null)
                    ? subject.getBalanceDirection() : 1;

            CashJournalVO vo = new CashJournalVO();
            vo.setVoucherDate(voucher.getVoucherDate());
            vo.setVoucherNo(String.valueOf(voucher.getVoucherNo()));
            vo.setSummary(detail.getSummary());

            BigDecimal debit = detail.getDebit() != null ? detail.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = detail.getCredit() != null ? detail.getCredit() : BigDecimal.ZERO;

            if (balanceDirection == 1) {
                // 借方科目：借方为收入，贷方为支出
                vo.setIncome(debit);
                vo.setExpense(credit);
                runningBalance = runningBalance.add(debit).subtract(credit);
            } else {
                // 贷方科目：贷方为收入，借方为支出
                vo.setIncome(credit);
                vo.setExpense(debit);
                runningBalance = runningBalance.add(credit).subtract(debit);
            }
            vo.setBalance(runningBalance);

            allRows.add(vo);
        }

        // 手动分页
        int total = allRows.size();
        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<CashJournalVO> pageList;
        if (fromIndex >= total) {
            pageList = Collections.emptyList();
        } else {
            pageList = allRows.subList(fromIndex, toIndex);
        }

        log.info("查询{}日记账成功，共 {} 条记录", 
                "cash".equals(journalType) ? "现金" : "银行", total);
        return PageResult.of(pageList, (long) total, pageNum, pageSize);
    }
}
