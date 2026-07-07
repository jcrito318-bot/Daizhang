package com.company.daizhang.module.subject.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.subject.dto.AuxiliaryCategoryRequest;
import com.company.daizhang.module.subject.dto.AuxiliaryItemRequest;
import com.company.daizhang.module.subject.entity.AuxiliaryCategory;
import com.company.daizhang.module.subject.entity.AuxiliaryItem;
import com.company.daizhang.module.subject.mapper.AuxiliaryCategoryMapper;
import com.company.daizhang.module.subject.mapper.AuxiliaryItemMapper;
import com.company.daizhang.module.subject.service.AuxiliaryService;
import com.company.daizhang.module.subject.vo.AuxiliaryCategoryVO;
import com.company.daizhang.module.subject.vo.AuxiliaryItemVO;
import com.company.daizhang.module.voucher.entity.VoucherDetail;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 辅助核算服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuxiliaryServiceImpl implements AuxiliaryService {

    private final AuxiliaryCategoryMapper auxiliaryCategoryMapper;
    private final AuxiliaryItemMapper auxiliaryItemMapper;
    private final VoucherDetailMapper voucherDetailMapper;
    private final AccountSetAccessService accountSetAccessService;

    // ==================== 类别管理 ====================

    @Override
    public List<AuxiliaryCategoryVO> listCategories(Long accountSetId) {
        LambdaQueryWrapper<AuxiliaryCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuxiliaryCategory::getAccountSetId, accountSetId)
               .orderByAsc(AuxiliaryCategory::getCategoryCode);

        List<AuxiliaryCategory> categories = auxiliaryCategoryMapper.selectList(wrapper);

        // 查询该账套下所有项目，按类别分组
        LambdaQueryWrapper<AuxiliaryItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(AuxiliaryItem::getAccountSetId, accountSetId)
                   .orderByAsc(AuxiliaryItem::getItemCode);
        List<AuxiliaryItem> allItems = auxiliaryItemMapper.selectList(itemWrapper);
        Map<Long, List<AuxiliaryItem>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(AuxiliaryItem::getCategoryId));

        return categories.stream()
                .map(category -> {
                    AuxiliaryCategoryVO vo = convertCategoryToVO(category);
                    List<AuxiliaryItem> items = itemMap.getOrDefault(category.getId(), Collections.emptyList());
                    vo.setItems(items.stream()
                            .map(this::convertItemToVO)
                            .collect(Collectors.toList()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCategory(AuxiliaryCategoryRequest request) {
        // IDOR治理:校验当前用户对该账套的所有者权限
        accountSetAccessService.checkOwner(request.getAccountSetId());

        // 检查类别编码是否已存在
        LambdaQueryWrapper<AuxiliaryCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuxiliaryCategory::getAccountSetId, request.getAccountSetId())
               .eq(AuxiliaryCategory::getCategoryCode, request.getCategoryCode());
        Long count = auxiliaryCategoryMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "类别编码已存在");
        }

        AuxiliaryCategory category = new AuxiliaryCategory();
        BeanUtil.copyProperties(request, category);
        auxiliaryCategoryMapper.insert(category);

        log.info("创建辅助核算类别成功，类别编码: {}, 类别名称: {}", category.getCategoryCode(), category.getCategoryName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long id, AuxiliaryCategoryRequest request) {
        AuxiliaryCategory category = auxiliaryCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "辅助核算类别不存在");
        }
        // IDOR治理:校验当前用户对该类别所属账套的所有者权限
        accountSetAccessService.checkOwner(category.getAccountSetId());

        // 检查类别编码是否与其他记录重复
        LambdaQueryWrapper<AuxiliaryCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuxiliaryCategory::getAccountSetId, category.getAccountSetId())
               .eq(AuxiliaryCategory::getCategoryCode, request.getCategoryCode())
               .ne(AuxiliaryCategory::getId, id);
        Long count = auxiliaryCategoryMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "类别编码已存在");
        }

        BeanUtil.copyProperties(request, category, "id", "accountSetId");
        category.setId(id);
        auxiliaryCategoryMapper.updateById(category);

        log.info("更新辅助核算类别成功，类别ID: {}, 类别编码: {}", id, category.getCategoryCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        AuxiliaryCategory category = auxiliaryCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "辅助核算类别不存在");
        }
        // IDOR治理:校验当前用户对该类别所属账套的所有者权限
        accountSetAccessService.checkOwner(category.getAccountSetId());

        // 业务校验：检查类别下是否存在项目
        LambdaQueryWrapper<AuxiliaryItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(AuxiliaryItem::getCategoryId, id);
        Long itemCount = auxiliaryItemMapper.selectCount(itemWrapper);
        if (itemCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该类别下存在项目，无法删除");
        }

        auxiliaryCategoryMapper.deleteById(id);

        log.info("删除辅助核算类别成功，类别ID: {}, 类别编码: {}", id, category.getCategoryCode());
    }

    // ==================== 项目管理 ====================

    @Override
    public PageResult<AuxiliaryItemVO> pageItems(Long accountSetId, Long categoryId, String itemName, int pageNum, int pageSize) {
        Page<AuxiliaryItem> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<AuxiliaryItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(accountSetId != null, AuxiliaryItem::getAccountSetId, accountSetId)
               .eq(categoryId != null, AuxiliaryItem::getCategoryId, categoryId)
               .like(StrUtil.isNotBlank(itemName), AuxiliaryItem::getItemName, itemName)
               .orderByAsc(AuxiliaryItem::getItemCode);

        IPage<AuxiliaryItem> result = auxiliaryItemMapper.selectPage(page, wrapper);

        List<AuxiliaryItemVO> voList = result.getRecords().stream()
                .map(this::convertItemToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public List<AuxiliaryItemVO> listItemsByCategory(Long categoryId) {
        LambdaQueryWrapper<AuxiliaryItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuxiliaryItem::getCategoryId, categoryId)
               .orderByAsc(AuxiliaryItem::getItemCode);

        List<AuxiliaryItem> items = auxiliaryItemMapper.selectList(wrapper);
        return items.stream()
                .map(this::convertItemToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createItem(AuxiliaryItemRequest request) {
        // 校验类别是否存在
        AuxiliaryCategory category = auxiliaryCategoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "辅助核算类别不存在");
        }
        // IDOR治理:校验当前用户对该类别所属账套的所有者权限
        accountSetAccessService.checkOwner(category.getAccountSetId());
        // 校验项目账套与类别账套一致，防止跨账套越权
        if (!category.getAccountSetId().equals(request.getAccountSetId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "项目账套与类别账套不一致");
        }

        // 检查项目编码是否已存在
        LambdaQueryWrapper<AuxiliaryItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuxiliaryItem::getAccountSetId, request.getAccountSetId())
               .eq(AuxiliaryItem::getCategoryId, request.getCategoryId())
               .eq(AuxiliaryItem::getItemCode, request.getItemCode());
        Long count = auxiliaryItemMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "项目编码已存在");
        }

        AuxiliaryItem item = new AuxiliaryItem();
        BeanUtil.copyProperties(request, item);
        if (item.getParentId() == null) {
            item.setParentId(0L);
        }
        if (item.getStatus() == null) {
            item.setStatus(1);
        }
        auxiliaryItemMapper.insert(item);

        log.info("创建辅助核算项目成功，项目编码: {}, 项目名称: {}", item.getItemCode(), item.getItemName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(Long id, AuxiliaryItemRequest request) {
        AuxiliaryItem item = auxiliaryItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "辅助核算项目不存在");
        }
        // IDOR治理:校验当前用户对该项目所属账套的所有者权限
        accountSetAccessService.checkOwner(item.getAccountSetId());

        // 校验类别是否存在
        AuxiliaryCategory category = auxiliaryCategoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "辅助核算类别不存在");
        }

        // 检查项目编码是否与其他记录重复
        LambdaQueryWrapper<AuxiliaryItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuxiliaryItem::getAccountSetId, item.getAccountSetId())
               .eq(AuxiliaryItem::getCategoryId, request.getCategoryId())
               .eq(AuxiliaryItem::getItemCode, request.getItemCode())
               .ne(AuxiliaryItem::getId, id);
        Long count = auxiliaryItemMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "项目编码已存在");
        }

        BeanUtil.copyProperties(request, item, "id", "accountSetId");
        item.setId(id);
        if (item.getParentId() == null) {
            item.setParentId(0L);
        }
        auxiliaryItemMapper.updateById(item);

        log.info("更新辅助核算项目成功，项目ID: {}, 项目编码: {}", id, item.getItemCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long id) {
        AuxiliaryItem item = auxiliaryItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "辅助核算项目不存在");
        }
        // IDOR治理:校验当前用户对该项目所属账套的所有者权限
        accountSetAccessService.checkOwner(item.getAccountSetId());

        // 业务校验：检查是否存在下级项目
        LambdaQueryWrapper<AuxiliaryItem> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(AuxiliaryItem::getParentId, id);
        Long childCount = auxiliaryItemMapper.selectCount(childWrapper);
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该项目存在下级项目，无法删除");
        }

        // 业务校验：检查是否被凭证明细引用，被引用不允许删除
        Long voucherRefCount = voucherDetailMapper.selectCount(
                new LambdaQueryWrapper<VoucherDetail>().eq(VoucherDetail::getAuxiliaryId, id));
        if (voucherRefCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该辅助核算项目已被凭证引用，无法删除");
        }

        auxiliaryItemMapper.deleteById(id);

        log.info("删除辅助核算项目成功，项目ID: {}, 项目编码: {}", id, item.getItemCode());
    }

    // ==================== 转换方法 ====================

    private AuxiliaryCategoryVO convertCategoryToVO(AuxiliaryCategory category) {
        AuxiliaryCategoryVO vo = new AuxiliaryCategoryVO();
        BeanUtil.copyProperties(category, vo);
        vo.setItems(new ArrayList<>());
        return vo;
    }

    private AuxiliaryItemVO convertItemToVO(AuxiliaryItem item) {
        AuxiliaryItemVO vo = new AuxiliaryItemVO();
        BeanUtil.copyProperties(item, vo);
        // 查询父级名称
        if (item.getParentId() != null && item.getParentId() > 0) {
            AuxiliaryItem parent = auxiliaryItemMapper.selectById(item.getParentId());
            if (parent != null) {
                vo.setParentName(parent.getItemName());
            }
        }
        return vo;
    }
}
