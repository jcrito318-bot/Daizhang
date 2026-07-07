package com.company.daizhang.module.report.util;

import com.company.daizhang.module.report.vo.BalanceSheetItem;
import com.company.daizhang.module.report.vo.BalanceSheetVO;
import com.company.daizhang.module.report.vo.CashFlowItemVO;
import com.company.daizhang.module.report.vo.CashFlowStatementVO;
import com.company.daizhang.module.report.vo.DepartmentExpenseItem;
import com.company.daizhang.module.report.vo.DepartmentExpenseReportVO;
import com.company.daizhang.module.report.vo.EquityChangeItem;
import com.company.daizhang.module.report.vo.EquityChangeStatementVO;
import com.company.daizhang.module.report.vo.IncomeStatementItem;
import com.company.daizhang.module.report.vo.IncomeStatementVO;
import com.company.daizhang.module.report.vo.SubjectBalanceRow;
import com.company.daizhang.module.report.vo.SubjectBalanceTableVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 报表Excel导出工具类
 */
@Slf4j
@Component
public class ReportExcelUtil {

    /**
     * 导出资产负债表
     */
    public void exportBalanceSheet(BalanceSheetVO data, Integer year, Integer month, HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("资产负债表");
            
            // 创建标题行样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            // 金额单元格样式（避免 BigDecimal 转 double 精度丢失/无数字格式）
            CellStyle amountStyle = createAmountStyle(workbook);

            // 标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("资产负债表 " + year + "年" + month + "月");
            titleCell.setCellStyle(headerStyle);

            // 表头
            Row headerRow = sheet.createRow(2);
            headerRow.createCell(0).setCellValue("项目");
            headerRow.createCell(1).setCellValue("行次");
            headerRow.createCell(2).setCellValue("年初余额");
            headerRow.createCell(3).setCellValue("期末余额");
            for (int i = 0; i < 4; i++) {
                headerRow.getCell(i).setCellStyle(headerStyle);
            }

            // 资产项目
            int rowNum = 3;
            rowNum = writeBalanceSheetItems(sheet, rowNum, "资产", data.getAssets(), amountStyle);

            // 负债和所有者权益项目
            rowNum++;
            rowNum = writeBalanceSheetItems(sheet, rowNum, "负债", data.getLiabilities(), amountStyle);

            rowNum++;
            rowNum = writeBalanceSheetItems(sheet, rowNum, "所有者权益", data.getEquity(), amountStyle);
            
            // 设置列宽
            sheet.setColumnWidth(0, 20 * 256);
            sheet.setColumnWidth(1, 10 * 256);
            sheet.setColumnWidth(2, 15 * 256);
            sheet.setColumnWidth(3, 15 * 256);
            
            // 输出
            outputExcel(response, workbook, "资产负债表_" + year + "年" + month + "月.xlsx");
        } catch (IOException e) {
            log.error("导出资产负债表失败", e);
            throw new RuntimeException("导出失败", e);
        }
    }

    /**
     * 导出利润表
     */
    public void exportIncomeStatement(IncomeStatementVO data, Integer year, Integer month, HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("利润表");
            
            // 创建标题行样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            // 金额单元格样式（避免 BigDecimal 转 double 精度丢失/无数字格式）
            CellStyle amountStyle = createAmountStyle(workbook);

            // 标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("利润表 " + year + "年" + month + "月");
            titleCell.setCellStyle(headerStyle);

            // 表头
            Row headerRow = sheet.createRow(2);
            headerRow.createCell(0).setCellValue("项目");
            headerRow.createCell(1).setCellValue("行次");
            headerRow.createCell(2).setCellValue("本月数");
            headerRow.createCell(3).setCellValue("本年累计数");
            for (int i = 0; i < 4; i++) {
                headerRow.getCell(i).setCellStyle(headerStyle);
            }

            // 数据行
            int rowNum = 3;
            for (IncomeStatementItem item : data.getItems()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(item.getName());
                row.createCell(1).setCellValue(item.getRowNo());
                setAmountCell(row, 2, item.getCurrentAmount().doubleValue(), amountStyle);
                setAmountCell(row, 3, item.getYearAmount().doubleValue(), amountStyle);
            }
            
            // 设置列宽
            sheet.setColumnWidth(0, 20 * 256);
            sheet.setColumnWidth(1, 10 * 256);
            sheet.setColumnWidth(2, 15 * 256);
            sheet.setColumnWidth(3, 15 * 256);
            
            // 输出
            outputExcel(response, workbook, "利润表_" + year + "年" + month + "月.xlsx");
        } catch (IOException e) {
            log.error("导出利润表失败", e);
            throw new RuntimeException("导出失败", e);
        }
    }

    /**
     * 导出科目余额表
     */
    public void exportSubjectBalanceTable(SubjectBalanceTableVO data, Integer year, Integer month, HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("科目余额表");
            
            // 创建标题行样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            // 金额单元格样式（避免 BigDecimal 转 double 精度丢失/无数字格式）
            CellStyle amountStyle = createAmountStyle(workbook);

            // 标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("科目余额表 " + year + "年" + month + "月");
            titleCell.setCellStyle(headerStyle);

            // 表头
            Row headerRow = sheet.createRow(2);
            headerRow.createCell(0).setCellValue("科目编码");
            headerRow.createCell(1).setCellValue("科目名称");
            headerRow.createCell(2).setCellValue("期初借方");
            headerRow.createCell(3).setCellValue("期初贷方");
            headerRow.createCell(4).setCellValue("本期借方");
            headerRow.createCell(5).setCellValue("本期贷方");
            headerRow.createCell(6).setCellValue("期末借方");
            headerRow.createCell(7).setCellValue("期末贷方");
            for (int i = 0; i < 8; i++) {
                headerRow.getCell(i).setCellStyle(headerStyle);
            }

            // 数据行
            int rowNum = 3;
            for (SubjectBalanceRow row : data.getRows()) {
                Row dataRow = sheet.createRow(rowNum++);
                dataRow.createCell(0).setCellValue(row.getSubjectCode());
                dataRow.createCell(1).setCellValue(row.getSubjectName());
                setAmountCell(dataRow, 2, row.getBeginDebit().doubleValue(), amountStyle);
                setAmountCell(dataRow, 3, row.getBeginCredit().doubleValue(), amountStyle);
                setAmountCell(dataRow, 4, row.getPeriodDebit().doubleValue(), amountStyle);
                setAmountCell(dataRow, 5, row.getPeriodCredit().doubleValue(), amountStyle);
                setAmountCell(dataRow, 6, row.getEndDebit().doubleValue(), amountStyle);
                setAmountCell(dataRow, 7, row.getEndCredit().doubleValue(), amountStyle);
            }
            
            // 设置列宽
            sheet.setColumnWidth(0, 12 * 256);
            sheet.setColumnWidth(1, 20 * 256);
            for (int i = 2; i < 8; i++) {
                sheet.setColumnWidth(i, 15 * 256);
            }
            
            // 输出
            outputExcel(response, workbook, "科目余额表_" + year + "年" + month + "月.xlsx");
        } catch (IOException e) {
            log.error("导出科目余额表失败", e);
            throw new RuntimeException("导出失败", e);
        }
    }

    /**
     * 导出所有者权益变动表
     */
    public void exportEquityChangeStatement(EquityChangeStatementVO data, Integer year, Integer month, HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("所有者权益变动表");

            // 创建标题行样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            // 金额单元格样式（避免 BigDecimal 转 double 精度丢失/无数字格式）
            CellStyle amountStyle = createAmountStyle(workbook);

            // 标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("所有者权益变动表 " + year + "年" + month + "月");
            titleCell.setCellStyle(headerStyle);

            // 表头
            Row headerRow = sheet.createRow(2);
            headerRow.createCell(0).setCellValue("项目");
            headerRow.createCell(1).setCellValue("行次");
            headerRow.createCell(2).setCellValue("年初余额");
            headerRow.createCell(3).setCellValue("本年增加");
            headerRow.createCell(4).setCellValue("本年减少");
            headerRow.createCell(5).setCellValue("期末余额");
            for (int i = 0; i < 6; i++) {
                headerRow.getCell(i).setCellStyle(headerStyle);
            }

            // 数据行
            int rowNum = 3;
            if (data.getItems() != null) {
                for (EquityChangeItem item : data.getItems()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(item.getItemName());
                    row.createCell(1).setCellValue(item.getRowNo());
                    setAmountCell(row, 2, item.getBeginningBalance() != null ? item.getBeginningBalance().doubleValue() : 0, amountStyle);
                    setAmountCell(row, 3, item.getIncreaseAmount() != null ? item.getIncreaseAmount().doubleValue() : 0, amountStyle);
                    setAmountCell(row, 4, item.getDecreaseAmount() != null ? item.getDecreaseAmount().doubleValue() : 0, amountStyle);
                    setAmountCell(row, 5, item.getEndingBalance() != null ? item.getEndingBalance().doubleValue() : 0, amountStyle);
                }
            }

            // 合计行
            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(0).setCellValue("合计");
            setAmountCell(totalRow, 2, data.getTotalBeginningBalance() != null ? data.getTotalBeginningBalance().doubleValue() : 0, amountStyle);
            setAmountCell(totalRow, 3, data.getTotalIncrease() != null ? data.getTotalIncrease().doubleValue() : 0, amountStyle);
            setAmountCell(totalRow, 4, data.getTotalDecrease() != null ? data.getTotalDecrease().doubleValue() : 0, amountStyle);
            setAmountCell(totalRow, 5, data.getTotalEndingBalance() != null ? data.getTotalEndingBalance().doubleValue() : 0, amountStyle);

            // 设置列宽
            sheet.setColumnWidth(0, 25 * 256);
            sheet.setColumnWidth(1, 10 * 256);
            for (int i = 2; i < 6; i++) {
                sheet.setColumnWidth(i, 15 * 256);
            }

            // 输出
            outputExcel(response, workbook, "所有者权益变动表_" + year + "年" + month + "月.xlsx");
        } catch (IOException e) {
            log.error("导出所有者权益变动表失败", e);
            throw new RuntimeException("导出失败", e);
        }
    }

    /**
     * 导出部门费用分析表
     */
    public void exportDepartmentExpense(DepartmentExpenseReportVO data, Integer year, Integer month, HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("部门费用分析表");

            // 创建标题行样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            // 金额单元格样式（避免 BigDecimal 转 double 精度丢失/无数字格式）
            CellStyle amountStyle = createAmountStyle(workbook);

            // 标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("部门费用分析表 " + year + "年" + month + "月");
            titleCell.setCellStyle(headerStyle);

            // 表头
            Row headerRow = sheet.createRow(2);
            headerRow.createCell(0).setCellValue("部门编码");
            headerRow.createCell(1).setCellValue("部门名称");
            headerRow.createCell(2).setCellValue("本期借方发生额");
            headerRow.createCell(3).setCellValue("本年累计");
            headerRow.createCell(4).setCellValue("占比(%)");
            for (int i = 0; i < 5; i++) {
                headerRow.getCell(i).setCellStyle(headerStyle);
            }

            // 数据行
            int rowNum = 3;
            if (data.getItems() != null) {
                for (DepartmentExpenseItem item : data.getItems()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(item.getDepartmentCode() != null ? item.getDepartmentCode() : "");
                    row.createCell(1).setCellValue(item.getDepartmentName() != null ? item.getDepartmentName() : "");
                    setAmountCell(row, 2, item.getPeriodAmount() != null ? item.getPeriodAmount().doubleValue() : 0, amountStyle);
                    setAmountCell(row, 3, item.getYearAmount() != null ? item.getYearAmount().doubleValue() : 0, amountStyle);
                    row.createCell(4).setCellValue(item.getPercentage() != null ? item.getPercentage().doubleValue() : 0);
                }
            }

            // 合计行
            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(0).setCellValue("合计");
            setAmountCell(totalRow, 2, data.getTotalExpense() != null ? data.getTotalExpense().doubleValue() : 0, amountStyle);

            // 设置列宽
            sheet.setColumnWidth(0, 15 * 256);
            sheet.setColumnWidth(1, 20 * 256);
            for (int i = 2; i < 5; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }

            // 输出
            outputExcel(response, workbook, "部门费用分析表_" + year + "年" + month + "月.xlsx");
        } catch (IOException e) {
            log.error("导出部门费用分析表失败", e);
            throw new RuntimeException("导出失败", e);
        }
    }

    private int writeBalanceSheetItems(Sheet sheet, int rowNum, String category, List<BalanceSheetItem> items, CellStyle amountStyle) {
        for (BalanceSheetItem item : items) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getName());
            row.createCell(1).setCellValue(item.getRowNo());
            setAmountCell(row, 2, item.getBeginningBalance().doubleValue(), amountStyle);
            setAmountCell(row, 3, item.getEndingBalance().doubleValue(), amountStyle);
        }
        return rowNum;
    }

    /**
     * 导出现金流量表
     */
    public void exportCashFlowStatement(CashFlowStatementVO data, Integer year, Integer month, HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("现金流量表");

            // 创建标题行样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            // 金额单元格样式（避免 BigDecimal 转 double 精度丢失/无数字格式）
            CellStyle amountStyle = createAmountStyle(workbook);

            // 标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("现金流量表 " + year + "年" + month + "月");
            titleCell.setCellStyle(headerStyle);

            // 表头
            Row headerRow = sheet.createRow(2);
            headerRow.createCell(0).setCellValue("项目");
            headerRow.createCell(1).setCellValue("行次");
            headerRow.createCell(2).setCellValue("金额");
            for (int i = 0; i < 3; i++) {
                headerRow.getCell(i).setCellStyle(headerStyle);
            }

            // 数据行
            int rowNum = 3;
            int rowNo = 1;

            // 一、经营活动产生的现金流量
            rowNum = writeCashFlowSection(sheet, rowNum, "一、经营活动产生的现金流量", headerStyle, true);
            if (data.getItems() != null) {
                for (CashFlowItemVO item : data.getItems()) {
                    if ("经营".equals(item.getCategory()) || "经营活动".equals(item.getCategory())) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue("    " + item.getItemName());
                        row.createCell(1).setCellValue(rowNo++);
                        setAmountCell(row, 2, item.getAmount() != null ? item.getAmount().doubleValue() : 0, amountStyle);
                    }
                }
            }
            rowNum = writeCashFlowSummary(sheet, rowNum, "    现金流入小计", data.getOperatingInflow(), rowNo++, amountStyle);
            rowNum = writeCashFlowSummary(sheet, rowNum, "    现金流出小计", data.getOperatingOutflow(), rowNo++, amountStyle);
            rowNum = writeCashFlowSummary(sheet, rowNum, "    经营活动产生的现金流量净额", data.getOperatingNetFlow(), rowNo++, amountStyle);

            // 二、投资活动产生的现金流量
            rowNum = writeCashFlowSection(sheet, rowNum, "二、投资活动产生的现金流量", headerStyle, true);
            if (data.getItems() != null) {
                for (CashFlowItemVO item : data.getItems()) {
                    if ("投资".equals(item.getCategory()) || "投资活动".equals(item.getCategory())) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue("    " + item.getItemName());
                        row.createCell(1).setCellValue(rowNo++);
                        setAmountCell(row, 2, item.getAmount() != null ? item.getAmount().doubleValue() : 0, amountStyle);
                    }
                }
            }
            rowNum = writeCashFlowSummary(sheet, rowNum, "    现金流入小计", data.getInvestingInflow(), rowNo++, amountStyle);
            rowNum = writeCashFlowSummary(sheet, rowNum, "    现金流出小计", data.getInvestingOutflow(), rowNo++, amountStyle);
            rowNum = writeCashFlowSummary(sheet, rowNum, "    投资活动产生的现金流量净额", data.getInvestingNetFlow(), rowNo++, amountStyle);

            // 三、筹资活动产生的现金流量
            rowNum = writeCashFlowSection(sheet, rowNum, "三、筹资活动产生的现金流量", headerStyle, true);
            if (data.getItems() != null) {
                for (CashFlowItemVO item : data.getItems()) {
                    if ("筹资".equals(item.getCategory()) || "筹资活动".equals(item.getCategory())) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue("    " + item.getItemName());
                        row.createCell(1).setCellValue(rowNo++);
                        setAmountCell(row, 2, item.getAmount() != null ? item.getAmount().doubleValue() : 0, amountStyle);
                    }
                }
            }
            rowNum = writeCashFlowSummary(sheet, rowNum, "    现金流入小计", data.getFinancingInflow(), rowNo++, amountStyle);
            rowNum = writeCashFlowSummary(sheet, rowNum, "    现金流出小计", data.getFinancingOutflow(), rowNo++, amountStyle);
            rowNum = writeCashFlowSummary(sheet, rowNum, "    筹资活动产生的现金流量净额", data.getFinancingNetFlow(), rowNo++, amountStyle);

            // 四、现金及现金等价物净增加额
            rowNum = writeCashFlowSection(sheet, rowNum, "四、现金及现金等价物净增加额", headerStyle, true);
            rowNum = writeCashFlowSummary(sheet, rowNum, "    现金及现金等价物净增加额", data.getNetIncrease(), rowNo++, amountStyle);

            // 设置列宽
            sheet.setColumnWidth(0, 35 * 256);
            sheet.setColumnWidth(1, 10 * 256);
            sheet.setColumnWidth(2, 18 * 256);

            // 输出
            outputExcel(response, workbook, "现金流量表_" + year + "年" + month + "月.xlsx");
        } catch (IOException e) {
            log.error("导出现金流量表失败", e);
            throw new RuntimeException("导出失败", e);
        }
    }

    /**
     * 写入现金流量表分区标题
     */
    private int writeCashFlowSection(Sheet sheet, int rowNum, String sectionName, CellStyle style, boolean bold) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(sectionName);
        if (bold) {
            row.getCell(0).setCellStyle(style);
        }
        return rowNum;
    }

    /**
     * 写入现金流量表小计/净额行
     */
    private int writeCashFlowSummary(Sheet sheet, int rowNum, String name, java.math.BigDecimal amount, int rowNo, CellStyle amountStyle) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(name);
        row.createCell(1).setCellValue(rowNo);
        setAmountCell(row, 2, amount != null ? amount.doubleValue() : 0, amountStyle);
        return rowNum;
    }

    /**
     * 创建金额单元格样式（千分位 + 两位小数）。
     * BigDecimal 直接转 double 写入 Excel 会丢失精度且默认无数字格式，
     * 应用统一数字格式后可保证显示规范并在 Excel 中精确求和。
     */
    private CellStyle createAmountStyle(Workbook workbook) {
        CellStyle amountStyle = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        amountStyle.setDataFormat(format.getFormat("#,##0.00"));
        return amountStyle;
    }

    /**
     * 写入金额单元格并应用数字格式
     */
    private void setAmountCell(Row row, int col, double value, CellStyle amountStyle) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        if (amountStyle != null) {
            cell.setCellStyle(amountStyle);
        }
    }

    private void outputExcel(HttpServletResponse response, Workbook workbook, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName);
        workbook.write(response.getOutputStream());
    }
}
