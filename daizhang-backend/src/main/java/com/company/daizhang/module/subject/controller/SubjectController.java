package com.company.daizhang.module.subject.controller;

import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.subject.dto.SubjectCreateRequest;
import com.company.daizhang.module.subject.dto.SubjectUpdateRequest;
import com.company.daizhang.module.subject.service.SubjectService;
import com.company.daizhang.module.subject.vo.SubjectVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 科目管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/subject")
@RequiredArgsConstructor
@Tag(name = "科目管理", description = "会计科目管理接口")
public class SubjectController {

    private final SubjectService subjectService;

    /**
     * 查询科目树
     */
    @GetMapping("/tree")
    @Operation(summary = "查询科目树", description = "根据账套ID查询科目树形结构")
    public Result<List<SubjectVO>> getTree(@RequestParam Long accountSetId) {
        List<SubjectVO> subjects = subjectService.listSubjectsByAccountSetId(accountSetId);
        
        // 构建树形结构
        List<SubjectVO> tree = buildTree(subjects, 0L);
        
        return Result.success(tree);
    }

    /**
     * 根据ID查询科目
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询科目详情", description = "根据ID查询科目详情")
    public Result<SubjectVO> getById(@PathVariable Long id) {
        SubjectVO subject = subjectService.getSubjectById(id);
        return Result.success(subject);
    }

    /**
     * 创建科目
     */
    @PostMapping
    @Operation(summary = "创建科目", description = "创建新的会计科目")
    public Result<SubjectVO> create(@Valid @RequestBody SubjectCreateRequest request) {
        subjectService.createSubject(request);
        return Result.success(null);
    }

    /**
     * 更新科目
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新科目", description = "更新会计科目信息")
    public Result<SubjectVO> update(@PathVariable Long id, @Valid @RequestBody SubjectUpdateRequest request) {
        subjectService.updateSubject(id, request);
        return Result.success(null);
    }

    /**
     * 删除科目
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除科目", description = "删除会计科目")
    public Result<Void> delete(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return Result.success(null);
    }

    /**
     * 初始化默认科目
     */
    @PostMapping("/init")
    @Operation(summary = "初始化默认科目", description = "为账套初始化默认科目模板")
    public Result<Void> initDefaultSubjects(@RequestParam Long accountSetId, 
                                            @RequestParam(defaultValue = "小企业会计准则") String accountingStandard) {
        subjectService.initDefaultSubjects(accountSetId, accountingStandard);
        return Result.success(null);
    }

    /**
     * 构建树形结构
     */
    private List<SubjectVO> buildTree(List<SubjectVO> subjects, Long parentId) {
        return subjects.stream()
                .filter(subject -> parentId.equals(subject.getParentId()))
                .peek(subject -> {
                    List<SubjectVO> children = buildTree(subjects, subject.getId());
                    subject.setChildren(children);
                })
                .collect(java.util.stream.Collectors.toList());
    }
}
