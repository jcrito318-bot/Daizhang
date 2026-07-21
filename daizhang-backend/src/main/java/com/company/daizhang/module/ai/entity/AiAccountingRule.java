package com.company.daizhang.module.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 记账规则实体
 * <p>
 * 关键词 -> 借贷科目映射规则,优先于 AI 调用:
 * 当业务描述命中关键词时直接返回规则中的借贷科目,不调用 GLM,既省钱又稳定。
 * account_set_id = 0 表示全局规则(系统内置,只读);其他值表示账套级规则(可由用户维护或自动学习生成)。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_accounting_rule")
public class AiAccountingRule extends BaseEntity {

    /**
     * 账套ID, 0=全局规则
     */
    private Long accountSetId;

    /**
     * 业务关键词,如 差旅费/办公费/工资
     */
    private String keyword;

    /**
     * 借方科目编码
     */
    private String debitSubjectCode;

    /**
     * 借方科目名称
     */
    private String debitSubjectName;

    /**
     * 贷方科目编码
     */
    private String creditSubjectCode;

    /**
     * 贷方科目名称
     */
    private String creditSubjectName;

    /**
     * 建议摘要
     */
    private String voucherSummary;

    /**
     * 优先级,数字越小越优先
     */
    private Integer priority;

    /**
     * 命中次数(用于排序优化)
     */
    private Integer hitCount;

    /**
     * 是否启用 0-禁用 1-启用
     */
    private Integer enabled;
}
