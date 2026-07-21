package com.company.daizhang.module.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户账套偏好实体(收藏/最近访问/排序)
 * <p>
 * 不继承 {@link com.company.daizhang.common.BaseEntity}: 该表无 deleted/version/create_by/update_by 字段,
 * 仅保留 create_time/update_time(由 MetaObjectHandlerConfig 自动填充,更新时由 Service 显式 set)。
 */
@Data
@TableName("user_account_set_preference")
public class UserAccountSetPreference implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 账套ID */
    private Long accountSetId;

    /** 是否收藏 0-否 1-是 */
    private Integer isFavorite;

    /** 最近访问时间 */
    private LocalDateTime lastAccessedAt;

    /** 访问次数 */
    private Integer accessCount;

    /** 排序 */
    private Integer sortOrder;

    /** 创建时间(自动填充) */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间(插入时自动填充,更新时由 Service 显式设置) */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
