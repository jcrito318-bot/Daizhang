package com.company.daizhang.module.subject.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 科目视图对象
 */
@Data
public class SubjectVO {
    
    private Long id;
    
    private Long accountSetId;
    
    private String subjectCode;
    
    private String subjectName;
    
    private String category;
    
    private Long parentId;
    
    private Integer level;
    
    private Integer subjectType;
    
    private Integer balanceDirection;
    
    private Integer auxiliaryAccounting;
    
    private Integer isCash;
    
    private Integer isBank;
    
    private Integer isCurrent;
    
    private Integer status;
    
    private LocalDateTime createTime;
    
    private List<SubjectVO> children;
}
