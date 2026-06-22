package com.company.daizhang.module.voucher.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 凭证视图对象
 */
@Data
public class VoucherVO {

    private Long id;

    private Long accountSetId;

    private Long voucherWordId;

    private String voucherNo;

    private LocalDate voucherDate;

    private Integer year;

    private Integer month;

    private BigDecimal totalDebit;

    private BigDecimal totalCredit;

    private Integer attachmentCount;

    private Integer status;

    private Long auditBy;

    private LocalDateTime auditTime;

    private Long postBy;

    private LocalDateTime postTime;

    private Integer source;

    private LocalDateTime createTime;

    private Long createBy;

    private String voucherWordName;

    private String createByName;

    private String auditByName;

    private String postByName;

    private List<VoucherDetailVO> details;
}
