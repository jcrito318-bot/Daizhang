package com.company.daizhang.module.voucher.service;

import java.io.IOException;
import java.util.List;

/**
 * 凭证打印服务
 * 提供标准会计凭证格式的打印/导出能力：
 * - HTML打印格式（浏览器直接打印）
 * - PDF导出（标准凭证格式，含中文字体）
 * - Excel导出（批量凭证列表）
 */
public interface VoucherPrintService {

    /**
     * 生成单张凭证的标准打印HTML
     */
    String generatePrintHtml(Long voucherId);

    /**
     * 批量生成凭证打印HTML（多张凭证分页）
     */
    String generatePrintHtmlBatch(List<Long> voucherIds);

    /**
     * 导出单张凭证为PDF
     */
    byte[] exportPdf(Long voucherId) throws IOException;

    /**
     * 批量导出凭证为PDF（多页）
     */
    byte[] exportPdfBatch(List<Long> voucherIds) throws IOException;

    /**
     * 批量导出凭证为Excel（标准凭证格式）
     */
    byte[] exportExcel(List<Long> voucherIds) throws IOException;
}
