package com.company.daizhang.module.subject.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.subject.dto.AuxiliaryCategoryRequest;
import com.company.daizhang.module.subject.dto.AuxiliaryItemRequest;
import com.company.daizhang.module.subject.vo.AuxiliaryCategoryVO;
import com.company.daizhang.module.subject.vo.AuxiliaryItemVO;

import java.util.List;

/**
 * 辅助核算服务接口
 */
public interface AuxiliaryService {

    // ==================== 类别管理 ====================

    /**
     * 根据账套ID查询辅助核算类别列表
     */
    List<AuxiliaryCategoryVO> listCategories(Long accountSetId);

    /**
     * 创建辅助核算类别
     */
    void createCategory(AuxiliaryCategoryRequest request);

    /**
     * 更新辅助核算类别
     */
    void updateCategory(Long id, AuxiliaryCategoryRequest request);

    /**
     * 删除辅助核算类别
     */
    void deleteCategory(Long id);

    // ==================== 项目管理 ====================

    /**
     * 分页查询辅助核算项目
     */
    PageResult<AuxiliaryItemVO> pageItems(Long accountSetId, Long categoryId, String itemName, int pageNum, int pageSize);

    /**
     * 根据类别ID查询项目列表
     */
    List<AuxiliaryItemVO> listItemsByCategory(Long categoryId);

    /**
     * 创建辅助核算项目
     */
    void createItem(AuxiliaryItemRequest request);

    /**
     * 更新辅助核算项目
     */
    void updateItem(Long id, AuxiliaryItemRequest request);

    /**
     * 删除辅助核算项目
     */
    void deleteItem(Long id);
}
