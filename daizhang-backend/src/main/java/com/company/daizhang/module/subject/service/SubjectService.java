package com.company.daizhang.module.subject.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.module.subject.dto.SubjectCreateRequest;
import com.company.daizhang.module.subject.dto.SubjectUpdateRequest;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.vo.SubjectVO;

import java.util.List;

/**
 * 科目服务接口
 */
public interface SubjectService extends IService<Subject> {
    
    /**
     * 初始化默认科目
     */
    void initDefaultSubjects(Long accountSetId, String accountingStandard);
    
    /**
     * 根据账套ID查询科目列表
     */
    List<SubjectVO> listSubjectsByAccountSetId(Long accountSetId);
    
    /**
     * 根据ID查询科目详情
     */
    SubjectVO getSubjectById(Long id);
    
    /**
     * 创建科目
     */
    void createSubject(SubjectCreateRequest request);
    
    /**
     * 更新科目
     */
    void updateSubject(Long id, SubjectUpdateRequest request);
    
    /**
     * 删除科目
     */
    void deleteSubject(Long id);

    /**
     * 跨账套复制科目体系(P5.0.1)
     * 将源账套的全部科目复制到目标账套。parentId 重新映射。
     * @param sourceAccountSetId 源账套ID
     * @param targetAccountSetId 目标账套ID
     * @return 复制的科目数量
     */
    int copySubjectsFromAccountSet(Long sourceAccountSetId, Long targetAccountSetId);
}
