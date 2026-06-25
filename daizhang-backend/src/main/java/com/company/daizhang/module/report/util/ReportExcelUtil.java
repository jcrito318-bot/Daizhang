package com.company.daizhang.module.report.util;

import com.company.daizhang.module.report.vo.BalanceSheetItem;
import com.company.daizhang.module.report.vo.BalanceSheetVO;
import com.company.daizhang.module.report.vo.CashFlowItemVO;
import com.company.daizhang.module.report.vo.CashFlowStatementVO;
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
            rowNum = writeBalanceSheetItems(sheet, rowNum, "资产", data.getAssets());
            
            // 负债和所有者权益项目
            rowNum++;
            rowNum = writeBalanceSheetItems(sheet, rowNum, "负债", data.getLiabilities());
            
            rowNum++;
            rowNum = writeBalanceSheetItems(sheet, rowNum, "所有者权益", data.getEquity());
            
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
                row.createCell(2).setCellValue(item.getCurrentAmount().doubleValue());
                row.createCell(3).setCellValue(item.getYearAmount().doubleValue());
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
                dataRow.createCell(2).setCellValue(row.getBeginDebit().doubleValue());
                dataRow.createCell(3).setCellValue(row.getBeginCredit().doubleValue());
                dataRow.createCell(4).setCellValue(row.getPeriodDebit().doubleValue());
                dataRow.createCell(5).setCellValue(row.getPeriodCredit().doubleValue());
                dataRow.createCell(6).setCellValue(row.getEndDebit().doubleValue());
                dataRow.createCell(7).setCellValue(row.getEndCredit().doubleValue());
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

    private int writeBalanceSheetItems(Sheet sheet, int rowNum, String category, List<BalanceSheetItem> items) {
        for (BalanceSheetItem item : items) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getName());
            row.createCell(1).setCellValue(item.getRowNo());
            row.createCell(2).setCellValue(item.getBeginningBalance().doubleValue());
            row.createCell(3).setCellValue(item.getEndingBalance().doubleValue());
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
                        row.createCell(2).setCellValue(item.getAmount() != null ? item.getAmount().doubleValue() : 0);
                    }
                }
            }
            rowNum = writeCashFlowSummary(sheet, rowNum, "    现金流入小计", data.getOperatingInflow(), rowNo++);
            rowNum = writeCashFlowSummary(sheet, rowNum, "    现金流出小计", data.getOperatingOutflow(), rowNo++);
            rowNum = writeCashFlowSummary(sheet, rowNum, "    经营活动产生的现金流量净额", data.getOperatingNetFlow(), rowNo++);

            // 二、投资活动产生的现金流量
            rowNum = writeCashFlowSection(sheet, rowNum, "二、投资活动产生的现金流量", headerStyle, true);
            if (data.getItems() != null) {
                for (CashFlowItemVO item : data.getItems()) {
                    if ("投资".equals(item.getCategory()) || "投资活动".equals(item.getCategory())) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue("    " + item.getItemName());
                        row.createCell(1).setCellValue(rowNo++);
                        row.createCell(2).setCellValue(item.getAmount() != null ? item.getAmount().doubleValue() : 0);
                    }
                }
            }
            rowNum = writeCashFlowSummary(sheet, rowNum, "    现金流入小计", data.getInvestingInflow(), rowNo++);
            rowNum = writeCashFlowSummary(sheet, rowNum, "    现金流出小计", data.getInvestingOutflow(), rowNo++);
            rowNum = writeCashFlowSummary(sheet, rowNum, "    投资活动产生的现金流量净额", data.getInvestingNetFlow(), rowNo++);

            // 三、筹资活动产生的现金流量
            rowNum = writeCashFlowSection(sheet, rowNum, "三、筹资活动产生的现金流量", headerStyle, true);
            if (data.getItems() != null) {
                for (CashFlowItemVO item : data.getItems()) {
                    if ("筹资".equals(item.getCategory()) || "筹资活动".equals(item.getCategory())) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue("    " + item.getItemName());
                        row.createCell(1).setCellValue(rowNo++);
                        row.createCell(2).setCellValue(item.getAmount() != null ? item.getAmount().doubleValue() : 0);
                    }
                }
            }
            rowNum = writeCashFlowSummary(sheet, rowNum, "    现金流入小计", data.getFinancingInflow(), rowNo++);
            rowNum = writeCashFlowSummary(sheet, rowNum, "    现金流出小计", data.getFinancingOutflow(), rowNo++);
            rowNum = writeCashFlowSummary(sheet, rowNum, "    筹资活动产生的现金流量净额", data.getFinancingNetFlow(), rowNo++);

            // 四、现金及现金等价物净增加额
            rowNum = writeCashFlowSection(sheet, rowNum, "四、现金及现金等价物净增加额", headerStyle, true);
            rowNum = writeCashFlowSummary(sheet, rowNum, "    现金及现金等价物净增加额", data.getNetIncrease(), rowNo++);

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
    private int writeCashFlowSummary(Sheet sheet, int rowNum, String name, java.math.BigDecimal amount, int rowNo) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(name);
        row.createCell(1).setCellValue(rowNo);
        row.createCell(2).setCellValue(amount != null ? amount.doubleValue() : 0);
        return rowNum;
    }

    private void outputExcel(HttpServletResponse response, Workbook workbook, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName);
        workbook.write(response.getOutputStream());
    }
}
