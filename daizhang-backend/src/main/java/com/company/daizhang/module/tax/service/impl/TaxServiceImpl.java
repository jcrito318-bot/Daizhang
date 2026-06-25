package com.company.daizhang.module.tax.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.accountset.entity.AccountBalance;
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.mapper.AccountBalanceMapper;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
import com.company.daizhang.module.accountset.mapper.AccountSetMapper;
import com.company.daizhang.module.asset.mapper.FixedAssetMapper;
import com.company.daizhang.module.document.entity.InputInvoice;
import com.company.daizhang.module.document.entity.OutputInvoice;
import com.company.daizhang.module.document.mapper.InputInvoiceMapper;
import com.company.daizhang.module.document.mapper.OutputInvoiceMapper;
import com.company.daizhang.module.salary.entity.SalarySheet;
import com.company.daizhang.module.salary.mapper.SalarySheetMapper;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.tax.service.TaxService;
import com.company.daizhang.module.tax.vo.TaxDeadlineReminderVO;
import com.company.daizhang.module.tax.vo.TaxDeclarationFormItemVO;
import com.company.daizhang.module.tax.vo.TaxDeclarationFormVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 申报服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxServiceImpl implements TaxService {

    private final AccountSetMapper accountSetMapper;
    private final AccountPeriodMapper accountPeriodMapper;
    private final AccountBalanceMapper accountBalanceMapper;
    private final SubjectMapper subjectMapper;
    private final InputInvoiceMapper inputInvoiceMapper;
    private final OutputInvoiceMapper outputInvoiceMapper;
    private final SalarySheetMapper salarySheetMapper;
    private final FixedAssetMapper fixedAssetMapper;

    @Override
    public TaxDeclarationFormVO generateDeclarationForm(Long accountSetId, Integer year, Integer month, String formType) {
        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_SET_NOT_FOUND);
        }

        switch (formType) {
            case "VAT":
                return generateVatForm(accountSet, year, month);
            case "Surcharge":
                return generateSurchargeForm(accountSet, year, month);
            case "IncomeTax":
                return generateIncomeTaxForm(accountSet, year, month);
            case "PersonalTax":
                return generatePersonalTaxForm(accountSet, year, month);
            case "SmallScaleVAT":
                return generateSmallScaleVatForm(accountSet, year, month);
            case "StampTax":
                return generateStampTaxForm(accountSet, year, month);
            case "HouseTax":
                return generateHouseTaxForm(accountSet, year, month);
            case "LandUseTax":
                return generateLandUseTaxForm(accountSet, year, month);
            case "VehicleTax":
                return generateVehicleTaxForm(accountSet, year, month);
            default:
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "不支持的申报表类型: " + formType);
        }
    }

    @Override
    public byte[] exportDeclarationForm(Long accountSetId, Integer year, Integer month, String formType) {
        TaxDeclarationFormVO form = generateDeclarationForm(accountSetId, year, month, formType);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(form.getFormName());

            // 标题样式
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            // 表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // 数据样式
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // 标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(form.getFormName() + " " + year + "年" + month + "月");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

            // 纳税人信息
            Row infoRow1 = sheet.createRow(1);
            infoRow1.createCell(0).setCellValue("纳税人名称:");
            infoRow1.createCell(1).setCellValue(form.getTaxpayerName() != null ? form.getTaxpayerName() : "");
            infoRow1.createCell(2).setCellValue("纳税人识别号:");
            infoRow1.createCell(3).setCellValue(form.getTaxNumber() != null ? form.getTaxNumber() : "");

            // 表头
            Row headerRow = sheet.createRow(2);
            headerRow.createCell(0).setCellValue("行次");
            headerRow.createCell(1).setCellValue("项目");
            headerRow.createCell(2).setCellValue("公式");
            headerRow.createCell(3).setCellValue("金额");
            for (int i = 0; i < 4; i++) {
                headerRow.getCell(i).setCellStyle(headerStyle);
            }

            // 数据行
            int rowNum = 3;
            if (form.getItems() != null) {
                for (TaxDeclarationFormItemVO item : form.getItems()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(item.getRowNo() != null ? item.getRowNo() : 0);
                    row.createCell(1).setCellValue(item.getItemName() != null ? item.getItemName() : "");
                    row.createCell(2).setCellValue(item.getFormula() != null ? item.getFormula() : "");
                    Cell amountCell = row.createCell(3);
                    if (item.getAmount() != null) {
                        amountCell.setCellValue(item.getAmount().doubleValue());
                    } else {
                        amountCell.setCellValue(0);
                    }
                    for (int i = 0; i < 4; i++) {
                        row.getCell(i).setCellStyle(dataStyle);
                    }
                }
            }

            // 合计行
            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(0).setCellValue("");
            Cell totalLabelCell = totalRow.createCell(1);
            totalLabelCell.setCellValue("应纳税额合计");
            totalLabelCell.setCellStyle(headerStyle);
            totalRow.createCell(2).setCellValue("");
            Cell totalAmountCell = totalRow.createCell(3);
            totalAmountCell.setCellValue(form.getTaxAmount() != null ? form.getTaxAmount().doubleValue() : 0);
            totalAmountCell.setCellStyle(headerStyle);
            totalRow.getCell(0).setCellStyle(headerStyle);
            totalRow.getCell(2).setCellStyle(headerStyle);

            // 列宽
            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 30 * 256);
            sheet.setColumnWidth(2, 25 * 256);
            sheet.setColumnWidth(3, 18 * 256);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("导出申报表失败", e);
            throw new BusinessException("导出申报表失败: " + e.getMessage());
        }
    }

    @Override
    public List<TaxDeadlineReminderVO> getDeadlineReminders() {
        List<TaxDeadlineReminderVO> reminders = new ArrayList<>();

        // 查询所有账套
        List<AccountSet> accountSets = accountSetMapper.selectList(null);

        // 当前日期
        LocalDate today = LocalDate.now();
        // 上月所属年月
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        Integer lastYear = lastMonth.getYear();
        Integer lastMonthValue = lastMonth.getMonthValue();

        // 申报截止日为次月15日（即当月15日）
        LocalDate deadline = LocalDate.of(today.getYear(), today.getMonthValue(), 15);
        // 如果当月15日已过，则截止日为下月15日
        if (today.getDayOfMonth() > 15) {
            YearMonth nextMonth = YearMonth.now().plusMonths(1);
            deadline = LocalDate.of(nextMonth.getYear(), nextMonth.getMonthValue(), 15);
        }

        int daysRemaining = (int) java.time.temporal.ChronoUnit.DAYS.between(today, deadline);

        // 各税种
        String[] taxTypes = {"VAT", "Surcharge", "IncomeTax", "PersonalTax", "SmallScaleVAT", "StampTax", "HouseTax", "LandUseTax", "VehicleTax"};

        for (AccountSet accountSet : accountSets) {
            // 查询上月会计期间状态
            LambdaQueryWrapper<AccountPeriod> periodWrapper = new LambdaQueryWrapper<>();
            periodWrapper.eq(AccountPeriod::getAccountSetId, accountSet.getId())
                    .eq(AccountPeriod::getYear, lastYear)
                    .eq(AccountPeriod::getMonth, lastMonthValue);
            AccountPeriod period = accountPeriodMapper.selectOne(periodWrapper);

            // 判断申报状态：期间已结账(status=1)视为已申报
            String status;
            if (period != null && period.getStatus() != null && period.getStatus() == 1) {
                status = "已申报";
            } else if (today.isAfter(deadline)) {
                status = "已逾期";
            } else {
                status = "未申报";
            }

            for (String taxType : taxTypes) {
                TaxDeadlineReminderVO vo = new TaxDeadlineReminderVO();
                vo.setAccountSetId(accountSet.getId());
                vo.setAccountSetName(accountSet.getName());
                vo.setYear(lastYear);
                vo.setMonth(lastMonthValue);
                vo.setTaxType(taxType);
                vo.setDeadline(deadline);
                vo.setDaysRemaining(daysRemaining);
                vo.setStatus(status);
                reminders.add(vo);
            }
        }

        return reminders;
    }

    // ==================== 申报表生成 ====================

    /**
     * 生成增值税申报表
     * 销项税额、进项税额、应纳税额
     */
    private TaxDeclarationFormVO generateVatForm(AccountSet accountSet, Integer year, Integer month) {
        TaxDeclarationFormVO vo = buildBaseForm(accountSet, year, month, "VAT", "增值税纳税申报表");

        // 查询销项发票（销项税额）
        LambdaQueryWrapper<OutputInvoice> outputWrapper = new LambdaQueryWrapper<>();
        outputWrapper.eq(OutputInvoice::getAccountSetId, accountSet.getId());
        List<OutputInvoice> outputInvoices = outputInvoiceMapper.selectList(outputWrapper);

        BigDecimal outputTax = outputInvoices.stream()
                .map(i -> i.getTaxAmount() != null ? i.getTaxAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 查询进项发票（进项税额）
        LambdaQueryWrapper<InputInvoice> inputWrapper = new LambdaQueryWrapper<>();
        inputWrapper.eq(InputInvoice::getAccountSetId, accountSet.getId());
        List<InputInvoice> inputInvoices = inputInvoiceMapper.selectList(inputWrapper);

        BigDecimal inputTax = inputInvoices.stream()
                .map(i -> i.getTaxAmount() != null ? i.getTaxAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 应纳税额 = 销项税额 - 进项税额
        BigDecimal taxAmount = outputTax.subtract(inputTax);
        if (taxAmount.compareTo(BigDecimal.ZERO) < 0) {
            taxAmount = BigDecimal.ZERO;
        }

        List<TaxDeclarationFormItemVO> items = new ArrayList<>();
        items.add(buildItem(1, "销项税额", "按适用税率计税销售额×税率", outputTax));
        items.add(buildItem(2, "进项税额", "本期认证抵扣的进项税额", inputTax));
        items.add(buildItem(3, "应纳税额", "销项税额-进项税额", taxAmount));

        vo.setTaxableIncome(outputTax);
        vo.setTaxRate(new BigDecimal("0.13"));
        vo.setTaxAmount(taxAmount);
        vo.setItems(items);
        return vo;
    }

    /**
     * 生成附加税申报表
     * 城建税、教育附加、地方教育附加
     */
    private TaxDeclarationFormVO generateSurchargeForm(AccountSet accountSet, Integer year, Integer month) {
        TaxDeclarationFormVO vo = buildBaseForm(accountSet, year, month, "Surcharge", "附加税纳税申报表");

        // 计税依据为增值税应纳税额
        TaxDeclarationFormVO vatForm = generateVatForm(accountSet, year, month);
        BigDecimal vatAmount = vatForm.getTaxAmount() != null ? vatForm.getTaxAmount() : BigDecimal.ZERO;

        // 城建税 7%
        BigDecimal cityTax = vatAmount.multiply(new BigDecimal("0.07")).setScale(2, RoundingMode.HALF_UP);
        // 教育附加 3%
        BigDecimal educationSurcharge = vatAmount.multiply(new BigDecimal("0.03")).setScale(2, RoundingMode.HALF_UP);
        // 地方教育附加 2%
        BigDecimal localEducationSurcharge = vatAmount.multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalSurcharge = cityTax.add(educationSurcharge).add(localEducationSurcharge);

        List<TaxDeclarationFormItemVO> items = new ArrayList<>();
        items.add(buildItem(1, "增值税应纳税额", "计税依据", vatAmount));
        items.add(buildItem(2, "城市维护建设税", "增值税×7%", cityTax));
        items.add(buildItem(3, "教育费附加", "增值税×3%", educationSurcharge));
        items.add(buildItem(4, "地方教育附加", "增值税×2%", localEducationSurcharge));
        items.add(buildItem(5, "附加税合计", "城建税+教育附加+地方教育附加", totalSurcharge));

        vo.setTaxableIncome(vatAmount);
        vo.setTaxRate(new BigDecimal("0.12"));
        vo.setTaxAmount(totalSurcharge);
        vo.setItems(items);
        return vo;
    }

    /**
     * 生成企业所得税申报表
     * 营业收入、成本、利润总额、应纳税额
     */
    private TaxDeclarationFormVO generateIncomeTaxForm(AccountSet accountSet, Integer year, Integer month) {
        TaxDeclarationFormVO vo = buildBaseForm(accountSet, year, month, "IncomeTax", "企业所得税纳税申报表");

        // 查询科目余额
        Map<Long, AccountBalance> balanceMap = queryBalanceMap(accountSet.getId(), year, month);
        List<Subject> subjects = querySubjects(accountSet.getId());

        BigDecimal operatingRevenue = sumBySubjectPrefix(subjects, balanceMap, "5001", true, false);
        BigDecimal operatingCost = sumBySubjectPrefix(subjects, balanceMap, "5401", false, true);
        BigDecimal totalProfit = operatingRevenue.subtract(operatingCost);

        // 应纳税额 = 利润总额 × 25%（假设为25%税率）
        BigDecimal taxRate = new BigDecimal("0.25");
        BigDecimal taxAmount = totalProfit.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        if (taxAmount.compareTo(BigDecimal.ZERO) < 0) {
            taxAmount = BigDecimal.ZERO;
        }

        List<TaxDeclarationFormItemVO> items = new ArrayList<>();
        items.add(buildItem(1, "营业收入", "主营业务收入+其他业务收入", operatingRevenue));
        items.add(buildItem(2, "营业成本", "主营业务成本+其他业务成本", operatingCost));
        items.add(buildItem(3, "利润总额", "营业收入-营业成本", totalProfit));
        items.add(buildItem(4, "适用税率", "企业所得税税率25%", taxRate.multiply(new BigDecimal("100"))));
        items.add(buildItem(5, "应纳所得税额", "利润总额×25%", taxAmount));

        vo.setTaxableIncome(totalProfit);
        vo.setTaxRate(taxRate);
        vo.setTaxAmount(taxAmount);
        vo.setItems(items);
        return vo;
    }

    /**
     * 生成个税申报表
     * 员工薪资、个税合计
     */
    private TaxDeclarationFormVO generatePersonalTaxForm(AccountSet accountSet, Integer year, Integer month) {
        TaxDeclarationFormVO vo = buildBaseForm(accountSet, year, month, "PersonalTax", "个人所得税申报表");

        // 查询薪资表
        LambdaQueryWrapper<SalarySheet> salaryWrapper = new LambdaQueryWrapper<>();
        salaryWrapper.eq(SalarySheet::getAccountSetId, accountSet.getId())
                .eq(SalarySheet::getYear, year)
                .eq(SalarySheet::getMonth, month);
        List<SalarySheet> salarySheets = salarySheetMapper.selectList(salaryWrapper);

        BigDecimal totalSalary = salarySheets.stream()
                .map(s -> s.getNetSalary() != null ? s.getNetSalary() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTaxableIncome = salarySheets.stream()
                .map(s -> s.getTaxableIncome() != null ? s.getTaxableIncome() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalIncomeTax = salarySheets.stream()
                .map(s -> s.getIncomeTax() != null ? s.getIncomeTax() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TaxDeclarationFormItemVO> items = new ArrayList<>();
        items.add(buildItem(1, "员工人数", "本月发薪人数", new BigDecimal(salarySheets.size())));
        items.add(buildItem(2, "应发工资合计", "本月员工应发工资", totalSalary));
        items.add(buildItem(3, "应纳税所得额合计", "员工应纳税所得额合计", totalTaxableIncome));
        items.add(buildItem(4, "个人所得税合计", "代扣代缴个税合计", totalIncomeTax));

        vo.setTaxableIncome(totalTaxableIncome);
        vo.setTaxRate(new BigDecimal("0.10"));
        vo.setTaxAmount(totalIncomeTax);
        vo.setItems(items);
        return vo;
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成小规模纳税人增值税申报表
     * 小规模纳税人按征收率（3%）简易计税，不抵扣进项税额
     */
    private TaxDeclarationFormVO generateSmallScaleVatForm(AccountSet accountSet, Integer year, Integer month) {
        TaxDeclarationFormVO vo = buildBaseForm(accountSet, year, month, "SmallScaleVAT", "增值税纳税申报表（小规模纳税人适用）");

        // 查询销项发票（不含税销售额）
        LambdaQueryWrapper<OutputInvoice> outputWrapper = new LambdaQueryWrapper<>();
        outputWrapper.eq(OutputInvoice::getAccountSetId, accountSet.getId());
        List<OutputInvoice> outputInvoices = outputInvoiceMapper.selectList(outputWrapper);

        BigDecimal salesAmount = outputInvoices.stream()
                .map(i -> i.getAmount() != null ? i.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 征收率3%（小规模纳税人法定征收率）
        BigDecimal levyRate = new BigDecimal("0.03");
        BigDecimal taxAmount = salesAmount.multiply(levyRate).setScale(2, RoundingMode.HALF_UP);

        List<TaxDeclarationFormItemVO> items = new ArrayList<>();
        items.add(buildItem(1, "不含税销售额", "全部销售收入（不含税）", salesAmount));
        items.add(buildItem(2, "征收率", "小规模纳税人法定征收率", levyRate.multiply(new BigDecimal("100"))));
        items.add(buildItem(3, "应纳税额", "不含税销售额×3%", taxAmount));

        vo.setTaxableIncome(salesAmount);
        vo.setTaxRate(levyRate);
        vo.setTaxAmount(taxAmount);
        vo.setItems(items);
        return vo;
    }

    /**
     * 生成印花税申报表
     * 印花税按购销合同等计税，常见为购销合同金额×0.03%
     */
    private TaxDeclarationFormVO generateStampTaxForm(AccountSet accountSet, Integer year, Integer month) {
        TaxDeclarationFormVO vo = buildBaseForm(accountSet, year, month, "StampTax", "印花税纳税申报表");

        // 计税依据1：购销合同金额（按营业收入估算）
        Map<Long, AccountBalance> balanceMap = queryBalanceMap(accountSet.getId(), year, month);
        List<Subject> subjects = querySubjects(accountSet.getId());
        BigDecimal operatingRevenue = sumBySubjectPrefix(subjects, balanceMap, "5001", true, false);

        // 购销合同印花税税率0.03%
        BigDecimal purchaseSaleRate = new BigDecimal("0.0003");
        BigDecimal purchaseSaleTax = operatingRevenue.multiply(purchaseSaleRate).setScale(2, RoundingMode.HALF_UP);

        // 计税依据2：资金账簿（实收资本+资本公积，此处简化按0当期无增资）
        BigDecimal capitalAmount = BigDecimal.ZERO;
        BigDecimal capitalRate = new BigDecimal("0.00025");
        BigDecimal capitalTax = capitalAmount.multiply(capitalRate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalStampTax = purchaseSaleTax.add(capitalTax);

        List<TaxDeclarationFormItemVO> items = new ArrayList<>();
        items.add(buildItem(1, "购销合同金额", "按营业收入计", operatingRevenue));
        items.add(buildItem(2, "购销合同印花税", "购销合同金额×0.03%", purchaseSaleTax));
        items.add(buildItem(3, "资金账簿金额", "实收资本+资本公积本期增加额", capitalAmount));
        items.add(buildItem(4, "资金账簿印花税", "资金账簿金额×0.025%", capitalTax));
        items.add(buildItem(5, "印花税合计", "购销合同+资金账簿", totalStampTax));

        vo.setTaxableIncome(operatingRevenue);
        vo.setTaxRate(purchaseSaleRate);
        vo.setTaxAmount(totalStampTax);
        vo.setItems(items);
        return vo;
    }

    /**
     * 生成房产税申报表
     * 房产税分为从价计征（按房产原值×70%×1.2%）和从租计征（按租金×12%）
     * 此处按从价计征，房产原值取自固定资产-房屋类资产
     */
    private TaxDeclarationFormVO generateHouseTaxForm(AccountSet accountSet, Integer year, Integer month) {
        TaxDeclarationFormVO vo = buildBaseForm(accountSet, year, month, "HouseTax", "房产税纳税申报表");

        // 房产原值（按固定资产中"房屋"类资产原值汇总）
        BigDecimal houseValue = BigDecimal.ZERO;
        // 简化处理：查询该账套固定资产，房屋类资产按名称包含"房"或category匹配
        LambdaQueryWrapper<com.company.daizhang.module.asset.entity.FixedAsset> assetWrapper = new LambdaQueryWrapper<>();
        assetWrapper.eq(com.company.daizhang.module.asset.entity.FixedAsset::getAccountSetId, accountSet.getId());
        List<com.company.daizhang.module.asset.entity.FixedAsset> assets = fixedAssetMapper.selectList(assetWrapper);
        for (com.company.daizhang.module.asset.entity.FixedAsset a : assets) {
            String name = a.getAssetName();
            if (name != null && (name.contains("房") || name.contains("楼") || name.contains("厂房"))) {
                BigDecimal v = a.getPurchaseAmount() != null ? a.getPurchaseAmount() : BigDecimal.ZERO;
                houseValue = houseValue.add(v);
            }
        }

        // 从价计征：原值×70%×1.2%
        BigDecimal deductionRate = new BigDecimal("0.70");
        BigDecimal taxRate = new BigDecimal("0.012");
        BigDecimal taxableValue = houseValue.multiply(deductionRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = taxableValue.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);

        List<TaxDeclarationFormItemVO> items = new ArrayList<>();
        items.add(buildItem(1, "房产原值", "房屋类固定资产原值", houseValue));
        items.add(buildItem(2, "计税价值", "房产原值×70%", taxableValue));
        items.add(buildItem(3, "适用税率", "从价计征1.2%", taxRate.multiply(new BigDecimal("100"))));
        items.add(buildItem(4, "应纳房产税", "计税价值×1.2%", taxAmount));

        vo.setTaxableIncome(houseValue);
        vo.setTaxRate(taxRate);
        vo.setTaxAmount(taxAmount);
        vo.setItems(items);
        return vo;
    }

    /**
     * 生成城镇土地使用税申报表
     * 按实际占用土地面积×适用税额计算，税额按城市级别分档（简化取15元/㎡）
     */
    private TaxDeclarationFormVO generateLandUseTaxForm(AccountSet accountSet, Integer year, Integer month) {
        TaxDeclarationFormVO vo = buildBaseForm(accountSet, year, month, "LandUseTax", "城镇土地使用税纳税申报表");

        // 土地面积：从固定资产中名称包含"土地"的资产获取
        // 注：FixedAsset实体无quantity字段，此处按资产原值估算占用面积（简化处理，实际应通过土地台账）
        BigDecimal landArea = BigDecimal.ZERO;
        LambdaQueryWrapper<com.company.daizhang.module.asset.entity.FixedAsset> assetWrapper = new LambdaQueryWrapper<>();
        assetWrapper.eq(com.company.daizhang.module.asset.entity.FixedAsset::getAccountSetId, accountSet.getId());
        List<com.company.daizhang.module.asset.entity.FixedAsset> assets = fixedAssetMapper.selectList(assetWrapper);
        for (com.company.daizhang.module.asset.entity.FixedAsset a : assets) {
            String name = a.getAssetName();
            if (name != null && (name.contains("土地") || name.contains("用地"))) {
                // 简化：按资产原值/10000估算面积（每万元1㎡），实际应从土地台账获取
                BigDecimal v = a.getPurchaseAmount() != null ? a.getPurchaseAmount() : BigDecimal.ZERO;
                landArea = landArea.add(v.divide(new BigDecimal("10000"), 2, RoundingMode.HALF_UP));
            }
        }

        // 适用税额15元/㎡（中等城市标准，实际应分地区配置）
        BigDecimal unitTaxAmount = new BigDecimal("15");
        BigDecimal taxAmount = landArea.multiply(unitTaxAmount).setScale(2, RoundingMode.HALF_UP);

        List<TaxDeclarationFormItemVO> items = new ArrayList<>();
        items.add(buildItem(1, "占用土地面积", "实际占用土地面积（㎡）", landArea));
        items.add(buildItem(2, "适用税额", "每平方米税额", unitTaxAmount));
        items.add(buildItem(3, "年应纳税额", "占用面积×适用税额", taxAmount));
        items.add(buildItem(4, "本期应纳税额", "年税额÷12", taxAmount.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP)));

        BigDecimal monthlyTax = taxAmount.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        vo.setTaxableIncome(landArea);
        vo.setTaxRate(unitTaxAmount);
        vo.setTaxAmount(monthlyTax);
        vo.setItems(items);
        return vo;
    }

    /**
     * 生成车船税申报表
     * 按车辆数量×辆/吨税额计算，此处简化按载客汽车每辆480元
     */
    private TaxDeclarationFormVO generateVehicleTaxForm(AccountSet accountSet, Integer year, Integer month) {
        TaxDeclarationFormVO vo = buildBaseForm(accountSet, year, month, "VehicleTax", "车船税纳税申报表");

        // 车辆数量：从固定资产中名称包含"车"的资产数量
        int vehicleCount = 0;
        LambdaQueryWrapper<com.company.daizhang.module.asset.entity.FixedAsset> assetWrapper = new LambdaQueryWrapper<>();
        assetWrapper.eq(com.company.daizhang.module.asset.entity.FixedAsset::getAccountSetId, accountSet.getId());
        List<com.company.daizhang.module.asset.entity.FixedAsset> assets = fixedAssetMapper.selectList(assetWrapper);
        for (com.company.daizhang.module.asset.entity.FixedAsset a : assets) {
            String name = a.getAssetName();
            if (name != null && (name.contains("车") || name.contains("轿") || name.contains("货"))) {
                vehicleCount++;
            }
        }

        // 载客汽车每辆年税额480元
        BigDecimal annualTaxPerVehicle = new BigDecimal("480");
        BigDecimal annualTax = annualTaxPerVehicle.multiply(new BigDecimal(vehicleCount));
        // 车船税按年缴纳，本期即年税额
        BigDecimal taxAmount = annualTax.setScale(2, RoundingMode.HALF_UP);

        List<TaxDeclarationFormItemVO> items = new ArrayList<>();
        items.add(buildItem(1, "车辆数量", "载客汽车数量（辆）", new BigDecimal(vehicleCount)));
        items.add(buildItem(2, "年单位税额", "载客汽车每辆", annualTaxPerVehicle));
        items.add(buildItem(3, "年应纳税额", "车辆数×单位税额", annualTax));
        items.add(buildItem(4, "本期应纳车船税", "按年缴纳", taxAmount));

        vo.setTaxableIncome(new BigDecimal(vehicleCount));
        vo.setTaxRate(annualTaxPerVehicle);
        vo.setTaxAmount(taxAmount);
        vo.setItems(items);
        return vo;
    }

    /**
     * 构建基础申报表
     */
    private TaxDeclarationFormVO buildBaseForm(AccountSet accountSet, Integer year, Integer month, String formType, String formName) {
        TaxDeclarationFormVO vo = new TaxDeclarationFormVO();
        vo.setFormType(formType);
        vo.setFormName(formName);
        vo.setTaxpayerName(accountSet.getCompanyName());
        vo.setTaxNumber(accountSet.getCode());
        vo.setYear(year);
        vo.setMonth(month);
        return vo;
    }

    /**
     * 构建申报表明细项
     */
    private TaxDeclarationFormItemVO buildItem(Integer rowNo, String itemName, String formula, BigDecimal amount) {
        TaxDeclarationFormItemVO item = new TaxDeclarationFormItemVO();
        item.setRowNo(rowNo);
        item.setItemName(itemName);
        item.setFormula(formula);
        item.setAmount(amount != null ? amount : BigDecimal.ZERO);
        return item;
    }

    /**
     * 查询科目余额Map
     */
    private Map<Long, AccountBalance> queryBalanceMap(Long accountSetId, Integer year, Integer month) {
        LambdaQueryWrapper<AccountBalance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountBalance::getAccountSetId, accountSetId)
                .eq(AccountBalance::getYear, year)
                .eq(AccountBalance::getMonth, month);
        List<AccountBalance> balances = accountBalanceMapper.selectList(wrapper);
        return balances.stream().collect(Collectors.toMap(AccountBalance::getSubjectId, b -> b));
    }

    /**
     * 查询科目列表
     */
    private List<Subject> querySubjects(Long accountSetId) {
        LambdaQueryWrapper<Subject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getStatus, 1);
        return subjectMapper.selectList(wrapper);
    }

    /**
     * 按科目编码前缀汇总金额
     *
     * @param isCredit true=汇总贷方（收入类），false=汇总借方（费用/成本类）
     */
    private BigDecimal sumBySubjectPrefix(List<Subject> subjects, Map<Long, AccountBalance> balanceMap,
                                          String codePrefix, boolean isRevenue, boolean isExpense) {
        BigDecimal total = BigDecimal.ZERO;
        for (Subject subject : subjects) {
            if (subject.getCode() == null || !subject.getCode().startsWith(codePrefix)) {
                continue;
            }
            AccountBalance balance = balanceMap.get(subject.getId());
            if (balance == null) {
                continue;
            }
            if (isRevenue) {
                total = total.add(balance.getPeriodCredit() != null ? balance.getPeriodCredit() : BigDecimal.ZERO);
            } else if (isExpense) {
                total = total.add(balance.getPeriodDebit() != null ? balance.getPeriodDebit() : BigDecimal.ZERO);
            }
        }
        return total;
    }
}
