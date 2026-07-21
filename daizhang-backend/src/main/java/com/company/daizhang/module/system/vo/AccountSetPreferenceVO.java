package com.company.daizhang.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账套偏好视图对象
 */
@Data
public class AccountSetPreferenceVO {

    /** 账套ID */
    private Long accountSetId;

    /** 账套名称 */
    private String accountSetName;

    /** 是否收藏 0-否 1-是 */
    private Integer isFavorite;

    /** 最近访问时间 */
    private LocalDateTime lastAccessedAt;

    /** 访问次数 */
    private Integer accessCount;

    /** 排序 */
    private Integer sortOrder;
}
