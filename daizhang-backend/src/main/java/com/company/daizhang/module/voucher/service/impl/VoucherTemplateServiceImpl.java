package com.company.daizhang.module.voucher.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.voucher.dto.VoucherTemplateRequest;
import com.company.daizhang.module.voucher.entity.VoucherTemplate;
import com.company.daizhang.module.voucher.entity.VoucherTemplateDetail;
import com.company.daizhang.module.voucher.mapper.VoucherTemplateDetailMapper;
import com.company.daizhang.module.voucher.mapper.VoucherTemplateMapper;
import com.company.daizhang.module.voucher.service.VoucherTemplateService;
import com.company.daizhang.module.voucher.vo.VoucherTemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 凭证模板服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherTemplateServiceImpl extends ServiceImpl<VoucherTemplateMapper, VoucherTemplate> implements VoucherTemplateService {

    private final VoucherTemplateDetailMapper voucherTemplateDetailMapper;

    @Override
    public PageResult<VoucherTemplateVO> pageTemplates(Long accountSetId, String templateName, int pageNum, int pageSize) {
        Page<VoucherTemplate> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<VoucherTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoucherTemplate::getAccountSetId, accountSetId)
               .like(StrUtil.isNotBlank(templateName), VoucherTemplate::getTemplateName, templateName)
               .orderByDesc(VoucherTemplate::getCreateTime);

        Page<VoucherTemplate> result = this.page(page, wrapper);

        List<VoucherTemplateVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public VoucherTemplateVO getTemplateById(Long id) {
        VoucherTemplate template = this.getById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.VOUCHER_TEMPLATE_NOT_FOUND);
        }

        VoucherTemplateVO vo = convertToVO(template);

        // 查询模板明细
        LambdaQueryWrapper<VoucherTemplateDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(VoucherTemplateDetail::getTemplateId, id)
                     .orderByAsc(VoucherTemplateDetail::getSortOrder);
        List<VoucherTemplateDetail> details = voucherTemplateDetailMapper.selectList(detailWrapper);

        List<VoucherTemplateVO.VoucherTemplateDetailVO> detailVOs = details.stream()
                .map(this::convertDetailToVO)
                .collect(Collectors.toList());
        vo.setDetails(detailVOs);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTemplate(VoucherTemplateRequest request) {
        // 业务校验：模板明细不能为空
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new BusinessException(ErrorCode.VOUCHER_TEMPLATE_DETAIL_EMPTY);
        }

        // 保存模板
        VoucherTemplate template = new VoucherTemplate();
        BeanUtil.copyProperties(request, template);
        this.save(template);

        // 保存模板明细
        saveDetails(template.getId(), request.getDetails());

        log.info("创建凭证模板成功，模板ID: {}, 模板名称: {}", template.getId(), template.getTemplateName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(Long id, VoucherTemplateRequest request) {
        VoucherTemplate template = this.getById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.VOUCHER_TEMPLATE_NOT_FOUND);
        }

        // 业务校验：模板明细不能为空
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new BusinessException(ErrorCode.VOUCHER_TEMPLATE_DETAIL_EMPTY);
        }

        // 更新模板
        BeanUtil.copyProperties(request, template);
        template.setId(id);
        this.updateById(template);

        // 删除旧明细
        LambdaQueryWrapper<VoucherTemplateDetail> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(VoucherTemplateDetail::getTemplateId, id);
        voucherTemplateDetailMapper.delete(deleteWrapper);

        // 保存新明细
        saveDetails(id, request.getDetails());

        log.info("更新凭证模板成功，模板ID: {}, 模板名称: {}", id, template.getTemplateName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        VoucherTemplate template = this.getById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.VOUCHER_TEMPLATE_NOT_FOUND);
        }

        // 删除模板明细
        LambdaQueryWrapper<VoucherTemplateDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(VoucherTemplateDetail::getTemplateId, id);
        voucherTemplateDetailMapper.delete(detailWrapper);

        // 删除模板
        this.removeById(id);

        log.info("删除凭证模板成功，模板ID: {}, 模板名称: {}", id, template.getTemplateName());
    }

    /**
     * 保存模板明细
     */
    private void saveDetails(Long templateId, List<VoucherTemplateRequest.VoucherTemplateDetailRequest> detailRequests) {
        for (int i = 0; i < detailRequests.size(); i++) {
            VoucherTemplateRequest.VoucherTemplateDetailRequest request = detailRequests.get(i);
            VoucherTemplateDetail detail = new VoucherTemplateDetail();
            detail.setTemplateId(templateId);
            detail.setLineNo(request.getLineNo() != null ? request.getLineNo() : i + 1);
            detail.setSummary(request.getSummary());
            detail.setSubjectId(request.getSubjectId());
            detail.setSubjectCode(request.getSubjectCode());
            detail.setSubjectName(request.getSubjectName());
            detail.setDebit(request.getDebit() != null ? request.getDebit() : BigDecimal.ZERO);
            detail.setCredit(request.getCredit() != null ? request.getCredit() : BigDecimal.ZERO);
            detail.setSortOrder(i + 1);
            voucherTemplateDetailMapper.insert(detail);
        }
    }

    /**
     * 模板实体转VO
     */
    private VoucherTemplateVO convertToVO(VoucherTemplate template) {
        VoucherTemplateVO vo = new VoucherTemplateVO();
        BeanUtil.copyProperties(template, vo);
        return vo;
    }

    /**
     * 模板明细实体转VO
     */
    private VoucherTemplateVO.VoucherTemplateDetailVO convertDetailToVO(VoucherTemplateDetail detail) {
        VoucherTemplateVO.VoucherTemplateDetailVO vo = new VoucherTemplateVO.VoucherTemplateDetailVO();
        BeanUtil.copyProperties(detail, vo);
        return vo;
    }
}
