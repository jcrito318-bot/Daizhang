package com.company.daizhang.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.module.system.dto.IndustryTemplateRequest;
import com.company.daizhang.module.system.entity.IndustryTemplate;
import com.company.daizhang.module.system.vo.IndustryTemplateVO;

import java.util.List;

/**
 * 行业模板服务接口
 */
public interface IndustryTemplateService extends IService<IndustryTemplate> {

    /**
     * 查询行业模板列表
     */
    List<IndustryTemplateVO> listTemplates(String industryType);

    /**
     * 根据ID查询行业模板
     */
    IndustryTemplateVO getTemplateById(Long id);

    /**
     * 创建行业模板
     */
    void createTemplate(IndustryTemplateRequest request);

    /**
     * 更新行业模板
     */
    void updateTemplate(Long id, IndustryTemplateRequest request);

    /**
     * 删除行业模板
     */
    void deleteTemplate(Long id);
}
