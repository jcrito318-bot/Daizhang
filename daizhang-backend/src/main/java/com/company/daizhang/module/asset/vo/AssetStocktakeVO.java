package com.company.daizhang.module.asset.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资产盘点单视图对象
 */
@Data
public class AssetStocktakeVO {

    private Long id;

    private Long accountSetId;

    private String stocktakeNo;

    private String stocktakeName;

    private LocalDate stocktakeDate;

    private String stocktakePerson;

    private String scope;

    private String scopeDesc;

    private Integer status;

    private String statusDesc;

    private Integer totalCount;

    private Integer lossCount;

    private Integer gainCount;

    private Integer matchCount;

    private String remark;

    private LocalDateTime createTime;

    private List<AssetStocktakeDetailVO> details;
}
