package com.company.daizhang.module.voucher.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 凭证附件视图对象
 */
@Data
public class VoucherAttachmentVO {

    private Long id;

    private Long voucherId;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private String fileType;

    private Long createBy;

    private LocalDateTime createTime;
}
