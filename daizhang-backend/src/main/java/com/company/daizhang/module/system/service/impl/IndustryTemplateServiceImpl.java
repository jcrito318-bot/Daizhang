package com.company.daizhang.module.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.system.dto.IndustryTemplateRequest;
import com.company.daizhang.module.system.entity.IndustryTemplate;
import com.company.daizhang.module.system.mapper.IndustryTemplateMapper;
import com.company.daizhang.module.system.service.IndustryTemplateService;
import com.company.daizhang.module.system.vo.IndustryTemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 行业模板服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndustryTemplateServiceImpl extends ServiceImpl<IndustryTemplateMapper, IndustryTemplate> implements IndustryTemplateService {

    @Override
    public List<IndustryTemplateVO> listTemplates(String industryType) {
        LambdaQueryWrapper<IndustryTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(industryType), IndustryTemplate::getIndustryType, industryType)
               .orderByDesc(IndustryTemplate::getCreateTime);
        List<IndustryTemplate> list = this.list(wrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public IndustryTemplateVO getTemplateById(Long id) {
        IndustryTemplate template = this.getById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "行业模板不存在");
        }
        return convertToVO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTemplate(IndustryTemplateRequest request) {
        // 检查编码是否已存在
        LambdaQueryWrapper<IndustryTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IndustryTemplate::getTemplateCode, request.getTemplateCode());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板编码已存在");
        }

        IndustryTemplate template = new IndustryTemplate();
        BeanUtil.copyProperties(request, template);
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        this.save(template);

        log.info("创建行业模板成功，模板ID: {}, 模板名称: {}", template.getId(), template.getTemplateName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(Long id, IndustryTemplateRequest request) {
        IndustryTemplate template = this.getById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "行业模板不存在");
        }

        // 检查编码是否与其他记录冲突
        LambdaQueryWrapper<IndustryTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IndustryTemplate::getTemplateCode, request.getTemplateCode())
               .ne(IndustryTemplate::getId, id);
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模板编码已存在");
        }

        BeanUtil.copyProperties(request, template);
        template.setId(id);
        this.updateById(template);

        log.info("更新行业模板成功，模板ID: {}, 模板名称: {}", id, template.getTemplateName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        IndustryTemplate template = this.getById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "行业模板不存在");
        }

        this.removeById(id);

        log.info("删除行业模板成功，模板ID: {}, 模板名称: {}", id, template.getTemplateName());
    }

    /**
     * 实体转VO
     */
    private IndustryTemplateVO convertToVO(IndustryTemplate template) {
        IndustryTemplateVO vo = new IndustryTemplateVO();
        BeanUtil.copyProperties(template, vo);
        return vo;
    }
}
