package com.company.daizhang.module.voucher.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 凭证模板实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acc_voucher_template")
public class VoucherTemplate extends BaseEntity {

    /**
     * 账套ID(0-全局)
     */
    private Long accountSetId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板分类
     */
    private String templateCategory;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 凭证字ID
     */
    private Long voucherWordId;

    /**
     * 附件数
     */
    private Integer attachmentCount;

    /**
     * 备注
     */
    private String remark;
}
