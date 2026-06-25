package com.company.daizhang.module.subject.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 辅助核算项目视图对象
 */
@Data
public class AuxiliaryItemVO {

    private Long id;

    private Long accountSetId;

    private Long categoryId;

    private String itemCode;

    private String itemName;

    private Long parentId;

    private String parentName;

    private Integer status;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
