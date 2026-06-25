package com.company.daizhang.module.salary.util;

import com.company.daizhang.module.salary.entity.Employee;
import com.company.daizhang.module.salary.entity.SalarySheet;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 薪资导出工具类（银行代发文件、工资条）
 */
@Slf4j
@Component
public class SalaryExportUtil {

    /**
     * 导出银行代发文件（Excel格式）
     * 包含汇总信息+明细
     */
    public void exportBankDisbursementFile(List<SalarySheet> salarySheets,
                                           Map<Long, Employee> employeeMap,
                                           Integer year, Integer month,
                                           HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("银行代发工资");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

            // 标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("银行代发工资表 " + year + "年" + month + "月");
            titleCell.setCellStyle(headerStyle);

            // 汇总行
            BigDecimal totalAmount = salarySheets.stream()
                    .map(SalarySheet::getNetSalary)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Row summaryRow = sheet.createRow(1);
            summaryRow.createCell(0).setCellValue("总人数:");
            summaryRow.createCell(1).setCellValue(salarySheets.size());
            summaryRow.createCell(2).setCellValue("实发合计:");
            Cell amountCell = summaryRow.createCell(3);
            amountCell.setCellValue(totalAmount.doubleValue());
            amountCell.setCellStyle(amountStyle);
            summaryRow.createCell(4).setCellValue("元");

            // 表头
            Row headerRow = sheet.createRow(3);
            String[] headers = {"序号", "姓名", "身份证号", "开户银行", "银行账号", "实发工资(元)", "备注"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 明细行
            int rowNum = 4;
            int seq = 1;
            for (SalarySheet sheetItem : salarySheets) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(seq++);
                row.createCell(1).setCellValue(sheetItem.getEmployeeName() != null ? sheetItem.getEmployeeName() : "");

                Employee emp = employeeMap.get(sheetItem.getEmployeeId());
                row.createCell(2).setCellValue(emp != null && emp.getIdCard() != null ? emp.getIdCard() : "");
                row.createCell(3).setCellValue(emp != null && emp.getBankName() != null ? emp.getBankName() : "");
                row.createCell(4).setCellValue(emp != null && emp.getBankAccount() != null ? emp.getBankAccount() : "");

                BigDecimal net = sheetItem.getNetSalary() != null ? sheetItem.getNetSalary() : BigDecimal.ZERO;
                Cell amtCell = row.createCell(5);
                amtCell.setCellValue(net.doubleValue());
                amtCell.setCellStyle(amountStyle);
                row.createCell(6).setCellValue(sheetItem.getRemark() != null ? sheetItem.getRemark() : "");
            }

            // 列宽
            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 15 * 256);
            sheet.setColumnWidth(2, 25 * 256);
            sheet.setColumnWidth(3, 25 * 256);
            sheet.setColumnWidth(4, 25 * 256);
            sheet.setColumnWidth(5, 15 * 256);
            sheet.setColumnWidth(6, 25 * 256);

            outputExcel(response, workbook, "银行代发工资_" + year + "年" + month + "月.xlsx");
        } catch (IOException e) {
            log.error("导出银行代发文件失败", e);
            throw new RuntimeException("导出失败", e);
        }
    }

    /**
     * 导出工资条（Excel格式，表格式）
     * 每员工一行，包含所有薪资项目
     */
    public void exportPayslips(List<SalarySheet> salarySheets,
                               Map<Long, Employee> employeeMap,
                               Integer year, Integer month,
                               HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("工资条");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

            // 标题
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("工资条 " + year + "年" + month + "月");
            titleCell.setCellStyle(headerStyle);

            // 表头
            Row headerRow = sheet.createRow(2);
            String[] headers = {"序号", "员工编号", "姓名", "部门", "基本工资", "津贴补贴", "奖金",
                    "扣款", "社保", "公积金", "应纳税所得额", "个人所得税", "实发工资"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 明细
            int rowNum = 3;
            int seq = 1;
            BigDecimal totalNet = BigDecimal.ZERO;
            for (SalarySheet s : salarySheets) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(seq++);
                Employee emp = employeeMap.get(s.getEmployeeId());
                row.createCell(1).setCellValue(emp != null && emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "");
                row.createCell(2).setCellValue(s.getEmployeeName() != null ? s.getEmployeeName() : "");
                row.createCell(3).setCellValue(emp != null && emp.getDepartment() != null ? emp.getDepartment() : "");

                setAmountCell(row, 4, s.getBaseSalary(), amountStyle);
                setAmountCell(row, 5, s.getAllowance(), amountStyle);
                setAmountCell(row, 6, s.getBonus(), amountStyle);
                setAmountCell(row, 7, s.getDeduction(), amountStyle);
                setAmountCell(row, 8, s.getSocialSecurity(), amountStyle);
                setAmountCell(row, 9, s.getHousingFund(), amountStyle);
                setAmountCell(row, 10, s.getTaxableIncome(), amountStyle);
                setAmountCell(row, 11, s.getIncomeTax(), amountStyle);
                setAmountCell(row, 12, s.getNetSalary(), amountStyle);

                if (s.getNetSalary() != null) {
                    totalNet = totalNet.add(s.getNetSalary());
                }
            }

            // 合计行
            Row totalRow = sheet.createRow(rowNum);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("合计");
            totalLabel.setCellStyle(headerStyle);
            Cell totalAmtCell = totalRow.createCell(12);
            totalAmtCell.setCellValue(totalNet.doubleValue());
            totalAmtCell.setCellStyle(amountStyle);

            // 列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 15 * 256);
            }

            outputExcel(response, workbook, "工资条_" + year + "年" + month + "月.xlsx");
        } catch (IOException e) {
            log.error("导出工资条失败", e);
            throw new RuntimeException("导出失败", e);
        }
    }

    private void setAmountCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value.doubleValue() : 0d);
        cell.setCellStyle(style);
    }

    private void outputExcel(HttpServletResponse response, Workbook workbook, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName);
        workbook.write(response.getOutputStream());
    }
}
