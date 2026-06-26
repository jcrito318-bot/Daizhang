package com.company.daizhang.module.voucher.service.impl;

import com.company.daizhang.common.utils.ChineseAmountUtils;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.mapper.AccountSetMapper;
import com.company.daizhang.module.voucher.service.VoucherPrintService;
import com.company.daizhang.module.voucher.service.VoucherService;
import com.company.daizhang.module.voucher.vo.VoucherDetailVO;
import com.company.daizhang.module.voucher.vo.VoucherVO;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * 凭证打印服务实现
 * 采用标准会计凭证格式（参照金蝶/用友/JinBooks），编辑时可简化，打印/导出时使用正式格式。
 *
 * 凭证格式说明：
 *  ┌────────────────────────────────────────────┐
 *  │     单位名称                                │
 *  │         记 字 第 1 号                       │
 *  │  日期：2026年1月15日        附件：1 张       │
 *  │ ┌──────┬──────────┬─────────┬─────────┐    │
 *  │ │ 摘要 │   科目    │  借方   │  贷方   │    │
 *  │ ├──────┼──────────┼─────────┼─────────┤    │
 *  │ │ 提现 │1001库存现金│ 1000.00 │         │    │
 *  │ │ 提现 │1002银行存款│         │ 1000.00 │    │
 *  │ ├──────┴──────────┼─────────┼─────────┤    │
 *  │ │ 合计：壹仟元整  │ 1000.00 │ 1000.00 │    │
 *  │ ├─────────────────┴─────────┴─────────┤    │
 *  │ │ 制单：xxx 审核：xxx 记账：xxx 出纳：xxx │    │
 *  │ └─────────────────────────────────────┘    │
 *  └────────────────────────────────────────────┘
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherPrintServiceImpl implements VoucherPrintService {

    private final VoucherService voucherService;
    private final AccountSetMapper accountSetMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy年M月d日");

    @Override
    public String generatePrintHtml(Long voucherId) {
        VoucherVO voucher = voucherService.getVoucherById(voucherId);
        return buildSingleVoucherHtml(voucher);
    }

    @Override
    public String generatePrintHtmlBatch(List<Long> voucherIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\"/>");
        sb.append("<title>凭证打印</title>");
        sb.append(buildPrintStyle());
        sb.append("</head><body>");
        if (voucherIds == null || voucherIds.isEmpty()) {
            sb.append("<div style=\"text-align:center;padding:60px;color:#999;font-size:16px;\">未选择要打印的凭证</div>");
        } else {
            for (int i = 0; i < voucherIds.size(); i++) {
                VoucherVO voucher = voucherService.getVoucherById(voucherIds.get(i));
                sb.append(buildSingleVoucherBody(voucher));
                if (i < voucherIds.size() - 1) {
                    sb.append("<div class=\"page-break\"></div>");
                }
            }
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    @Override
    public byte[] exportPdf(Long voucherId) throws IOException {
        return exportPdfBatch(Collections.singletonList(voucherId));
    }

    @Override
    public byte[] exportPdfBatch(List<Long> voucherIds) throws IOException {
        if (voucherIds == null || voucherIds.isEmpty()) {
            throw new com.company.daizhang.common.exception.BusinessException(
                    com.company.daizhang.common.exception.ErrorCode.PARAM_ERROR);
        }
        String html = generatePrintHtmlBatch(voucherIds);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            // 使用系统内置中文字体
            useChineseFont(builder);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            log.error("PDF导出失败", e);
            throw new IOException("PDF导出失败: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportExcel(List<Long> voucherIds) throws IOException {
        if (voucherIds == null || voucherIds.isEmpty()) {
            throw new com.company.daizhang.common.exception.BusinessException(
                    com.company.daizhang.common.exception.ErrorCode.PARAM_ERROR);
        }
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("会计凭证");
            // 列宽
            sheet.setColumnWidth(0, 4000);
            sheet.setColumnWidth(1, 8000);
            sheet.setColumnWidth(2, 4000);
            sheet.setColumnWidth(3, 4000);

            int rowIdx = 0;
            if (voucherIds != null) {
                for (Long id : voucherIds) {
                    VoucherVO voucher = voucherService.getVoucherById(id);
                    rowIdx = writeVoucherToSheet(workbook, sheet, voucher, rowIdx);
                    // 凭证间空行
                    rowIdx += 2;
                }
            }
            workbook.write(os);
            return os.toByteArray();
        }
    }

    // ==================== HTML构建 ====================

    private String buildSingleVoucherHtml(VoucherVO voucher) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\"/>");
        sb.append("<title>").append(safe(voucher.getVoucherWordName())).append("字第").append(extractVoucherSeq(voucher.getVoucherNo())).append("号</title>");
        sb.append(buildPrintStyle());
        sb.append("</head><body>");
        sb.append(buildSingleVoucherBody(voucher));
        sb.append("</body></html>");
        return sb.toString();
    }

    private String buildSingleVoucherBody(VoucherVO voucher) {
        String companyName = getCompanyName(voucher.getAccountSetId());
        String wordName = safe(voucher.getVoucherWordName());
        if (wordName.isEmpty()) {
            wordName = "记";
        }
        String seqNo = extractVoucherSeq(voucher.getVoucherNo());
        String dateStr = voucher.getVoucherDate() != null ? voucher.getVoucherDate().format(DATE_FMT) : "";

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"voucher\">");
        // 单位名称
        if (!companyName.isEmpty()) {
            sb.append("<div class=\"company-name\">").append(escape(companyName)).append("</div>");
        }
        // 凭证标题：记 字 第 1 号
        sb.append("<div class=\"voucher-title\">");
        sb.append("<span class=\"word\">").append(escape(wordName)).append("</span>");
        sb.append("<span class=\"word-gap\">字</span>");
        sb.append("<span class=\"seq-label\">第</span>");
        sb.append("<span class=\"seq-no\">").append(escape(seqNo)).append("</span>");
        sb.append("<span class=\"seq-label\">号</span>");
        sb.append("</div>");
        // 日期 + 附件
        sb.append("<div class=\"voucher-header\">");
        sb.append("<span class=\"date\">日期：").append(escape(dateStr)).append("</span>");
        sb.append("<span class=\"attachment\">附件：").append(voucher.getAttachmentCount() != null ? voucher.getAttachmentCount() : 0).append(" 张</span>");
        sb.append("</div>");
        // 表格
        sb.append("<table class=\"voucher-table\">");
        // 表头
        sb.append("<thead><tr>");
        sb.append("<th class=\"col-summary\">摘要</th>");
        sb.append("<th class=\"col-subject\">科目</th>");
        sb.append("<th class=\"col-amount\">借方金额</th>");
        sb.append("<th class=\"col-amount\">贷方金额</th>");
        sb.append("</tr></thead>");
        sb.append("<tbody>");
        // 明细行
        List<VoucherDetailVO> details = voucher.getDetails();
        int minRows = 5; // 最少5行，不足补空行
        int totalRows = Math.max(details != null ? details.size() : 0, minRows);
        if (details != null) {
            for (VoucherDetailVO d : details) {
                sb.append(buildDetailRow(d));
            }
        }
        // 补空行
        for (int i = details != null ? details.size() : 0; i < totalRows; i++) {
            sb.append("<tr class=\"empty-row\">");
            sb.append("<td>&#160;</td><td>&#160;</td><td>&#160;</td><td>&#160;</td>");
            sb.append("</tr>");
        }
        // 合计行
        BigDecimal totalDebit = voucher.getTotalDebit() != null ? voucher.getTotalDebit() : BigDecimal.ZERO;
        BigDecimal totalCredit = voucher.getTotalCredit() != null ? voucher.getTotalCredit() : BigDecimal.ZERO;
        String chineseTotal = ChineseAmountUtils.toChinese(totalDebit.max(totalCredit));
        sb.append("<tr class=\"total-row\">");
        sb.append("<td colspan=\"2\" class=\"total-text\">合计（大写）：<span class=\"chinese-amount\">").append(escape(chineseTotal)).append("</span></td>");
        sb.append("<td class=\"amount-cell\">").append(formatAmount(totalDebit)).append("</td>");
        sb.append("<td class=\"amount-cell\">").append(formatAmount(totalCredit)).append("</td>");
        sb.append("</tr>");
        sb.append("</tbody></table>");
        // 签字栏
        sb.append("<div class=\"sign-row\">");
        sb.append("<span>制单：<u>").append(escape(safe(voucher.getCreateByName()))).append("</u></span>");
        sb.append("<span>审核：<u>").append(escape(safe(voucher.getAuditByName()))).append("</u></span>");
        sb.append("<span>记账：<u>").append(escape(safe(voucher.getPostByName()))).append("</u></span>");
        sb.append("<span>出纳：<u>&#160;&#160;&#160;&#160;&#160;</u></span>");
        sb.append("</div>");
        sb.append("</div>");
        return sb.toString();
    }

    private String buildDetailRow(VoucherDetailVO d) {
        StringBuilder sb = new StringBuilder();
        sb.append("<tr>");
        sb.append("<td class=\"summary-cell\">").append(escape(safe(d.getSummary()))).append("</td>");
        String subject = (d.getSubjectCode() != null ? d.getSubjectCode() + " " : "") + safe(d.getSubjectName());
        sb.append("<td class=\"subject-cell\">").append(escape(subject)).append("</td>");
        sb.append("<td class=\"amount-cell\">").append(d.getDebit() != null && d.getDebit().compareTo(BigDecimal.ZERO) != 0 ? formatAmount(d.getDebit()) : "").append("</td>");
        sb.append("<td class=\"amount-cell\">").append(d.getCredit() != null && d.getCredit().compareTo(BigDecimal.ZERO) != 0 ? formatAmount(d.getCredit()) : "").append("</td>");
        sb.append("</tr>");
        return sb.toString();
    }

    private String buildPrintStyle() {
        return "<style>"
                + "@page { size: A4; margin: 15mm 12mm; }"
                + "body { font-family: \"SimSun\", \"宋体\", \"Noto Sans CJK SC\", serif; font-size: 12px; color: #000; }"
                + ".voucher { width: 100%; margin: 0 auto; }"
                + ".company-name { text-align: center; font-size: 16px; font-weight: bold; margin-bottom: 8px; }"
                + ".voucher-title { text-align: center; font-size: 18px; font-weight: bold; margin-bottom: 6px; }"
                + ".voucher-title .word { letter-spacing: 4px; }"
                + ".voucher-title .word-gap, .voucher-title .seq-label { margin: 0 4px; }"
                + ".voucher-title .seq-no { display: inline-block; min-width: 40px; text-align: center; border-bottom: 1px solid #000; padding: 0 8px; }"
                + ".voucher-header { display: flex; justify-content: space-between; margin-bottom: 4px; font-size: 12px; }"
                + ".voucher-table { width: 100%; border-collapse: collapse; }"
                + ".voucher-table th, .voucher-table td { border: 1px solid #000; padding: 4px 6px; height: 24px; }"
                + ".voucher-table th { background: #f0f0f0; text-align: center; font-weight: bold; }"
                + ".col-summary { width: 28%; }"
                + ".col-subject { width: 32%; }"
                + ".col-amount { width: 20%; }"
                + ".summary-cell { text-align: left; }"
                + ".subject-cell { text-align: left; }"
                + ".amount-cell { text-align: right; font-family: \"Courier New\", monospace; }"
                + ".total-row td { font-weight: bold; border-top: 2px double #000; }"
                + ".total-text { text-align: left; }"
                + ".chinese-amount { font-weight: bold; }"
                + ".empty-row td { height: 24px; }"
                + ".sign-row { display: flex; justify-content: space-between; margin-top: 16px; font-size: 12px; }"
                + ".sign-row span { flex: 1; text-align: center; }"
                + ".sign-row u { display: inline-block; min-width: 60px; text-align: center; }"
                + ".page-break { page-break-after: always; }"
                + "@media print { body { -webkit-print-color-adjust: exact; } .page-break { page-break-after: always; } }"
                + "</style>";
    }

    // ==================== Excel构建 ====================

    private int writeVoucherToSheet(Workbook workbook, Sheet sheet, VoucherVO voucher, int startRow) {
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        CellStyle amountStyle = workbook.createCellStyle();
        amountStyle.setAlignment(HorizontalAlignment.RIGHT);
        DataFormat df = workbook.createDataFormat();
        amountStyle.setDataFormat(df.getFormat("#,##0.00"));
        amountStyle.setBorderTop(BorderStyle.THIN);
        amountStyle.setBorderBottom(BorderStyle.THIN);
        amountStyle.setBorderLeft(BorderStyle.THIN);
        amountStyle.setBorderRight(BorderStyle.THIN);

        CellStyle textStyle = workbook.createCellStyle();
        textStyle.setBorderTop(BorderStyle.THIN);
        textStyle.setBorderBottom(BorderStyle.THIN);
        textStyle.setBorderLeft(BorderStyle.THIN);
        textStyle.setBorderRight(BorderStyle.THIN);

        CellStyle totalStyle = workbook.createCellStyle();
        totalStyle.setFont(headerFont);
        totalStyle.setBorderTop(BorderStyle.DOUBLE);
        totalStyle.setBorderBottom(BorderStyle.THIN);
        totalStyle.setBorderLeft(BorderStyle.THIN);
        totalStyle.setBorderRight(BorderStyle.THIN);

        int row = startRow;
        String companyName = getCompanyName(voucher.getAccountSetId());
        // 单位名称
        if (!companyName.isEmpty()) {
            Row r = sheet.createRow(row++);
            Cell c = r.createCell(0);
            c.setCellValue(companyName);
            c.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 3));
        }
        // 凭证标题
        String wordName = safe(voucher.getVoucherWordName());
        if (wordName.isEmpty()) {
            wordName = "记";
        }
        Row titleRow = sheet.createRow(row++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(wordName + " 字 第 " + extractVoucherSeq(voucher.getVoucherNo()) + " 号");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 3));

        // 日期+附件
        Row dateRow = sheet.createRow(row++);
        String dateStr = voucher.getVoucherDate() != null ? voucher.getVoucherDate().format(DATE_FMT) : "";
        dateRow.createCell(0).setCellValue("日期：" + dateStr);
        dateRow.createCell(2).setCellValue("附件：");
        dateRow.createCell(3).setCellValue((voucher.getAttachmentCount() != null ? voucher.getAttachmentCount() : 0) + " 张");

        // 表头
        Row headerRow = sheet.createRow(row++);
        String[] headers = {"摘要", "科目", "借方金额", "贷方金额"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }
        // 明细
        List<VoucherDetailVO> details = voucher.getDetails();
        int minRows = 5;
        int detailCount = details != null ? details.size() : 0;
        int totalRows = Math.max(detailCount, minRows);
        if (details != null) {
            for (VoucherDetailVO d : details) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(safe(d.getSummary()));
                String subject = (d.getSubjectCode() != null ? d.getSubjectCode() + " " : "") + safe(d.getSubjectName());
                r.createCell(1).setCellValue(subject);
                if (d.getDebit() != null && d.getDebit().compareTo(BigDecimal.ZERO) != 0) {
                    Cell dc = r.createCell(2);
                    // 先 setScale(2) 保证两位小数精度，再转 double 写入（Excel金额格式 #,##0.00 会正确显示）
                    dc.setCellValue(d.getDebit().setScale(2, RoundingMode.HALF_UP).doubleValue());
                    dc.setCellStyle(amountStyle);
                } else {
                    r.createCell(2).setCellStyle(amountStyle);
                }
                if (d.getCredit() != null && d.getCredit().compareTo(BigDecimal.ZERO) != 0) {
                    Cell cc = r.createCell(3);
                    cc.setCellValue(d.getCredit().setScale(2, RoundingMode.HALF_UP).doubleValue());
                    cc.setCellStyle(amountStyle);
                } else {
                    r.createCell(3).setCellStyle(amountStyle);
                }
                r.getCell(0).setCellStyle(textStyle);
                r.getCell(1).setCellStyle(textStyle);
            }
        }
        // 空行
        for (int i = detailCount; i < totalRows; i++) {
            Row r = sheet.createRow(row++);
            for (int j = 0; j < 4; j++) {
                r.createCell(j).setCellStyle(textStyle);
            }
        }
        // 合计行
        BigDecimal totalDebit = voucher.getTotalDebit() != null ? voucher.getTotalDebit() : BigDecimal.ZERO;
        BigDecimal totalCredit = voucher.getTotalCredit() != null ? voucher.getTotalCredit() : BigDecimal.ZERO;
        String chineseTotal = ChineseAmountUtils.toChinese(totalDebit.max(totalCredit));
        Row totalRow = sheet.createRow(row++);
        Cell totalText = totalRow.createCell(0);
        totalText.setCellValue("合计（大写）：" + chineseTotal);
        totalText.setCellStyle(totalStyle);
        sheet.addMergedRegion(new CellRangeAddress(row - 1, row - 1, 0, 1));
        Cell td = totalRow.createCell(2);
        td.setCellValue(totalDebit.setScale(2, RoundingMode.HALF_UP).doubleValue());
        td.setCellStyle(totalStyle);
        Cell tc = totalRow.createCell(3);
        tc.setCellValue(totalCredit.setScale(2, RoundingMode.HALF_UP).doubleValue());
        tc.setCellStyle(totalStyle);

        // 签字行
        Row signRow = sheet.createRow(row++);
        signRow.createCell(0).setCellValue("制单：" + safe(voucher.getCreateByName()));
        signRow.createCell(1).setCellValue("审核：" + safe(voucher.getAuditByName()));
        signRow.createCell(2).setCellValue("记账：" + safe(voucher.getPostByName()));
        signRow.createCell(3).setCellValue("出纳：");

        return row;
    }

    // ==================== 辅助方法 ====================

    private String getCompanyName(Long accountSetId) {
        if (accountSetId == null) {
            return "";
        }
        AccountSet accountSet = accountSetMapper.selectById(accountSetId);
        if (accountSet == null) {
            return "";
        }
        return safe(accountSet.getCompanyName());
    }

    /**
     * 从凭证号中提取序号
     * 凭证号格式可能为 "2026-01-001" 或 "001" 或 "1"
     */
    private String extractVoucherSeq(String voucherNo) {
        if (voucherNo == null || voucherNo.isEmpty()) {
            return "";
        }
        // 取最后一段
        String[] parts = voucherNo.split("[-/]");
        return parts[parts.length - 1];
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * 配置中文字体（PDF渲染）
     * OpenHTMLtopdf 的 PDFBox 后端仅支持 TTF 格式，不支持 OTF
     * 优先使用 AR PL UMing（文鼎宋体，TTF格式）渲染中文
     */
    private void useChineseFont(PdfRendererBuilder builder) {
        // 注意：PDFBox 后端不支持 OTF，必须用 TTF
        String[] fontPaths = {
                "/usr/share/fonts/truetype/arphic/uming.ttc",
                "/usr/share/fonts/truetype/arphic/ukai.ttc",
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
                "/usr/share/fonts/truetype/simsun.ttc"
        };
        for (String path : fontPaths) {
            java.io.File f = new java.io.File(path);
            if (f.exists()) {
                builder.useFont(f, "SimSun");
                builder.useFont(f, "宋体");
                return;
            }
        }
    }
}
