package com.company.daizhang.module.accountset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户账套关联实体(数据级授权:IDOR越权治理基础)
 * 一个用户可关联多个账套,一个账套可被多个用户关联(多对多)
 */
@Data
@TableName("sys_user_account_set")
public class UserAccountSet {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long accountSetId;

    /** 关系类型 OWNER-所有者 ACCOUNTANT-记账员 VIEWER-查看者 */
    private String roleType;
}
