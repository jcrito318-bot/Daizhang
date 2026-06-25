package com.company.daizhang.module.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 员工绩效实体
 */
@Data
@TableName("sys_employee_performance")
public class EmployeePerformance implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 员工ID
     */
    private Long userId;

    /**
     * 员工姓名
     */
    private String userName;

    /**
     * 年度
     */
    @TableField("`year`")
    private Integer year;

    /**
     * 月份
     */
    @TableField("`month`")
    private Integer month;

    /**
     * 凭证录入数
     */
    private Integer voucherCount;

    /**
     * 审核数
     */
    private Integer auditCount;

    /**
     * 任务完成数
     */
    private Integer taskCompleteCount;

    /**
     * 服务客户数
     */
    private Integer customerCount;

    /**
     * 绩效分数
     */
    private BigDecimal performanceScore;

    /**
     * 备注
     */
    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
