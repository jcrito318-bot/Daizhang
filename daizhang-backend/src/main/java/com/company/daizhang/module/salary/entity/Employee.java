package com.company.daizhang.module.salary.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import com.company.daizhang.common.annotation.FieldEncrypt;
import com.company.daizhang.common.crypto.annotation.EncryptedField;
import com.company.daizhang.common.crypto.enums.MaskType;
import com.company.daizhang.common.crypto.mybatis.EncryptTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 员工实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sal_employee", autoResultMap = true)
public class Employee extends BaseEntity {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 员工编号
     */
    private String employeeCode;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 基本工资
     */
    private BigDecimal baseSalary;

    /**
     * 部门
     */
    private String department;

    /**
     * 职位
     */
    @TableField("`job_position`")
    private String position;

    /**
     * 身份证号 (P4.1: AES-GCM 加密存储,读库自动解密;对外展示脱敏)
     */
    @TableField(typeHandler = EncryptTypeHandler.class)
    @EncryptedField("员工身份证号")
    @FieldEncrypt(maskType = MaskType.ID_CARD)
    private String idCard;

    /**
     * 联系电话 (P4.1: AES-GCM 加密存储,读库自动解密;对外展示脱敏)
     */
    @TableField(typeHandler = EncryptTypeHandler.class)
    @EncryptedField("员工联系电话")
    @FieldEncrypt(maskType = MaskType.PHONE)
    private String phone;

    /**
     * 开户银行
     */
    private String bankName;

    /**
     * 银行账号 (P4.1: AES-GCM 加密存储,读库自动解密;对外展示脱敏)
     */
    @TableField(typeHandler = EncryptTypeHandler.class)
    @EncryptedField("员工银行账号")
    @FieldEncrypt(maskType = MaskType.BANK_ACCOUNT)
    private String bankAccount;

    /**
     * 入职日期
     */
    private LocalDate entryDate;

    /**
     * 状态：0-离职 1-在职
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
