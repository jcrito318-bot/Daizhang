package com.company.daizhang.module.voucher.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 凭证模板实体
 * <p>
 * 代账会计每月重复录入类似凭证(工资/折旧/社保),可将凭证结构保存为模板,
 * 后续通过模板一键调用,避免重复录入。模板分录明细以 JSON 形式存储在
 * {@link #detailJson} 字段中,由 Service 层使用 Jackson 序列化/反序列化
 * 为 {@link VoucherTemplateDetail} 列表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("voucher_template")
public class VoucherTemplate extends BaseEntity {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 模板编码(账套内唯一)
     */
    private String templateCode;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板分类: 工资/折旧/社保/税金/结转/其他
     */
    private String templateCategory;

    /**
     * 凭证摘要
     */
    private String summary;

    /**
     * 分录明细 JSON
     * <p>
     * 存储格式: {@code [{"subjectCode":"6602","subjectName":"管理费用",
     * "debitAmount":1000.00,"creditAmount":0,"summary":"计提工资"}]}
     * <p>
     * 由 Service 层使用 Jackson 与 {@link VoucherTemplateDetail} 列表互转。
     */
    @TableField("detail_json")
    private String detailJson;

    /**
     * 备注
     */
    private String remark;
}
