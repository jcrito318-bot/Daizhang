package com.company.daizhang.module.subject.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 辅助核算类别视图对象
 */
@Data
public class AuxiliaryCategoryVO {

    private Long id;

    private Long accountSetId;

    private String categoryCode;

    private String categoryName;

    private String categoryType;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;

    /**
     * 类别下的项目列表
     */
    private List<AuxiliaryItemVO> items;
}
