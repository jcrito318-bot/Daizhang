package com.company.daizhang.module.voucher.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.voucher.dto.AbstractLibraryQueryRequest;
import com.company.daizhang.module.voucher.dto.AbstractLibraryRequest;
import com.company.daizhang.module.voucher.entity.AbstractLibrary;
import com.company.daizhang.module.voucher.mapper.AbstractLibraryMapper;
import com.company.daizhang.module.voucher.service.AbstractLibraryService;
import com.company.daizhang.module.voucher.vo.AbstractLibraryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 常用摘要库服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AbstractLibraryServiceImpl extends ServiceImpl<AbstractLibraryMapper, AbstractLibrary> implements AbstractLibraryService {

    private final AccountSetAccessService accountSetAccessService;

    @Override
    public PageResult<AbstractLibraryVO> pageAbstracts(AbstractLibraryQueryRequest request) {
        // IDOR治理:校验当前用户对该账套的访问权
        accountSetAccessService.checkAccess(request.getAccountSetId());

        Page<AbstractLibrary> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<AbstractLibrary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AbstractLibrary::getAccountSetId, request.getAccountSetId())
               .like(StrUtil.isNotBlank(request.getAbstractText()), AbstractLibrary::getAbstractText, request.getAbstractText())
               .eq(StrUtil.isNotBlank(request.getAbstractCategory()), AbstractLibrary::getAbstractCategory, request.getAbstractCategory())
               .orderByDesc(AbstractLibrary::getUseCount)
               .orderByDesc(AbstractLibrary::getCreateTime);

        Page<AbstractLibrary> result = this.page(page, wrapper);

        List<AbstractLibraryVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public List<AbstractLibraryVO> searchAbstracts(Long accountSetId, String keyword, Integer limit) {
        // IDOR治理:校验当前用户对该账套的访问权
        accountSetAccessService.checkAccess(accountSetId);

        // limit 防御性兜底:避免前端传入过大值拖慢查询
        int safeLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);

        LambdaQueryWrapper<AbstractLibrary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AbstractLibrary::getAccountSetId, accountSetId)
               .like(StrUtil.isNotBlank(keyword), AbstractLibrary::getAbstractText, keyword)
               .orderByDesc(AbstractLibrary::getUseCount)
               .orderByDesc(AbstractLibrary::getCreateTime)
               .last("LIMIT " + safeLimit);

        List<AbstractLibrary> abstracts = this.list(wrapper);
        return abstracts.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAbstract(AbstractLibraryRequest request) {
        // IDOR治理:校验当前用户对该账套的所有者权限(写操作)
        accountSetAccessService.checkOwner(request.getAccountSetId());

        AbstractLibrary entity = new AbstractLibrary();
        BeanUtil.copyProperties(request, entity);
        entity.setUseCount(1);
        this.save(entity);

        log.info("创建常用摘要成功,摘要ID: {}, 摘要文本: {}", entity.getId(), entity.getAbstractText());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementUseCount(Long id) {
        AbstractLibrary entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.ABSTRACT_NOT_FOUND);
        }
        // IDOR治理:校验当前用户对该摘要所属账套的访问权
        accountSetAccessService.checkAccess(entity.getAccountSetId());

        // 使用 UPDATE ... SET use_count = use_count + 1 避免并发覆盖
        LambdaUpdateWrapper<AbstractLibrary> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AbstractLibrary::getId, id)
                     .setSql("use_count = use_count + 1");
        this.update(updateWrapper);

        log.info("常用摘要使用次数 +1,摘要ID: {}, 摘要文本: {}", id, entity.getAbstractText());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAbstract(Long id) {
        AbstractLibrary entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.ABSTRACT_NOT_FOUND);
        }
        // IDOR治理:校验当前用户对该摘要所属账套的所有者权限
        accountSetAccessService.checkOwner(entity.getAccountSetId());

        // 逻辑删除(MyBatis-Plus @TableLogic 自动处理)
        this.removeById(id);

        log.info("删除常用摘要成功,摘要ID: {}, 摘要文本: {}", id, entity.getAbstractText());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 实体转 VO
     */
    private AbstractLibraryVO convertToVO(AbstractLibrary entity) {
        AbstractLibraryVO vo = new AbstractLibraryVO();
        BeanUtil.copyProperties(entity, vo);
        return vo;
    }
}
