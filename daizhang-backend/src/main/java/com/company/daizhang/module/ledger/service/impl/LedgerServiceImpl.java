package com.company.daizhang.module.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.accountset.mapper.AccountSetMapper;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.bank.entity.BankAccount;
import com.company.daizhang.module.bank.entity.BankTransaction;
import com.company.daizhang.module.bank.mapper.BankAccountMapper;
import com.company.daizhang.module.bank.mapper.BankTransactionMapper;
import com.company.daizhang.module.ledger.dto.LedgerQueryRequest;
import com.company.daizhang.module.ledger.dto.SubjectBalanceQueryRequest;
import com.company.daizhang.module.ledger.entity.MultiColumnConfig;
import com.company.daizhang.module.ledger.mapper.MultiColumnConfigMapper;
import com.company.daizhang.module.ledger.service.LedgerService;
import com.company.daizhang.module.ledger.vo.AccountCheckVO;
import com.company.daizhang.module.ledger.vo.AgingAnalysisVO;
import com.company.daizhang.module.ledger.vo.AuxiliaryBalanceVO;
import com.company.daizhang.module.ledger.vo.AuxiliaryDetailLedgerDetailVO;
import com.company.daizhang.module.ledger.vo.AuxiliaryDetailLedgerVO;
import com.company.daizhang.module.ledger.vo.CashJournalVO;
import com.company.daizhang.module.ledger.vo.DetailLedgerVO;
import com.company.daizhang.module.ledger.vo.GeneralLedgerVO;
import com.company.daizhang.module.ledger.vo.MultiColumnLedgerDetailVO;
import com.company.daizhang.module.ledger.vo.MultiColumnLedgerVO;
import com.company.daizhang.module.ledger.vo.MultiColumnTotalVO;
import com.company.daizhang.module.ledger.vo.QuantityAmountLedgerDetailVO;
import com.company.daizhang.module.ledger.vo.QuantityAmountLedgerVO;
import com.company.daizhang.module.ledger.vo.ReconciliationDetailVO;
import com.company.daizhang.module.ledger.vo.ReconciliationVO;
import com.company.daizhang.module.ledger.vo.SubjectBalanceVO;
import com.company.daizhang.module.subject.entity.AuxiliaryCategory;
import com.company.daizhang.module.subject.entity.AuxiliaryItem;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.AuxiliaryCategoryMapper;
import com.company.daizhang.module.subject.mapper.AuxiliaryItemMapper;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.entity.VoucherDetail;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    private final MultiColumnConfigMapper multiColumnConfigMapper;
    private final AuxiliaryItemMapper auxiliaryItemMapper;
    private final AuxiliaryCategoryMapper auxiliaryCategoryMapper;
    private final AccountSetAccessService accountSetAccessService;
    private final BankAccountMapper bankAccountMapper;
    private final BankTransactionMapper bankTransactionMapper;

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

        // IDOR越权校验：账套读权限
        accountSetAccessService.checkAccess(accountSetId);

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
        // Java层按凭证维度重新排序,跨凭证查询时sortOrder会重复,需先按凭证顺序排序
        sortDetailsByVoucherOrder(details, vouchers);

        // 构建凭证ID到凭证的映射
        Map<Long, Voucher> voucherMap = vouchers.stream()
                .collect(Collectors.toMap(Voucher::getId, v -> v));

        // 构建明细账VO列表
        int balanceDirection = (subject.getBalanceDirection() != null)
                ? subject.getBalanceDirection() : 1;

        // 获取期初余额（month=null表示查全年,取1月期初作为年初余额）
        BigDecimal beginBalance = BigDecimal.ZERO;
        Integer balanceMonth = (month != null) ? month : 1;
        {
            LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
            balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                    .eq(AccountBalance::getSubjectId, subjectId)
                    .eq(AccountBalance::getYear, year)
                    .eq(AccountBalance::getMonth, balanceMonth);
            AccountBalance accountBalance = accountBalanceMapper.selectOne(balanceWrapper);
            if (accountBalance != null) {
                if (balanceDirection == 1) {
                    BigDecimal d = accountBalance.getBeginDebit() != null ? accountBalance.getBeginDebit() : BigDecimal.ZERO;
                    BigDecimal c = accountBalance.getBeginCredit() != null ? accountBalance.getBeginCredit() : BigDecimal.ZERO;
                    beginBalance = d.subtract(c);
                } else {
                    BigDecimal d = accountBalance.getBeginDebit() != null ? accountBalance.getBeginDebit() : BigDecimal.ZERO;
                    BigDecimal c = accountBalance.getBeginCredit() != null ? accountBalance.getBeginCredit() : BigDecimal.ZERO;
                    beginBalance = c.subtract(d);
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
            int cmp = runningBalance.compareTo(BigDecimal.ZERO);
            vo.setDirection(cmp > 0 ? "借" : (cmp < 0 ? "贷" : "平"));
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
        // month 为空表示查询全年汇总,不再静默回退为 1 月(否则前端"年度总账"场景会误显示 1 月单月数据)
        Integer month = request.getMonth();

        // 业务校验：账套ID不能为空
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }

        // IDOR越权校验：账套读权限
        accountSetAccessService.checkAccess(accountSetId);

        // 业务校验：年度不能为空
        if (year == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "年度不能为空");
        }

        // 业务校验：年度必须合理（1900-2099）
        if (year < 1900 || year > 2099) {
            throw new BusinessException(ErrorCode.LEDGER_YEAR_INVALID);
        }

        // 业务校验：月份必须在1-12之间(仅当指定月份时校验;为空表示全年汇总)
        if (month != null && (month < 1 || month > 12)) {
            throw new BusinessException(ErrorCode.LEDGER_MONTH_INVALID);
        }

        // 业务校验：账套必须存在
        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }

        log.info("查询总账，账套ID: {}, 年度: {}, 月份: {}", accountSetId, year, month == null ? "全年" : month);

        // 查询科目余额:month 为空时不按月份过滤,取全年各月余额后聚合
        LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
        balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getYear, year);
        if (month != null) {
            balanceWrapper.eq(AccountBalance::getMonth, month);
        }
        List<AccountBalance> balances = accountBalanceMapper.selectList(balanceWrapper);

        // 查询科目信息
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId);
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);
        Map<Long, Subject> subjectMap = subjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));

        // 构建总账VO:统一按 subjectId 分组聚合。
        // - 单月查询:每个科目仅1条余额,聚合结果即该月期初/发生额/期末。
        // - 全年查询(month=null):每个科目多条余额(按月),期初取1月期初(年初),
        //   发生额取全年累加,期末取最大月份期末(年末)。
        Map<Long, List<AccountBalance>> balancesBySubject = balances.stream()
                .collect(Collectors.groupingBy(AccountBalance::getSubjectId));

        List<GeneralLedgerVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<AccountBalance>> entry : balancesBySubject.entrySet()) {
            Subject subject = subjectMap.get(entry.getKey());
            if (subject == null) {
                continue;
            }

            List<AccountBalance> subjectBalances = entry.getValue();
            // 按月份排序,确保首条为最早月(期初)、末条为最晚月(期末)
            subjectBalances.sort(Comparator.comparingInt(b -> b.getMonth() != null ? b.getMonth() : 0));
            AccountBalance first = subjectBalances.get(0);
            AccountBalance last = subjectBalances.get(subjectBalances.size() - 1);

            GeneralLedgerVO vo = new GeneralLedgerVO();
            vo.setSubjectCode(subject.getCode());
            vo.setSubjectName(subject.getName());
            // 期初:取最早月份的期初余额(单月即该月期初;全年即1月期初=年初)
            vo.setBeginDebit(first.getBeginDebit() != null ? first.getBeginDebit() : BigDecimal.ZERO);
            vo.setBeginCredit(first.getBeginCredit() != null ? first.getBeginCredit() : BigDecimal.ZERO);
            // 本期发生额:单月即该月发生额;全年为各月发生额累加
            BigDecimal periodDebit = BigDecimal.ZERO;
            BigDecimal periodCredit = BigDecimal.ZERO;
            for (AccountBalance b : subjectBalances) {
                periodDebit = periodDebit.add(b.getPeriodDebit() != null ? b.getPeriodDebit() : BigDecimal.ZERO);
                periodCredit = periodCredit.add(b.getPeriodCredit() != null ? b.getPeriodCredit() : BigDecimal.ZERO);
            }
            vo.setPeriodDebit(periodDebit);
            vo.setPeriodCredit(periodCredit);
            // 期末:取最晚月份的期末余额(单月即该月期末;全年即12月期末=年末)
            vo.setEndDebit(last.getEndDebit() != null ? last.getEndDebit() : BigDecimal.ZERO);
            vo.setEndCredit(last.getEndCredit() != null ? last.getEndCredit() : BigDecimal.ZERO);

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

        // IDOR越权校验：账套读权限
        accountSetAccessService.checkAccess(accountSetId);

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
                .le(AccountBalance::getMonth, endMonth)
                .orderByAsc(AccountBalance::getMonth);
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
                // yearDebit/yearCredit为本年累计值(截至期末的累计),各月存在包含关系,不能累加,取查询范围最后一期
                existing.setYearDebit(balance.getYearDebit() != null ? balance.getYearDebit() : BigDecimal.ZERO);
                existing.setYearCredit(balance.getYearCredit() != null ? balance.getYearCredit() : BigDecimal.ZERO);
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

        // IDOR越权校验：账套读权限
        accountSetAccessService.checkAccess(accountSetId);

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
        // Java层按凭证维度重新排序,跨凭证查询时sortOrder会重复,需先按凭证顺序排序
        sortDetailsByVoucherOrder(details, vouchers);

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

    @Override
    @Transactional(readOnly = true)
    public MultiColumnLedgerVO multiColumnLedger(Long accountSetId, Long subjectId, Integer year, Integer month) {
        // 业务校验
        validateLedgerParams(accountSetId, subjectId, year, month);
        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }
        Subject subject = subjectMapper.selectById(subjectId);
        if (subject == null || !accountSetId.equals(subject.getAccountSetId())) {
            throw new BusinessException(ErrorCode.LEDGER_SUBJECT_NOT_FOUND);
        }

        log.info("查询多栏账，账套ID: {}, 科目ID: {}, 年度: {}, 月份: {}", accountSetId, subjectId, year, month);

        // 查询多栏账配置
        LambdaQueryWrapper<MultiColumnConfig> configWrapper = new LambdaQueryWrapper<>();
        configWrapper.eq(MultiColumnConfig::getAccountSetId, accountSetId)
                .eq(MultiColumnConfig::getSubjectId, subjectId);
        MultiColumnConfig config = multiColumnConfigMapper.selectOne(configWrapper);

        List<String> columnItems = new ArrayList<>();
        if (config != null && config.getColumnItems() != null && !config.getColumnItems().isEmpty()) {
            columnItems = Arrays.asList(config.getColumnItems().split(","));
        }

        // 获取期初余额（month=null表示查全年,取1月期初作为年初余额）
        BigDecimal beginDebit = BigDecimal.ZERO;
        BigDecimal beginCredit = BigDecimal.ZERO;
        Integer balanceMonth = (month != null) ? month : 1;
        {
            LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
            balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                    .eq(AccountBalance::getSubjectId, subjectId)
                    .eq(AccountBalance::getYear, year)
                    .eq(AccountBalance::getMonth, balanceMonth);
            AccountBalance accountBalance = accountBalanceMapper.selectOne(balanceWrapper);
            if (accountBalance != null) {
                beginDebit = accountBalance.getBeginDebit() != null ? accountBalance.getBeginDebit() : BigDecimal.ZERO;
                beginCredit = accountBalance.getBeginCredit() != null ? accountBalance.getBeginCredit() : BigDecimal.ZERO;
            }
        }

        // 查询已过账的凭证
        List<VoucherDetail> details = queryPostedVoucherDetails(accountSetId, subjectId, year, month);
        Map<Long, Voucher> voucherMap = queryVoucherMap(accountSetId, year, month);

        int balanceDirection = (subject.getBalanceDirection() != null) ? subject.getBalanceDirection() : 1;
        BigDecimal runningBalance;
        if (balanceDirection == 1) {
            runningBalance = beginDebit.subtract(beginCredit);
        } else {
            runningBalance = beginCredit.subtract(beginDebit);
        }

        List<MultiColumnLedgerDetailVO> detailList = new ArrayList<>();
        // 各栏目合计
        Map<String, BigDecimal[]> columnTotals = new LinkedHashMap<>();
        for (String column : columnItems) {
            columnTotals.put(column, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (VoucherDetail detail : details) {
            Voucher voucher = voucherMap.get(detail.getVoucherId());
            if (voucher == null) {
                continue;
            }

            MultiColumnLedgerDetailVO vo = new MultiColumnLedgerDetailVO();
            vo.setVoucherDate(voucher.getVoucherDate());
            vo.setVoucherNo(String.valueOf(voucher.getVoucherNo()));
            vo.setSummary(detail.getSummary());
            BigDecimal debit = detail.getDebit() != null ? detail.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = detail.getCredit() != null ? detail.getCredit() : BigDecimal.ZERO;
            vo.setDebit(debit);
            vo.setCredit(credit);

            // 根据摘要匹配栏目
            String matchedColumn = matchColumn(detail.getSummary(), columnItems);
            vo.setColumnName(matchedColumn);

            // 计算余额
            if (balanceDirection == 1) {
                runningBalance = runningBalance.add(debit).subtract(credit);
            } else {
                runningBalance = runningBalance.add(credit).subtract(debit);
            }
            vo.setBalance(runningBalance);

            detailList.add(vo);

            // 累计栏目金额
            if (matchedColumn != null && columnTotals.containsKey(matchedColumn)) {
                BigDecimal[] totals = columnTotals.get(matchedColumn);
                totals[0] = totals[0].add(debit);
                totals[1] = totals[1].add(credit);
            }
        }

        // 构建栏目合计
        List<MultiColumnTotalVO> totals = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> entry : columnTotals.entrySet()) {
            MultiColumnTotalVO totalVO = new MultiColumnTotalVO();
            totalVO.setColumnName(entry.getKey());
            totalVO.setTotalDebit(entry.getValue()[0]);
            totalVO.setTotalCredit(entry.getValue()[1]);
            totals.add(totalVO);
        }

        MultiColumnLedgerVO result = new MultiColumnLedgerVO();
        result.setSubjectCode(subject.getCode());
        result.setSubjectName(subject.getName());
        result.setBeginDebit(beginDebit);
        result.setBeginCredit(beginCredit);
        result.setDetails(detailList);
        result.setTotals(totals);

        log.info("查询多栏账成功，共 {} 条记录", detailList.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public QuantityAmountLedgerVO quantityAmountLedger(Long accountSetId, Long subjectId, Integer year, Integer month) {
        // 业务校验
        validateLedgerParams(accountSetId, subjectId, year, month);
        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }
        Subject subject = subjectMapper.selectById(subjectId);
        if (subject == null || !accountSetId.equals(subject.getAccountSetId())) {
            throw new BusinessException(ErrorCode.LEDGER_SUBJECT_NOT_FOUND);
        }

        log.info("查询数量金额账，账套ID: {}, 科目ID: {}, 年度: {}, 月份: {}", accountSetId, subjectId, year, month);

        // 计算期初数量和金额（从年初到查询月份之前的累计）
        BigDecimal beginQuantity = BigDecimal.ZERO;
        BigDecimal beginAmount = BigDecimal.ZERO;
        int balanceDirection = (subject.getBalanceDirection() != null) ? subject.getBalanceDirection() : 1;

        // 查询年初至查询月份之前的凭证明细
        LambdaQueryWrapper<Voucher> beforeVoucherWrapper = new LambdaQueryWrapper<>();
        beforeVoucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getStatus, 2)
                .eq(Voucher::getYear, year);
        if (month != null) {
            beforeVoucherWrapper.lt(Voucher::getMonth, month);
        }
        beforeVoucherWrapper.orderByAsc(Voucher::getVoucherDate, Voucher::getVoucherNo);
        List<Voucher> beforeVouchers = voucherMapper.selectList(beforeVoucherWrapper);

        if (!beforeVouchers.isEmpty()) {
            List<Long> beforeVoucherIds = beforeVouchers.stream().map(Voucher::getId).collect(Collectors.toList());
            LambdaQueryWrapper<VoucherDetail> beforeDetailWrapper = new LambdaQueryWrapper<>();
            beforeDetailWrapper.in(VoucherDetail::getVoucherId, beforeVoucherIds)
                    .eq(VoucherDetail::getSubjectId, subjectId);
            List<VoucherDetail> beforeDetails = voucherDetailMapper.selectList(beforeDetailWrapper);
            for (VoucherDetail detail : beforeDetails) {
                BigDecimal qty = detail.getQuantity() != null ? detail.getQuantity() : BigDecimal.ZERO;
                BigDecimal amt = detail.getDebit() != null && detail.getDebit().compareTo(BigDecimal.ZERO) > 0
                        ? detail.getDebit() : (detail.getCredit() != null ? detail.getCredit().negate() : BigDecimal.ZERO);
                if (balanceDirection == 1) {
                    beginQuantity = beginQuantity.add(qty);
                    beginAmount = beginAmount.add(amt);
                } else {
                    beginQuantity = beginQuantity.subtract(qty);
                    beginAmount = beginAmount.subtract(amt);
                }
            }
        }

        // 查询本期凭证明细
        List<VoucherDetail> details = queryPostedVoucherDetails(accountSetId, subjectId, year, month);
        Map<Long, Voucher> voucherMap = queryVoucherMap(accountSetId, year, month);

        BigDecimal runningQuantity = beginQuantity;
        BigDecimal runningAmount = beginAmount;

        List<QuantityAmountLedgerDetailVO> detailList = new ArrayList<>();
        for (VoucherDetail detail : details) {
            Voucher voucher = voucherMap.get(detail.getVoucherId());
            if (voucher == null) {
                continue;
            }

            QuantityAmountLedgerDetailVO vo = new QuantityAmountLedgerDetailVO();
            vo.setVoucherDate(voucher.getVoucherDate());
            vo.setVoucherNo(String.valueOf(voucher.getVoucherNo()));
            vo.setSummary(detail.getSummary());

            BigDecimal quantity = detail.getQuantity() != null ? detail.getQuantity() : BigDecimal.ZERO;
            BigDecimal unitPrice = detail.getUnitPrice() != null ? detail.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal debit = detail.getDebit() != null ? detail.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = detail.getCredit() != null ? detail.getCredit() : BigDecimal.ZERO;

            vo.setQuantity(quantity);
            vo.setUnitPrice(unitPrice);

            BigDecimal amount;
            String direction;
            if (debit.compareTo(BigDecimal.ZERO) > 0) {
                amount = debit;
                direction = "借";
            } else {
                amount = credit;
                direction = "贷";
            }
            vo.setAmount(amount);
            vo.setDirection(direction);

            // 计算结余
            if (balanceDirection == 1) {
                runningQuantity = runningQuantity.add(quantity);
                runningAmount = runningAmount.add(amount);
            } else {
                runningQuantity = runningQuantity.subtract(quantity);
                runningAmount = runningAmount.subtract(amount);
            }
            vo.setBalanceQuantity(runningQuantity);
            vo.setBalanceAmount(runningAmount);

            detailList.add(vo);
        }

        QuantityAmountLedgerVO result = new QuantityAmountLedgerVO();
        result.setSubjectCode(subject.getCode());
        result.setSubjectName(subject.getName());
        result.setUnit(null);
        result.setBeginQuantity(beginQuantity);
        result.setBeginAmount(beginAmount);
        result.setDetails(detailList);
        result.setEndQuantity(runningQuantity);
        result.setEndAmount(runningAmount);

        log.info("查询数量金额账成功，共 {} 条记录", detailList.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public AuxiliaryDetailLedgerVO auxiliaryDetailLedger(Long accountSetId, Long subjectId, Long auxiliaryId, Integer year, Integer month) {
        // 业务校验
        validateLedgerParams(accountSetId, subjectId, year, month);
        if (auxiliaryId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "辅助核算项目ID不能为空");
        }

        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }
        Subject subject = subjectMapper.selectById(subjectId);
        if (subject == null || !accountSetId.equals(subject.getAccountSetId())) {
            throw new BusinessException(ErrorCode.LEDGER_SUBJECT_NOT_FOUND);
        }

        // 查询辅助核算项目信息
        AuxiliaryItem auxiliaryItem = auxiliaryItemMapper.selectById(auxiliaryId);
        if (auxiliaryItem == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "辅助核算项目不存在");
        }

        // 查询辅助核算类别信息
        String categoryName = "";
        if (auxiliaryItem.getCategoryId() != null) {
            AuxiliaryCategory category = auxiliaryCategoryMapper.selectById(auxiliaryItem.getCategoryId());
            if (category != null) {
                categoryName = category.getCategoryName();
            }
        }

        log.info("查询辅助核算明细账，账套ID: {}, 科目ID: {}, 辅助核算ID: {}, 年度: {}, 月份: {}",
                accountSetId, subjectId, auxiliaryId, year, month);

        int balanceDirection = (subject.getBalanceDirection() != null) ? subject.getBalanceDirection() : 1;

        // 计算期初余额（年初至查询月份之前的累计）
        BigDecimal beginNet = BigDecimal.ZERO;
        if (month != null) {
            LambdaQueryWrapper<Voucher> beforeVoucherWrapper = new LambdaQueryWrapper<>();
            beforeVoucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                    .eq(Voucher::getStatus, 2)
                    .eq(Voucher::getYear, year)
                    .lt(Voucher::getMonth, month);
            beforeVoucherWrapper.orderByAsc(Voucher::getVoucherDate, Voucher::getVoucherNo);
            List<Voucher> beforeVouchers = voucherMapper.selectList(beforeVoucherWrapper);

            if (!beforeVouchers.isEmpty()) {
                List<Long> beforeVoucherIds = beforeVouchers.stream().map(Voucher::getId).collect(Collectors.toList());
                LambdaQueryWrapper<VoucherDetail> beforeDetailWrapper = new LambdaQueryWrapper<>();
                beforeDetailWrapper.in(VoucherDetail::getVoucherId, beforeVoucherIds)
                        .eq(VoucherDetail::getSubjectId, subjectId)
                        .eq(VoucherDetail::getAuxiliaryId, auxiliaryId);
                List<VoucherDetail> beforeDetails = voucherDetailMapper.selectList(beforeDetailWrapper);

                for (VoucherDetail detail : beforeDetails) {
                    BigDecimal debit = detail.getDebit() != null ? detail.getDebit() : BigDecimal.ZERO;
                    BigDecimal credit = detail.getCredit() != null ? detail.getCredit() : BigDecimal.ZERO;
                    beginNet = beginNet.add(debit).subtract(credit);
                }
            }
        }

        BigDecimal beginDebit = beginNet.compareTo(BigDecimal.ZERO) >= 0 ? beginNet : BigDecimal.ZERO;
        BigDecimal beginCredit = beginNet.compareTo(BigDecimal.ZERO) < 0 ? beginNet.negate() : BigDecimal.ZERO;

        // 查询本期已过账的凭证
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getStatus, 2)
                .eq(Voucher::getYear, year);
        if (month != null) {
            voucherWrapper.eq(Voucher::getMonth, month);
        }
        voucherWrapper.orderByAsc(Voucher::getVoucherDate, Voucher::getVoucherNo);
        List<Voucher> vouchers = voucherMapper.selectList(voucherWrapper);

        Map<Long, Voucher> voucherMap = vouchers.stream()
                .collect(Collectors.toMap(Voucher::getId, v -> v));

        List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());

        List<VoucherDetail> details = Collections.emptyList();
        if (!voucherIds.isEmpty()) {
            LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                    .eq(VoucherDetail::getSubjectId, subjectId)
                    .eq(VoucherDetail::getAuxiliaryId, auxiliaryId);
            detailWrapper.orderByAsc(VoucherDetail::getSortOrder);
            details = voucherDetailMapper.selectList(detailWrapper);
            // Java层按凭证维度重新排序,跨凭证查询时sortOrder会重复,需先按凭证顺序排序
            sortDetailsByVoucherOrder(details, vouchers);
        }

        // 构建明细列表并计算余额
        List<AuxiliaryDetailLedgerDetailVO> detailList = new ArrayList<>();
        BigDecimal runningBalance;
        if (balanceDirection == 1) {
            runningBalance = beginDebit.subtract(beginCredit);
        } else {
            runningBalance = beginCredit.subtract(beginDebit);
        }

        for (VoucherDetail detail : details) {
            Voucher voucher = voucherMap.get(detail.getVoucherId());
            if (voucher == null) {
                continue;
            }

            AuxiliaryDetailLedgerDetailVO vo = new AuxiliaryDetailLedgerDetailVO();
            vo.setVoucherDate(voucher.getVoucherDate());
            vo.setVoucherNo(String.valueOf(voucher.getVoucherNo()));
            vo.setSummary(detail.getSummary());
            BigDecimal debit = detail.getDebit() != null ? detail.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = detail.getCredit() != null ? detail.getCredit() : BigDecimal.ZERO;
            vo.setDebit(debit);
            vo.setCredit(credit);

            if (balanceDirection == 1) {
                runningBalance = runningBalance.add(debit).subtract(credit);
            } else {
                runningBalance = runningBalance.add(credit).subtract(debit);
            }
            int cmp = runningBalance.compareTo(BigDecimal.ZERO);
            vo.setDirection(cmp > 0 ? "借" : (cmp < 0 ? "贷" : "平"));
            vo.setBalance(runningBalance.abs());

            detailList.add(vo);
        }

        // 计算期末余额
        BigDecimal endNet = (balanceDirection == 1) ? runningBalance : runningBalance.negate();
        BigDecimal endDebit = endNet.compareTo(BigDecimal.ZERO) >= 0 ? endNet : BigDecimal.ZERO;
        BigDecimal endCredit = endNet.compareTo(BigDecimal.ZERO) < 0 ? endNet.negate() : BigDecimal.ZERO;

        AuxiliaryDetailLedgerVO result = new AuxiliaryDetailLedgerVO();
        result.setSubjectCode(subject.getCode());
        result.setSubjectName(subject.getName());
        result.setAuxiliaryCategoryName(categoryName);
        result.setAuxiliaryItemName(auxiliaryItem.getItemName());
        result.setBeginDebit(beginDebit);
        result.setBeginCredit(beginCredit);
        result.setDetails(detailList);
        result.setEndDebit(endDebit);
        result.setEndCredit(endCredit);

        log.info("查询辅助核算明细账成功，共 {} 条记录", detailList.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportDetailLedger(Long accountSetId, Long subjectId, Integer year, Integer month) {
        validateLedgerParams(accountSetId, subjectId, year, month);
        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }
        Subject subject = subjectMapper.selectById(subjectId);
        if (subject == null || !accountSetId.equals(subject.getAccountSetId())) {
            throw new BusinessException(ErrorCode.LEDGER_SUBJECT_NOT_FOUND);
        }

        log.info("导出明细账，账套ID: {}, 科目ID: {}, 年度: {}, 月份: {}", accountSetId, subjectId, year, month);

        // 查询明细账数据
        List<VoucherDetail> details = queryPostedVoucherDetails(accountSetId, subjectId, year, month);
        Map<Long, Voucher> voucherMap = queryVoucherMap(accountSetId, year, month);

        int balanceDirection = (subject.getBalanceDirection() != null) ? subject.getBalanceDirection() : 1;
        BigDecimal runningBalance = getBeginBalance(accountSetId, subjectId, year, month, balanceDirection);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("明细账");

            CellStyle headerStyle = createHeaderStyle(workbook);

            // 标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("明细账 " + subject.getCode() + " " + subject.getName()
                    + " " + year + "年" + (month != null ? month + "月" : ""));
            titleCell.setCellStyle(headerStyle);

            // 表头
            Row headerRow = sheet.createRow(2);
            String[] headers = {"日期", "凭证号", "摘要", "借方", "贷方", "方向", "余额"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行
            int rowNum = 3;
            for (VoucherDetail detail : details) {
                Voucher voucher = voucherMap.get(detail.getVoucherId());
                if (voucher == null) {
                    continue;
                }
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(voucher.getVoucherDate() != null ? voucher.getVoucherDate().toString() : "");
                row.createCell(1).setCellValue(String.valueOf(voucher.getVoucherNo()));
                row.createCell(2).setCellValue(detail.getSummary() != null ? detail.getSummary() : "");
                BigDecimal debit = detail.getDebit() != null ? detail.getDebit() : BigDecimal.ZERO;
                BigDecimal credit = detail.getCredit() != null ? detail.getCredit() : BigDecimal.ZERO;
                row.createCell(3).setCellValue(debit.doubleValue());
                row.createCell(4).setCellValue(credit.doubleValue());

                if (balanceDirection == 1) {
                    runningBalance = runningBalance.add(debit).subtract(credit);
                } else {
                    runningBalance = runningBalance.add(credit).subtract(debit);
                }
                int cmp = runningBalance.compareTo(BigDecimal.ZERO);
                row.createCell(5).setCellValue(cmp > 0 ? "借" : (cmp < 0 ? "贷" : "平"));
                row.createCell(6).setCellValue(runningBalance.abs().doubleValue());
            }

            // 列宽
            sheet.setColumnWidth(0, 12 * 256);
            sheet.setColumnWidth(1, 12 * 256);
            sheet.setColumnWidth(2, 30 * 256);
            for (int i = 3; i < 7; i++) {
                sheet.setColumnWidth(i, 15 * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("导出明细账失败", e);
            throw new RuntimeException("导出明细账失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportGeneralLedger(Long accountSetId, Integer year, Integer month) {
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }
        // IDOR越权校验：账套读权限
        accountSetAccessService.checkAccess(accountSetId);
        if (year == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "年度不能为空");
        }
        if (year < 1900 || year > 2099) {
            throw new BusinessException(ErrorCode.LEDGER_YEAR_INVALID);
        }
        Integer queryMonth = month != null ? month : 1;
        if (queryMonth < 1 || queryMonth > 12) {
            throw new BusinessException(ErrorCode.LEDGER_MONTH_INVALID);
        }
        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }

        log.info("导出总账，账套ID: {}, 年度: {}, 月份: {}", accountSetId, year, month);

        // 复用总账查询逻辑
        LedgerQueryRequest request = new LedgerQueryRequest();
        request.setAccountSetId(accountSetId);
        request.setYear(year);
        request.setMonth(month);
        List<GeneralLedgerVO> list = generalLedger(request);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("总账");

            CellStyle headerStyle = createHeaderStyle(workbook);

            // 标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("总账 " + year + "年" + (month != null ? month + "月" : ""));
            titleCell.setCellStyle(headerStyle);

            // 表头
            Row headerRow = sheet.createRow(2);
            String[] headers = {"科目编码", "科目名称", "期初借方", "期初贷方", "本期借方", "本期贷方", "期末借方", "期末贷方"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行
            int rowNum = 3;
            for (GeneralLedgerVO vo : list) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(vo.getSubjectCode() != null ? vo.getSubjectCode() : "");
                row.createCell(1).setCellValue(vo.getSubjectName() != null ? vo.getSubjectName() : "");
                row.createCell(2).setCellValue(vo.getBeginDebit() != null ? vo.getBeginDebit().doubleValue() : 0);
                row.createCell(3).setCellValue(vo.getBeginCredit() != null ? vo.getBeginCredit().doubleValue() : 0);
                row.createCell(4).setCellValue(vo.getPeriodDebit() != null ? vo.getPeriodDebit().doubleValue() : 0);
                row.createCell(5).setCellValue(vo.getPeriodCredit() != null ? vo.getPeriodCredit().doubleValue() : 0);
                row.createCell(6).setCellValue(vo.getEndDebit() != null ? vo.getEndDebit().doubleValue() : 0);
                row.createCell(7).setCellValue(vo.getEndCredit() != null ? vo.getEndCredit().doubleValue() : 0);
            }

            // 列宽
            sheet.setColumnWidth(0, 12 * 256);
            sheet.setColumnWidth(1, 20 * 256);
            for (int i = 2; i < 8; i++) {
                sheet.setColumnWidth(i, 15 * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("导出总账失败", e);
            throw new RuntimeException("导出总账失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportSubjectBalance(Long accountSetId, Integer year, Integer startMonth, Integer endMonth) {
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }
        // IDOR越权校验：账套读权限
        accountSetAccessService.checkAccess(accountSetId);
        if (year == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "年度不能为空");
        }
        if (year < 1900 || year > 2099) {
            throw new BusinessException(ErrorCode.LEDGER_YEAR_INVALID);
        }
        Integer start = startMonth != null ? startMonth : 1;
        Integer end = endMonth != null ? endMonth : 12;
        if (start < 1 || start > 12) {
            throw new BusinessException(ErrorCode.LEDGER_MONTH_INVALID, "开始月份必须在1-12之间");
        }
        if (end < 1 || end > 12) {
            throw new BusinessException(ErrorCode.LEDGER_MONTH_INVALID, "结束月份必须在1-12之间");
        }
        if (start > end) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始月份不能大于结束月份");
        }
        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }

        log.info("导出科目余额表，账套ID: {}, 年度: {}, 开始月份: {}, 结束月份: {}", accountSetId, year, start, end);

        // 复用科目余额表查询逻辑
        SubjectBalanceQueryRequest request = new SubjectBalanceQueryRequest();
        request.setAccountSetId(accountSetId);
        request.setYear(year);
        request.setStartMonth(start);
        request.setEndMonth(end);
        List<SubjectBalanceVO> list = subjectBalance(request);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("科目余额表");

            CellStyle headerStyle = createHeaderStyle(workbook);

            // 标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("科目余额表 " + year + "年" + start + "-" + end + "月");
            titleCell.setCellStyle(headerStyle);

            // 表头
            Row headerRow = sheet.createRow(2);
            String[] headers = {"科目编码", "科目名称", "期初借方", "期初贷方", "本期借方", "本期贷方", "期末借方", "期末贷方", "本年借方", "本年贷方"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行
            int rowNum = 3;
            for (SubjectBalanceVO vo : list) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(vo.getSubjectCode() != null ? vo.getSubjectCode() : "");
                row.createCell(1).setCellValue(vo.getSubjectName() != null ? vo.getSubjectName() : "");
                row.createCell(2).setCellValue(vo.getBeginDebit() != null ? vo.getBeginDebit().doubleValue() : 0);
                row.createCell(3).setCellValue(vo.getBeginCredit() != null ? vo.getBeginCredit().doubleValue() : 0);
                row.createCell(4).setCellValue(vo.getPeriodDebit() != null ? vo.getPeriodDebit().doubleValue() : 0);
                row.createCell(5).setCellValue(vo.getPeriodCredit() != null ? vo.getPeriodCredit().doubleValue() : 0);
                row.createCell(6).setCellValue(vo.getEndDebit() != null ? vo.getEndDebit().doubleValue() : 0);
                row.createCell(7).setCellValue(vo.getEndCredit() != null ? vo.getEndCredit().doubleValue() : 0);
                row.createCell(8).setCellValue(vo.getYearDebit() != null ? vo.getYearDebit().doubleValue() : 0);
                row.createCell(9).setCellValue(vo.getYearCredit() != null ? vo.getYearCredit().doubleValue() : 0);
            }

            // 列宽
            sheet.setColumnWidth(0, 12 * 256);
            sheet.setColumnWidth(1, 20 * 256);
            for (int i = 2; i < 10; i++) {
                sheet.setColumnWidth(i, 15 * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("导出科目余额表失败", e);
            throw new RuntimeException("导出科目余额表失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgingAnalysisVO> agingAnalysis(Long accountSetId, Integer year, Integer month, String subjectType) {
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }
        // IDOR越权校验：账套读权限
        accountSetAccessService.checkAccess(accountSetId);
        if (year == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "年度不能为空");
        }
        if (year < 1900 || year > 2099) {
            throw new BusinessException(ErrorCode.LEDGER_YEAR_INVALID);
        }
        if (month != null && (month < 1 || month > 12)) {
            throw new BusinessException(ErrorCode.LEDGER_MONTH_INVALID);
        }
        if (subjectType == null || (!"receivable".equals(subjectType) && !"payable".equals(subjectType))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "科目类型必须为receivable或payable");
        }

        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }

        // 应收账款: 1122, 应付账款: 2202（用likeRight匹配下级科目,如112201/220202等）
        String subjectCode = "receivable".equals(subjectType) ? "1122" : "2202";
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId)
                .likeRight(Subject::getCode, subjectCode);
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);
        if (subjects.isEmpty()) {
            throw new BusinessException(ErrorCode.LEDGER_SUBJECT_NOT_FOUND,
                    "未找到科目编码 " + subjectCode);
        }
        // 取编码最短的作为一级科目(用于展示名和余额方向)
        Subject subject = subjects.get(0);
        for (Subject s : subjects) {
            if (s.getCode().length() < subject.getCode().length()) {
                subject = s;
            }
        }
        List<Long> subjectIds = subjects.stream().map(Subject::getId).collect(Collectors.toList());

        log.info("查询账龄分析，账套ID: {}, 年度: {}, 月份: {}, 科目类型: {}", accountSetId, year, month, subjectType);

        // 查询已过账的凭证(年初至当月,累计历史余额;month=null查全年)
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getStatus, 2)
                .eq(Voucher::getYear, year);
        if (month != null) {
            voucherWrapper.le(Voucher::getMonth, month);
        }
        voucherWrapper.orderByAsc(Voucher::getVoucherDate, Voucher::getVoucherNo);
        List<Voucher> vouchers = voucherMapper.selectList(voucherWrapper);

        if (vouchers.isEmpty()) {
            log.info("未找到已过账的凭证");
            return Collections.emptyList();
        }

        Map<Long, Voucher> voucherMap = vouchers.stream()
                .collect(Collectors.toMap(Voucher::getId, v -> v));
        List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());

        // 查询该科目(含下级)下所有带辅助核算的凭证明细
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                .in(VoucherDetail::getSubjectId, subjectIds)
                .isNotNull(VoucherDetail::getAuxiliaryId);
        detailWrapper.orderByAsc(VoucherDetail::getSortOrder);
        List<VoucherDetail> details = voucherDetailMapper.selectList(detailWrapper);
        // Java层按凭证维度重新排序,跨凭证查询时sortOrder会重复,需先按凭证顺序排序
        sortDetailsByVoucherOrder(details, vouchers);

        // 按辅助核算项目分组
        Map<Long, List<VoucherDetail>> auxiliaryGroupMap = details.stream()
                .collect(Collectors.groupingBy(VoucherDetail::getAuxiliaryId));

        // 查询辅助核算项目信息
        Map<Long, AuxiliaryItem> auxiliaryItemMap = new HashMap<>();
        if (!auxiliaryGroupMap.isEmpty()) {
            List<AuxiliaryItem> auxiliaryItems = auxiliaryItemMapper.selectBatchIds(auxiliaryGroupMap.keySet());
            auxiliaryItemMap = auxiliaryItems.stream()
                    .collect(Collectors.toMap(AuxiliaryItem::getId, a -> a));
        }

        // 余额方向：应收为借方(1)，应付为贷方(2)
        int balanceDirection = subject.getBalanceDirection() != null ? subject.getBalanceDirection() : 1;
        // 账龄基准日取查询期间期末，保证同一查询结果可复现、可审计(避免用LocalDate.now()导致每日结果不同)
        LocalDate currentDate;
        if (month != null) {
            // 期间期末：该月最后一天
            currentDate = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        } else {
            // 未指定月份则取年末
            currentDate = LocalDate.of(year, 12, 31);
        }

        List<AgingAnalysisVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<VoucherDetail>> entry : auxiliaryGroupMap.entrySet()) {
            AuxiliaryItem auxiliaryItem = auxiliaryItemMap.get(entry.getKey());
            String auxiliaryItemName = auxiliaryItem != null ? auxiliaryItem.getItemName() : "";

            AgingAnalysisVO vo = new AgingAnalysisVO();
            vo.setSubjectCode(subject.getCode());
            vo.setSubjectName(subject.getName());
            vo.setAuxiliaryItemName(auxiliaryItemName);

            BigDecimal notDueAmount = BigDecimal.ZERO;
            BigDecimal due0to30 = BigDecimal.ZERO;
            BigDecimal due31to60 = BigDecimal.ZERO;
            BigDecimal due61to90 = BigDecimal.ZERO;
            BigDecimal due91to180 = BigDecimal.ZERO;
            BigDecimal dueOver180 = BigDecimal.ZERO;

            for (VoucherDetail detail : entry.getValue()) {
                Voucher voucher = voucherMap.get(detail.getVoucherId());
                if (voucher == null || voucher.getVoucherDate() == null) {
                    continue;
                }

                BigDecimal debit = detail.getDebit() != null ? detail.getDebit() : BigDecimal.ZERO;
                BigDecimal credit = detail.getCredit() != null ? detail.getCredit() : BigDecimal.ZERO;
                // 计算净额：应收为借方-贷方，应付为贷方-借方
                BigDecimal amount = balanceDirection == 1
                        ? debit.subtract(credit)
                        : credit.subtract(debit);

                // 计算账龄：信用期30天，到期日=凭证日期+30天
                LocalDate dueDate = voucher.getVoucherDate().plusDays(30);
                long daysOverdue = ChronoUnit.DAYS.between(dueDate, currentDate);

                if (daysOverdue < 0) {
                    notDueAmount = notDueAmount.add(amount);
                } else if (daysOverdue <= 30) {
                    due0to30 = due0to30.add(amount);
                } else if (daysOverdue <= 60) {
                    due31to60 = due31to60.add(amount);
                } else if (daysOverdue <= 90) {
                    due61to90 = due61to90.add(amount);
                } else if (daysOverdue <= 180) {
                    due91to180 = due91to180.add(amount);
                } else {
                    dueOver180 = dueOver180.add(amount);
                }
            }

            BigDecimal totalAmount = notDueAmount.add(due0to30).add(due31to60)
                    .add(due61to90).add(due91to180).add(dueOver180);

            vo.setTotalAmount(totalAmount);
            vo.setNotDueAmount(notDueAmount);
            vo.setDue0to30(due0to30);
            vo.setDue31to60(due31to60);
            vo.setDue61to90(due61to90);
            vo.setDue91to180(due91to180);
            vo.setDueOver180(dueOver180);

            // 风险等级：180天以上占比>50%为高风险
            String agingLevel = "低风险";
            if (totalAmount.compareTo(BigDecimal.ZERO) != 0) {
                double over180Ratio = dueOver180.doubleValue() / totalAmount.doubleValue();
                if (over180Ratio > 0.5) {
                    agingLevel = "高风险";
                } else if (over180Ratio > 0.2) {
                    agingLevel = "中风险";
                }
            }
            vo.setAgingLevel(agingLevel);

            result.add(vo);
        }

        // 按总金额降序排序
        result.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));

        log.info("查询账龄分析成功，共 {} 条记录", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ReconciliationVO reconciliation(Long accountSetId, Long subjectId, Long auxiliaryId, Integer year, Integer month) {
        validateLedgerParams(accountSetId, subjectId, year, month);
        if (auxiliaryId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "辅助核算项目ID不能为空");
        }

        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }
        Subject subject = subjectMapper.selectById(subjectId);
        if (subject == null || !accountSetId.equals(subject.getAccountSetId())) {
            throw new BusinessException(ErrorCode.LEDGER_SUBJECT_NOT_FOUND);
        }

        AuxiliaryItem auxiliaryItem = auxiliaryItemMapper.selectById(auxiliaryId);
        if (auxiliaryItem == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "辅助核算项目不存在");
        }

        log.info("查询往来对账，账套ID: {}, 科目ID: {}, 辅助核算ID: {}, 年度: {}, 月份: {}",
                accountSetId, subjectId, auxiliaryId, year, month);

        // 查询已过账的凭证(年初至当月,累计历史余额;month=null查全年)
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getStatus, 2)
                .eq(Voucher::getYear, year);
        if (month != null) {
            voucherWrapper.le(Voucher::getMonth, month);
        }
        voucherWrapper.orderByAsc(Voucher::getVoucherDate, Voucher::getVoucherNo);
        List<Voucher> vouchers = voucherMapper.selectList(voucherWrapper);

        Map<Long, Voucher> voucherMap = vouchers.stream()
                .collect(Collectors.toMap(Voucher::getId, v -> v));
        List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());

        List<VoucherDetail> details = Collections.emptyList();
        if (!voucherIds.isEmpty()) {
            LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                    .eq(VoucherDetail::getSubjectId, subjectId)
                    .eq(VoucherDetail::getAuxiliaryId, auxiliaryId);
            detailWrapper.orderByAsc(VoucherDetail::getSortOrder);
            details = voucherDetailMapper.selectList(detailWrapper);
            // Java层按凭证维度重新排序,跨凭证查询时sortOrder会重复,需先按凭证顺序排序
            sortDetailsByVoucherOrder(details, vouchers);
        }

        // 银企对账：从银行流水计算对方余额，并收集已核对的凭证ID集合
        // 1. 查询该账套、该科目关联的银行账户（BankAccount.subjectId 关联银行存款明细科目）
        LambdaQueryWrapper<BankAccount> bankAccountWrapper = new LambdaQueryWrapper<>();
        bankAccountWrapper.eq(BankAccount::getAccountSetId, accountSetId)
                .eq(BankAccount::getSubjectId, subjectId);
        List<BankAccount> bankAccounts = bankAccountMapper.selectList(bankAccountWrapper);

        BigDecimal counterpartBalance;
        // 已与银行流水核对的凭证ID集合（BankTransaction.voucherId 关联凭证）
        Set<Long> matchedVoucherIds = new HashSet<>();
        // 银行流水明细集合，用于明细级别的核对标识
        if (bankAccounts.isEmpty()) {
            // 无银行账户数据，对方余额为0
            counterpartBalance = BigDecimal.ZERO;
        } else {
            // 提取银行账号（BankTransaction 通过 bankAccount 账号字符串关联银行账户）
            List<String> accountNumbers = bankAccounts.stream()
                    .map(BankAccount::getAccountNumber)
                    .filter(s -> s != null && !s.isEmpty())
                    .collect(Collectors.toList());

            // 期初余额累计
            // 已知限制：beginningBalance 为开户时设置的静态字段（BankAccount 无 year 字段），无年度关联。
            // 跨年度对账时该字段仍是开户年度的期初，加上今年全年流水会导致对方余额错误，
            // 跨年度对账需用户手动调整期初余额。
            BigDecimal beginningTotal = bankAccounts.stream()
                    .map(a -> a.getBeginningBalance() != null ? a.getBeginningBalance() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // 跨年度对账告警：若查询年度与银行账户开户年度不同，提示用户期初余额可能不准确
            for (BankAccount ba : bankAccounts) {
                if (ba.getOpenDate() != null && ba.getOpenDate().getYear() != year) {
                    log.warn("银行对账期初告警：查询年度{}与银行账户{}开户年度{}不一致，beginningBalance为开户期初，"
                                    + "跨年度对方余额可能不准确，请用户手动调整期初余额",
                            year, ba.getAccountNumber(), ba.getOpenDate().getYear());
                }
            }

            if (accountNumbers.isEmpty()) {
                // 银行账户未登记账号，仅以期初余额作为对方余额
                counterpartBalance = beginningTotal;
            } else {
                // 计算期间范围：年初至所选月份末（month=null 则取全年）
                LocalDate startDate = LocalDate.of(year, 1, 1);
                LocalDate endDate = month != null
                        ? LocalDate.of(year, month, 1).withDayOfMonth(LocalDate.of(year, month, 1).lengthOfMonth())
                        : LocalDate.of(year, 12, 31);

                // 已知限制：BankTransaction 实体无 auxiliaryId 字段，对方余额（银行流水）仅按
                // bankAccount 账号 + 日期汇总，不区分辅助核算。而账面余额按 subjectId + auxiliaryId 过滤，
                // 若一个银行科目下挂多个辅助核算，对方余额是全部客户的流水合计，与账面余额不可比。
                // 多辅助核算场景需用户注意口径差异。
                log.warn("银行对账口径告警：对方余额按银行账号汇总，不区分辅助核算（auxiliaryId={}），"
                                + "多辅助核算场景下对方余额为全部客户流水合计，与账面余额不可比", auxiliaryId);
                LambdaQueryWrapper<BankTransaction> bankTxnWrapper = new LambdaQueryWrapper<>();
                bankTxnWrapper.in(BankTransaction::getBankAccount, accountNumbers)
                        .ge(BankTransaction::getTransactionDate, startDate)
                        .le(BankTransaction::getTransactionDate, endDate);
                List<BankTransaction> bankTransactions = bankTransactionMapper.selectList(bankTxnWrapper);

                // 收入流水（transactionType=1）
                BigDecimal incomeTotal = bankTransactions.stream()
                        .filter(t -> t.getTransactionType() != null && t.getTransactionType() == 1)
                        .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                // 支出流水（transactionType=2）
                BigDecimal expenseTotal = bankTransactions.stream()
                        .filter(t -> t.getTransactionType() != null && t.getTransactionType() == 2)
                        .map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 对方余额 = 期初余额 + 收入流水 - 支出流水
                counterpartBalance = beginningTotal.add(incomeTotal).subtract(expenseTotal);

                // 收集已与银行流水核对的凭证ID（bankTransaction.voucherId 不为空表示已生成凭证/已核对）
                matchedVoucherIds = bankTransactions.stream()
                        .map(BankTransaction::getVoucherId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }
        }

        // 构建明细列表并计算账面余额
        List<ReconciliationDetailVO> detailList = new ArrayList<>();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (VoucherDetail detail : details) {
            Voucher voucher = voucherMap.get(detail.getVoucherId());
            if (voucher == null) {
                continue;
            }

            ReconciliationDetailVO detailVO = new ReconciliationDetailVO();
            detailVO.setVoucherDate(voucher.getVoucherDate());
            detailVO.setVoucherNo(String.valueOf(voucher.getVoucherNo()));
            detailVO.setSummary(detail.getSummary());
            BigDecimal debit = detail.getDebit() != null ? detail.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = detail.getCredit() != null ? detail.getCredit() : BigDecimal.ZERO;
            detailVO.setDebit(debit);
            detailVO.setCredit(credit);
            // 凭证已与银行流水核对则标记为已核对
            detailVO.setMatched(matchedVoucherIds.contains(detail.getVoucherId()));

            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);

            detailList.add(detailVO);
        }

        // 账面余额:借方科目(资产类)=借方累计-贷方累计;贷方科目(负债类)=贷方累计-借方累计
        int balanceDirection = subject.getBalanceDirection() != null ? subject.getBalanceDirection() : 1;
        BigDecimal bookBalance = (balanceDirection == 1)
                ? totalDebit.subtract(totalCredit)
                : totalCredit.subtract(totalDebit);
        BigDecimal difference = bookBalance.subtract(counterpartBalance);
        String status = difference.compareTo(BigDecimal.ZERO) == 0 ? "已平" : "未平";

        ReconciliationVO result = new ReconciliationVO();
        result.setSubjectCode(subject.getCode());
        result.setSubjectName(subject.getName());
        result.setAuxiliaryItemName(auxiliaryItem.getItemName());
        result.setBookBalance(bookBalance);
        result.setCounterpartBalance(counterpartBalance);
        result.setDifference(difference);
        result.setStatus(status);
        result.setDetails(detailList);

        log.info("查询往来对账成功，共 {} 条记录", detailList.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountCheckVO> accountCheck(Long accountSetId, Integer year, Integer month) {
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }
        // IDOR越权校验：账套读权限
        accountSetAccessService.checkAccess(accountSetId);
        if (year == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "年度不能为空");
        }
        if (year < 1900 || year > 2099) {
            throw new BusinessException(ErrorCode.LEDGER_YEAR_INVALID);
        }
        if (month != null && (month < 1 || month > 12)) {
            throw new BusinessException(ErrorCode.LEDGER_MONTH_INVALID);
        }

        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }

        log.info("查询账账核对，账套ID: {}, 年度: {}, 月份: {}", accountSetId, year, month);

        List<AccountCheckVO> result = new ArrayList<>();

        // 查询科目余额
        LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
        balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getYear, year);
        if (month != null) {
            balanceWrapper.eq(AccountBalance::getMonth, month);
        }
        List<AccountBalance> balances = accountBalanceMapper.selectList(balanceWrapper);

        // 查询科目信息
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, accountSetId);
        List<Subject> subjects = subjectMapper.selectList(subjectWrapper);
        Map<Long, Subject> subjectMap = subjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s));

        // 查询已过账的凭证
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getStatus, 2)
                .eq(Voucher::getYear, year);
        if (month != null) {
            voucherWrapper.eq(Voucher::getMonth, month);
        }
        List<Voucher> vouchers = voucherMapper.selectList(voucherWrapper);

        List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());
        List<VoucherDetail> allDetails = Collections.emptyList();
        if (!voucherIds.isEmpty()) {
            LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.in(VoucherDetail::getVoucherId, voucherIds);
            allDetails = voucherDetailMapper.selectList(detailWrapper);
        }

        // 1. 总账与明细账核对：比较总账本期发生额与明细账汇总
        BigDecimal generalPeriodDebit = BigDecimal.ZERO;
        BigDecimal generalPeriodCredit = BigDecimal.ZERO;
        for (AccountBalance balance : balances) {
            generalPeriodDebit = generalPeriodDebit.add(balance.getPeriodDebit() != null ? balance.getPeriodDebit() : BigDecimal.ZERO);
            generalPeriodCredit = generalPeriodCredit.add(balance.getPeriodCredit() != null ? balance.getPeriodCredit() : BigDecimal.ZERO);
        }

        BigDecimal detailTotalDebit = BigDecimal.ZERO;
        BigDecimal detailTotalCredit = BigDecimal.ZERO;
        for (VoucherDetail detail : allDetails) {
            detailTotalDebit = detailTotalDebit.add(detail.getDebit() != null ? detail.getDebit() : BigDecimal.ZERO);
            detailTotalCredit = detailTotalCredit.add(detail.getCredit() != null ? detail.getCredit() : BigDecimal.ZERO);
        }

        AccountCheckVO check1 = new AccountCheckVO();
        check1.setCheckType("general_vs_detail");
        check1.setCheckName("总账与明细账核对");
        check1.setLeftAmount(generalPeriodDebit);
        check1.setRightAmount(detailTotalDebit);
        check1.setDifference(generalPeriodDebit.subtract(detailTotalDebit));
        // AccountCheckVO 仅有 leftAmount/rightAmount 单组金额字段，当前仅展示借方对比。
        // 复式记账下借贷必相等，借方平衡则贷方必然平衡，故 balanced 仅按借方判定，
        // 避免出现借方差额为 0 但因贷方不一致而 balanced=false 的展示与判定不一致问题。
        check1.setBalanced(generalPeriodDebit.compareTo(detailTotalDebit) == 0);
        check1.setDescription(check1.getBalanced() ? "总账与明细账借贷方发生额一致" : "总账与明细账借贷方发生额不一致");
        result.add(check1);

        // 2. 总账与科目余额表核对：比较总账期末借贷方合计是否平衡
        BigDecimal generalEndDebit = BigDecimal.ZERO;
        BigDecimal generalEndCredit = BigDecimal.ZERO;
        for (AccountBalance balance : balances) {
            generalEndDebit = generalEndDebit.add(balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO);
            generalEndCredit = generalEndCredit.add(balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO);
        }

        AccountCheckVO check2 = new AccountCheckVO();
        check2.setCheckType("general_vs_subject_balance");
        check2.setCheckName("总账与科目余额表核对");
        check2.setLeftAmount(generalEndDebit);
        check2.setRightAmount(generalEndCredit);
        check2.setDifference(generalEndDebit.subtract(generalEndCredit));
        check2.setBalanced(generalEndDebit.compareTo(generalEndCredit) == 0);
        check2.setDescription(check2.getBalanced() ? "总账期末借贷方合计平衡" : "总账期末借贷方合计不平衡");
        result.add(check2);

        // 3. 现金总账与现金日记账核对
        List<Subject> cashSubjects = subjects.stream()
                .filter(s -> s.getIsCash() != null && s.getIsCash() == 1)
                .collect(Collectors.toList());
        BigDecimal cashGeneralBalance = BigDecimal.ZERO;
        for (Subject cashSubject : cashSubjects) {
            for (AccountBalance balance : balances) {
                if (cashSubject.getId().equals(balance.getSubjectId())) {
                    BigDecimal debit = balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO;
                    BigDecimal credit = balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO;
                    int dir = cashSubject.getBalanceDirection() != null ? cashSubject.getBalanceDirection() : 1;
                    cashGeneralBalance = cashGeneralBalance.add(dir == 1 ? debit.subtract(credit) : credit.subtract(debit));
                }
            }
        }
        Set<Long> cashSubjectIds = cashSubjects.stream().map(Subject::getId).collect(Collectors.toSet());
        BigDecimal cashJournalBalance = BigDecimal.ZERO;
        for (VoucherDetail detail : allDetails) {
            if (cashSubjectIds.contains(detail.getSubjectId())) {
                BigDecimal debit = detail.getDebit() != null ? detail.getDebit() : BigDecimal.ZERO;
                BigDecimal credit = detail.getCredit() != null ? detail.getCredit() : BigDecimal.ZERO;
                cashJournalBalance = cashJournalBalance.add(debit.subtract(credit));
            }
        }

        AccountCheckVO check3 = new AccountCheckVO();
        check3.setCheckType("cash_general_vs_journal");
        check3.setCheckName("现金总账与现金日记账核对");
        check3.setLeftAmount(cashGeneralBalance);
        check3.setRightAmount(cashJournalBalance);
        check3.setDifference(cashGeneralBalance.subtract(cashJournalBalance));
        check3.setBalanced(cashGeneralBalance.compareTo(cashJournalBalance) == 0);
        check3.setDescription(check3.getBalanced() ? "现金总账与现金日记账余额一致" : "现金总账与现金日记账余额不一致");
        result.add(check3);

        // 4. 银行总账与银行日记账核对
        List<Subject> bankSubjects = subjects.stream()
                .filter(s -> s.getIsBank() != null && s.getIsBank() == 1)
                .collect(Collectors.toList());
        BigDecimal bankGeneralBalance = BigDecimal.ZERO;
        for (Subject bankSubject : bankSubjects) {
            for (AccountBalance balance : balances) {
                if (bankSubject.getId().equals(balance.getSubjectId())) {
                    BigDecimal debit = balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO;
                    BigDecimal credit = balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO;
                    int dir = bankSubject.getBalanceDirection() != null ? bankSubject.getBalanceDirection() : 1;
                    bankGeneralBalance = bankGeneralBalance.add(dir == 1 ? debit.subtract(credit) : credit.subtract(debit));
                }
            }
        }
        Set<Long> bankSubjectIds = bankSubjects.stream().map(Subject::getId).collect(Collectors.toSet());
        BigDecimal bankJournalBalance = BigDecimal.ZERO;
        for (VoucherDetail detail : allDetails) {
            if (bankSubjectIds.contains(detail.getSubjectId())) {
                BigDecimal debit = detail.getDebit() != null ? detail.getDebit() : BigDecimal.ZERO;
                BigDecimal credit = detail.getCredit() != null ? detail.getCredit() : BigDecimal.ZERO;
                bankJournalBalance = bankJournalBalance.add(debit.subtract(credit));
            }
        }

        AccountCheckVO check4 = new AccountCheckVO();
        check4.setCheckType("bank_general_vs_journal");
        check4.setCheckName("银行总账与银行日记账核对");
        check4.setLeftAmount(bankGeneralBalance);
        check4.setRightAmount(bankJournalBalance);
        check4.setDifference(bankGeneralBalance.subtract(bankJournalBalance));
        check4.setBalanced(bankGeneralBalance.compareTo(bankJournalBalance) == 0);
        check4.setDescription(check4.getBalanced() ? "银行总账与银行日记账余额一致" : "银行总账与银行日记账余额不一致");
        result.add(check4);

        // 5. 资产=负债+所有者权益 核对
        BigDecimal assetBalance = BigDecimal.ZERO;
        BigDecimal liabilityBalance = BigDecimal.ZERO;
        BigDecimal equityBalance = BigDecimal.ZERO;
        for (AccountBalance balance : balances) {
            Subject subject = subjectMap.get(balance.getSubjectId());
            if (subject == null || subject.getCategory() == null) {
                continue;
            }
            BigDecimal debit = balance.getEndDebit() != null ? balance.getEndDebit() : BigDecimal.ZERO;
            BigDecimal credit = balance.getEndCredit() != null ? balance.getEndCredit() : BigDecimal.ZERO;
            BigDecimal net = debit.subtract(credit);
            String category = subject.getCategory();
            if ("资产".equals(category)) {
                assetBalance = assetBalance.add(net);
            } else if ("负债".equals(category)) {
                liabilityBalance = liabilityBalance.add(net.negate());
            } else if ("所有者权益".equals(category)) {
                equityBalance = equityBalance.add(net.negate());
            }
        }

        AccountCheckVO check5 = new AccountCheckVO();
        check5.setCheckType("accounting_equation");
        check5.setCheckName("资产=负债+所有者权益");
        check5.setLeftAmount(assetBalance);
        check5.setRightAmount(liabilityBalance.add(equityBalance));
        check5.setDifference(assetBalance.subtract(liabilityBalance).subtract(equityBalance));
        check5.setBalanced(assetBalance.compareTo(liabilityBalance.add(equityBalance)) == 0);
        check5.setDescription(check5.getBalanced() ? "会计等式平衡" : "会计等式不平衡");
        result.add(check5);

        // 6. 借方合计=贷方合计 核对
        BigDecimal totalDebitAmount = BigDecimal.ZERO;
        BigDecimal totalCreditAmount = BigDecimal.ZERO;
        for (VoucherDetail detail : allDetails) {
            totalDebitAmount = totalDebitAmount.add(detail.getDebit() != null ? detail.getDebit() : BigDecimal.ZERO);
            totalCreditAmount = totalCreditAmount.add(detail.getCredit() != null ? detail.getCredit() : BigDecimal.ZERO);
        }

        AccountCheckVO check6 = new AccountCheckVO();
        check6.setCheckType("debit_equals_credit");
        check6.setCheckName("借方合计=贷方合计");
        check6.setLeftAmount(totalDebitAmount);
        check6.setRightAmount(totalCreditAmount);
        check6.setDifference(totalDebitAmount.subtract(totalCreditAmount));
        check6.setBalanced(totalDebitAmount.compareTo(totalCreditAmount) == 0);
        check6.setDescription(check6.getBalanced() ? "借贷方合计平衡" : "借贷方合计不平衡");
        result.add(check6);

        log.info("查询账账核对成功，共 {} 条核对项", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public void exportCashJournal(Long accountSetId, Integer year, Integer month, HttpServletResponse response) {
        // 复用现金日记账查询逻辑，pageSize设大值以导出全部
        LedgerQueryRequest request = new LedgerQueryRequest();
        request.setAccountSetId(accountSetId);
        request.setYear(year);
        request.setMonth(month);
        request.setPageNum(1);
        request.setPageSize(100000);
        PageResult<CashJournalVO> page = cashJournal(request);
        writeJournalExcel(page.getList(), "现金日记账", year, month, response);
    }

    @Override
    @Transactional(readOnly = true)
    public void exportBankJournal(Long accountSetId, Integer year, Integer month, Long bankAccountId, HttpServletResponse response) {
        // 复用银行日记账查询逻辑（按所有银行科目汇总），pageSize设大值以导出全部
        // bankAccountId为预留参数，当前导出全部银行科目日记账
        LedgerQueryRequest request = new LedgerQueryRequest();
        request.setAccountSetId(accountSetId);
        request.setYear(year);
        request.setMonth(month);
        request.setPageNum(1);
        request.setPageSize(100000);
        PageResult<CashJournalVO> page = bankJournal(request);
        writeJournalExcel(page.getList(), "银行日记账", year, month, response);
    }

    /**
     * 将日记账数据写入Excel并输出到响应
     * 列：日期、凭证号、摘要、借方(收入)、贷方(支出)、余额
     */
    private void writeJournalExcel(List<CashJournalVO> list, String title, Integer year, Integer month, HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(title);
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title + " " + year + "年" + (month != null ? month + "月" : ""));
            titleCell.setCellStyle(headerStyle);

            // 表头
            Row headerRow = sheet.createRow(2);
            String[] headers = {"日期", "凭证号", "摘要", "借方", "贷方", "余额"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行（借方=收入，贷方=支出）
            int rowNum = 3;
            if (list != null) {
                for (CashJournalVO vo : list) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(vo.getVoucherDate() != null ? vo.getVoucherDate().toString() : "");
                    row.createCell(1).setCellValue(vo.getVoucherNo() != null ? vo.getVoucherNo() : "");
                    row.createCell(2).setCellValue(vo.getSummary() != null ? vo.getSummary() : "");
                    row.createCell(3).setCellValue(vo.getIncome() != null ? vo.getIncome().doubleValue() : 0);
                    row.createCell(4).setCellValue(vo.getExpense() != null ? vo.getExpense().doubleValue() : 0);
                    row.createCell(5).setCellValue(vo.getBalance() != null ? vo.getBalance().doubleValue() : 0);
                }
            }

            // 列宽
            sheet.setColumnWidth(0, 12 * 256);
            sheet.setColumnWidth(1, 14 * 256);
            sheet.setColumnWidth(2, 30 * 256);
            for (int i = 3; i < 6; i++) {
                sheet.setColumnWidth(i, 15 * 256);
            }

            workbook.write(out);
            writeExcelToResponse(response, out.toByteArray(), title + "_" + year + "年" + (month != null ? month + "月" : "") + ".xlsx");
        } catch (IOException e) {
            log.error("导出{}失败", title, e);
            throw new RuntimeException("导出" + title + "失败", e);
        }
    }

    /**
     * 将Excel字节数组写入HTTP响应
     */
    private void writeExcelToResponse(HttpServletResponse response, byte[] data, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName);
        try (OutputStream os = response.getOutputStream()) {
            os.write(data);
            os.flush();
        } catch (IOException e) {
            log.error("写入Excel响应失败", e);
            throw new RuntimeException("导出失败", e);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 校验账簿查询参数
     */
    private void validateLedgerParams(Long accountSetId, Long subjectId, Integer year, Integer month) {
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }
        // IDOR越权校验：账套读权限
        accountSetAccessService.checkAccess(accountSetId);
        if (subjectId == null) {
            throw new BusinessException(ErrorCode.LEDGER_SUBJECT_ID_BLANK);
        }
        if (year == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "年度不能为空");
        }
        if (year < 1900 || year > 2099) {
            throw new BusinessException(ErrorCode.LEDGER_YEAR_INVALID);
        }
        if (month != null && (month < 1 || month > 12)) {
            throw new BusinessException(ErrorCode.LEDGER_MONTH_INVALID);
        }
    }

    /**
     * 按凭证日期+凭证号顺序对凭证明细重新排序(同凭证内按sortOrder)。
     * sortOrder 是每张凭证从1开始,跨凭证查询时直接按sortOrder排序会乱序(数据库返回顺序不定),
     * 需先按凭证维度(voucherDate, voucherNo)排序,同凭证内再按sortOrder排序。
     *
     * @param details         待排序的凭证明细
     * @param sortedVouchers  已按 voucherDate, voucherNo 排序的凭证列表
     */
    private void sortDetailsByVoucherOrder(List<VoucherDetail> details, List<Voucher> sortedVouchers) {
        if (details == null || details.isEmpty() || sortedVouchers == null || sortedVouchers.isEmpty()) {
            return;
        }
        Map<Long, Integer> voucherOrderMap = new HashMap<>();
        for (int i = 0; i < sortedVouchers.size(); i++) {
            voucherOrderMap.put(sortedVouchers.get(i).getId(), i);
        }
        details.sort(Comparator.comparingInt((VoucherDetail d) -> voucherOrderMap.getOrDefault(d.getVoucherId(), Integer.MAX_VALUE))
                .thenComparingInt(VoucherDetail::getSortOrder));
    }

    /**
     * 查询已过账的凭证明细
     */
    private List<VoucherDetail> queryPostedVoucherDetails(Long accountSetId, Long subjectId, Integer year, Integer month) {
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getStatus, 2)
                .eq(Voucher::getYear, year);
        if (month != null) {
            voucherWrapper.eq(Voucher::getMonth, month);
        }
        voucherWrapper.orderByAsc(Voucher::getVoucherDate, Voucher::getVoucherNo);
        List<Voucher> vouchers = voucherMapper.selectList(voucherWrapper);

        if (vouchers.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());
        LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                .eq(VoucherDetail::getSubjectId, subjectId);
        List<VoucherDetail> details = voucherDetailMapper.selectList(detailWrapper);
        // Java层按凭证维度重新排序,跨凭证查询时sortOrder会重复,需先按凭证顺序排序
        sortDetailsByVoucherOrder(details, vouchers);
        return details;
    }

    /**
     * 查询凭证映射
     */
    private Map<Long, Voucher> queryVoucherMap(Long accountSetId, Integer year, Integer month) {
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getStatus, 2)
                .eq(Voucher::getYear, year);
        if (month != null) {
            voucherWrapper.eq(Voucher::getMonth, month);
        }
        List<Voucher> vouchers = voucherMapper.selectList(voucherWrapper);
        return vouchers.stream().collect(Collectors.toMap(Voucher::getId, v -> v));
    }

    /**
     * 获取期初余额（month=null表示查全年,取1月期初作为年初余额）
     */
    private BigDecimal getBeginBalance(Long accountSetId, Long subjectId, Integer year, Integer month, int balanceDirection) {
        BigDecimal beginBalance = BigDecimal.ZERO;
        Integer balanceMonth = (month != null) ? month : 1;
        LambdaQueryWrapper<AccountBalance> balanceWrapper = new LambdaQueryWrapper<>();
        balanceWrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getSubjectId, subjectId)
                .eq(AccountBalance::getYear, year)
                .eq(AccountBalance::getMonth, balanceMonth);
        AccountBalance accountBalance = accountBalanceMapper.selectOne(balanceWrapper);
        if (accountBalance != null) {
            if (balanceDirection == 1) {
                BigDecimal d = accountBalance.getBeginDebit() != null ? accountBalance.getBeginDebit() : BigDecimal.ZERO;
                BigDecimal c = accountBalance.getBeginCredit() != null ? accountBalance.getBeginCredit() : BigDecimal.ZERO;
                beginBalance = d.subtract(c);
            } else {
                BigDecimal d = accountBalance.getBeginDebit() != null ? accountBalance.getBeginDebit() : BigDecimal.ZERO;
                BigDecimal c = accountBalance.getBeginCredit() != null ? accountBalance.getBeginCredit() : BigDecimal.ZERO;
                beginBalance = c.subtract(d);
            }
        }
        return beginBalance;
    }

    /**
     * 匹配栏目：根据摘要匹配配置的栏目项
     */
    private String matchColumn(String summary, List<String> columnItems) {
        if (summary == null || summary.isEmpty() || columnItems.isEmpty()) {
            return null;
        }
        for (String column : columnItems) {
            String trimmed = column.trim();
            if (!trimmed.isEmpty() && summary.contains(trimmed)) {
                return trimmed;
            }
        }
        return null;
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        return headerStyle;
    }

    /**
     * 辅助核算余额表
     * 按"科目 + 辅助核算项"维度汇总期初/本期发生/期末借贷方余额
     * 期初余额算法与 auxiliaryDetailLedger 一致:从年初至查询月份之前(不含)的已过账凭证明细累计
     */
    @Override
    @Transactional(readOnly = true)
    public List<AuxiliaryBalanceVO> auxiliaryBalance(Long accountSetId, Long categoryId, Integer year, Integer month) {
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }
        if (year == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "年度不能为空");
        }
        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.LEDGER_ACCOUNT_SET_NOT_FOUND);
        }
        accountSetAccessService.checkAccess(accountSetId);
        log.info("辅助核算余额表查询: accountSetId={}, categoryId={}, year={}, month={}", accountSetId, categoryId, year, month);

        // 1. 查询本年已过账凭证(用于本期发生额;若指定month则仅查该月)
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, accountSetId)
                .eq(Voucher::getStatus, 2)
                .eq(Voucher::getYear, year);
        if (month != null) {
            voucherWrapper.eq(Voucher::getMonth, month);
        }
        List<Voucher> vouchers = voucherMapper.selectList(voucherWrapper);
        List<Long> voucherIds = vouchers.stream().map(Voucher::getId).collect(Collectors.toList());

        // 2. 查询本年已过账凭证(用于期初;month之前,不含month)
        List<Long> beforeVoucherIds = Collections.emptyList();
        if (month != null) {
            LambdaQueryWrapper<Voucher> beforeWrapper = new LambdaQueryWrapper<>();
            beforeWrapper.eq(Voucher::getAccountSetId, accountSetId)
                    .eq(Voucher::getStatus, 2)
                    .eq(Voucher::getYear, year)
                    .lt(Voucher::getMonth, month);
            List<Voucher> beforeVouchers = voucherMapper.selectList(beforeWrapper);
            beforeVoucherIds = beforeVouchers.stream().map(Voucher::getId).collect(Collectors.toList());
        }

        // 3. 查询有辅助核算的凭证明细(本期)
        List<VoucherDetail> periodDetails = Collections.emptyList();
        if (!voucherIds.isEmpty()) {
            LambdaQueryWrapper<VoucherDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.in(VoucherDetail::getVoucherId, voucherIds)
                    .isNotNull(VoucherDetail::getAuxiliaryId)
                    .ne(VoucherDetail::getAuxiliaryId, 0);
            periodDetails = voucherDetailMapper.selectList(detailWrapper);
        }

        // 4. 查询期初凭证明细(month之前)
        List<VoucherDetail> beforeDetails = Collections.emptyList();
        if (!beforeVoucherIds.isEmpty()) {
            LambdaQueryWrapper<VoucherDetail> beforeDetailWrapper = new LambdaQueryWrapper<>();
            beforeDetailWrapper.in(VoucherDetail::getVoucherId, beforeVoucherIds)
                    .isNotNull(VoucherDetail::getAuxiliaryId)
                    .ne(VoucherDetail::getAuxiliaryId, 0);
            beforeDetails = voucherDetailMapper.selectList(beforeDetailWrapper);
        }

        // 5. 收集所有涉及的 auxiliaryId,批量查辅助核算项目和类别
        Set<Long> auxiliaryIds = new HashSet<>();
        for (VoucherDetail d : periodDetails) { auxiliaryIds.add(d.getAuxiliaryId()); }
        for (VoucherDetail d : beforeDetails) { auxiliaryIds.add(d.getAuxiliaryId()); }
        Map<Long, AuxiliaryItem> itemMap = Collections.emptyMap();
        Map<Long, AuxiliaryCategory> categoryMap = Collections.emptyMap();
        if (!auxiliaryIds.isEmpty()) {
            List<AuxiliaryItem> items = auxiliaryItemMapper.selectBatchIds(auxiliaryIds);
            itemMap = items.stream().collect(Collectors.toMap(AuxiliaryItem::getId, i -> i));
            Set<Long> categoryIds = items.stream()
                    .map(AuxiliaryItem::getCategoryId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!categoryIds.isEmpty()) {
                categoryMap = auxiliaryCategoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(AuxiliaryCategory::getId, c -> c));
            }
        }

        // 6. 按 categoryId 过滤(若指定)
        final Map<Long, AuxiliaryItem> finalItemMap = itemMap;
        final Map<Long, AuxiliaryCategory> finalCategoryMap = categoryMap;
        if (categoryId != null) {
            periodDetails = periodDetails.stream()
                    .filter(d -> matchesCategory(d.getAuxiliaryId(), categoryId, finalItemMap))
                    .collect(Collectors.toList());
            beforeDetails = beforeDetails.stream()
                    .filter(d -> matchesCategory(d.getAuxiliaryId(), categoryId, finalItemMap))
                    .collect(Collectors.toList());
        }

        // 7. 收集涉及的 subjectId,批量查科目
        Set<Long> subjectIds = new HashSet<>();
        for (VoucherDetail d : periodDetails) { subjectIds.add(d.getSubjectId()); }
        for (VoucherDetail d : beforeDetails) { subjectIds.add(d.getSubjectId()); }
        Map<Long, Subject> subjectMap = Collections.emptyMap();
        if (!subjectIds.isEmpty()) {
            subjectMap = subjectMapper.selectBatchIds(subjectIds).stream()
                    .collect(Collectors.toMap(Subject::getId, s -> s));
        }

        // 8. 按 (subjectId, auxiliaryId) 分组聚合期初和本期发生额
        // key = subjectId + "|" + auxiliaryId
        Map<String, BigDecimal> beginNetMap = new HashMap<>();      // 净额(借-贷)
        Map<String, BigDecimal> periodDebitMap = new HashMap<>();
        Map<String, BigDecimal> periodCreditMap = new HashMap<>();
        Map<String, Long> subjMap = new HashMap<>();
        Map<String, Long> auxMap = new HashMap<>();

        for (VoucherDetail d : beforeDetails) {
            String key = d.getSubjectId() + "|" + d.getAuxiliaryId();
            BigDecimal debit = d.getDebit() != null ? d.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = d.getCredit() != null ? d.getCredit() : BigDecimal.ZERO;
            beginNetMap.merge(key, debit.subtract(credit), BigDecimal::add);
            subjMap.putIfAbsent(key, d.getSubjectId());
            auxMap.putIfAbsent(key, d.getAuxiliaryId());
        }
        for (VoucherDetail d : periodDetails) {
            String key = d.getSubjectId() + "|" + d.getAuxiliaryId();
            BigDecimal debit = d.getDebit() != null ? d.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = d.getCredit() != null ? d.getCredit() : BigDecimal.ZERO;
            periodDebitMap.merge(key, debit, BigDecimal::add);
            periodCreditMap.merge(key, credit, BigDecimal::add);
            subjMap.putIfAbsent(key, d.getSubjectId());
            auxMap.putIfAbsent(key, d.getAuxiliaryId());
        }

        // 9. 构建VO列表
        List<AuxiliaryBalanceVO> result = new ArrayList<>();
        for (String key : subjMap.keySet()) {
            Long subjectId = subjMap.get(key);
            Long auxiliaryId = auxMap.get(key);
            Subject subject = subjectMap.get(subjectId);
            if (subject == null) continue;
            AuxiliaryItem item = itemMap.get(auxiliaryId);
            if (item == null) continue;

            int balanceDirection = (subject.getBalanceDirection() != null) ? subject.getBalanceDirection() : 1;
            BigDecimal beginNet = beginNetMap.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal periodDebit = periodDebitMap.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal periodCredit = periodCreditMap.getOrDefault(key, BigDecimal.ZERO);

            // 期初按方向拆借/贷(与 auxiliaryDetailLedger 一致)
            BigDecimal beginDebit = beginNet.compareTo(BigDecimal.ZERO) >= 0 ? beginNet : BigDecimal.ZERO;
            BigDecimal beginCredit = beginNet.compareTo(BigDecimal.ZERO) < 0 ? beginNet.negate() : BigDecimal.ZERO;

            // 期末 = 期初净额 + 本期净额(按方向)
            BigDecimal endNet;
            if (balanceDirection == 1) {
                // 借方科目: 期末净 = 期初借 - 期初贷 + 本期借 - 本期贷
                endNet = beginDebit.subtract(beginCredit).add(periodDebit).subtract(periodCredit);
            } else {
                // 贷方科目: 期末净 = 期初贷 - 期初借 + 本期贷 - 本期借
                endNet = beginCredit.subtract(beginDebit).add(periodCredit).subtract(periodDebit);
            }
            BigDecimal endDebit = endNet.compareTo(BigDecimal.ZERO) >= 0 ? endNet : BigDecimal.ZERO;
            BigDecimal endCredit = endNet.compareTo(BigDecimal.ZERO) < 0 ? endNet.negate() : BigDecimal.ZERO;

            AuxiliaryBalanceVO vo = new AuxiliaryBalanceVO();
            vo.setSubjectCode(subject.getCode());
            vo.setSubjectName(subject.getName());
            vo.setAuxiliaryId(auxiliaryId);
            vo.setAuxiliaryItemName(item.getItemName());
            vo.setAuxiliaryCategoryId(item.getCategoryId());
            if (item.getCategoryId() != null) {
                AuxiliaryCategory cat = categoryMap.get(item.getCategoryId());
                if (cat != null) {
                    vo.setAuxiliaryCategoryName(cat.getCategoryName());
                }
            }
            vo.setBeginDebit(beginDebit);
            vo.setBeginCredit(beginCredit);
            vo.setPeriodDebit(periodDebit);
            vo.setPeriodCredit(periodCredit);
            vo.setEndDebit(endDebit);
            vo.setEndCredit(endCredit);
            int cmp = endNet.compareTo(BigDecimal.ZERO);
            vo.setBalanceDirection(cmp > 0 ? "借" : (cmp < 0 ? "贷" : "平"));
            result.add(vo);
        }

        // 10. 排序:科目编码 → 类别名 → 项目名
        result.sort(Comparator
                .comparing(AuxiliaryBalanceVO::getSubjectCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(AuxiliaryBalanceVO::getAuxiliaryCategoryName, Comparator.nullsLast(String::compareTo))
                .thenComparing(AuxiliaryBalanceVO::getAuxiliaryItemName, Comparator.nullsLast(String::compareTo)));

        log.info("辅助核算余额表查询成功,共 {} 条记录", result.size());
        return result;
    }

    /**
     * 判断辅助核算项是否属于指定类别
     */
    private boolean matchesCategory(Long auxiliaryId, Long categoryId, Map<Long, AuxiliaryItem> itemMap) {
        if (auxiliaryId == null || categoryId == null) return false;
        AuxiliaryItem item = itemMap.get(auxiliaryId);
        return item != null && categoryId.equals(item.getCategoryId());
    }
}
