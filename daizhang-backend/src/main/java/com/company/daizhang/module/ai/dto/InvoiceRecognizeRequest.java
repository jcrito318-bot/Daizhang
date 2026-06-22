package com.company.daizhang.module.ai.dto;

import lombok.Data;

/**
 * 票据识别请求DTO
 */
@Data
public class InvoiceRecognizeRequest {
    /**
     * 票据图片文件（Base64编码）
     */
    private String imageBase64;

    /**
     * 票据类型（可选）
     * 1-增值税发票 2-普通发票 3-银行回单 4-其他
     */
    private Integer invoiceType;
}
