package com.company.daizhang.module.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 自定义报表项目实体
 */
@Data
@TableName("rpt_custom_report_item")
public class CustomReportItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 报表ID
     */
    private Long reportId;

    /**
     * 行号
     */
    private Integer rowNo;

    /**
     * 项目名称
     */
    private String itemName;

    /**
     * 取数公式(科目编码组合，如"1001+1002")
     */
    private String formula;

    /**
     * 显示方向 0-借方 1-贷方
     */
    private Integer displayDirection;

    /**
     * 是否合计行(0-否 1-是)
     */
    private Integer isTotal;

    /**
     * 父行号
     */
    private Integer parentRowNo;
}
