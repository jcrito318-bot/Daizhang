package com.company.daizhang.module.asset.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产变动记录视图对象
 */
@Data
public class AssetChangeRecordVO {

    private Long id;

    private Long accountSetId;

    private Long assetId;

    private String changeType;

    private LocalDate changeDate;

    private BigDecimal changeAmount;

    private String fromDepartment;

    private String toDepartment;

    private Long voucherId;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;
}
