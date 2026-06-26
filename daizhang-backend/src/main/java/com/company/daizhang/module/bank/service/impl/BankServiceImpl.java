package com.company.daizhang.module.bank.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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

            Voucher matchedVoucher = null;
            String matchType = null;

            // 先尝试精确匹配（金额相同+日期相同）
            for (Voucher voucher : vouchers) {
                List<VoucherDetail> voucherDetails = detailsByVoucherId.get(voucher.getId());
                if (voucherDetails == null) {
                    continue;
                }

                for (VoucherDetail detail : voucherDetails) {
                    BigDecimal detailAmount = transaction.getTransactionType() == 1
                            ? detail.getDebit() : detail.getCredit();
                    if (detailAmount != null && detailAmount.compareTo(txAmount) == 0
                            && voucher.getVoucherDate().equals(txDate)) {
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
                        BigDecimal detailAmount = transaction.getTransactionType() == 1
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
        vo.setType(transaction.getTransactionType() != null && transaction.getTransactionType() == 1
                ? "收入" : "支出");
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
