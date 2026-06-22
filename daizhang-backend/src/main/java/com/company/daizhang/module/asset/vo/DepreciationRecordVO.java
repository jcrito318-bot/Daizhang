package com.company.daizhang.module.asset.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 折旧记录视图对象
 */
@Data
public class DepreciationRecordVO {

    private Long id;

    private Long accountSetId;

    private Long assetId;

    private String assetCode;

    private String assetName;

    private Integer year;

    private Integer month;

    private BigDecimal depreciationAmount;

    private BigDecimal accumulatedDepreciation;

    private BigDecimal netValue;

    private Long voucherId;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
