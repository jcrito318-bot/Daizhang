package com.company.daizhang.module.subject.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 科目实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("acc_subject")
public class Subject extends BaseEntity {
    
    private Long accountSetId;
    
    private String code;
    
    private String name;
    
    private String category;
    
    private Long parentId;
    
    private Integer level;
    
    private Integer balanceDirection;
    
    private Integer isAuxiliary;
    
    private Integer isCash;
    
    private Integer isBank;
    
    private Integer isCurrent;
    
    private Integer status;
}
