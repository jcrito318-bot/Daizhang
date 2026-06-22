package com.company.daizhang.module.subject.dto;

import lombok.Data;

/**
 * 科目更新请求
 */
@Data
public class SubjectUpdateRequest {
    
    private String subjectName;
    
    private String category;
    
    private Integer subjectType;
    
    private Integer balanceDirection;
    
    private Integer auxiliaryAccounting;
    
    private Integer isCash;
    
    private Integer isBank;
    
    private Integer isCurrent;
    
    private Integer status;
}
