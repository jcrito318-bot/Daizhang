package com.company.daizhang.module.voucher.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.voucher.dto.VoucherTemplateRequest;
import com.company.daizhang.module.voucher.entity.VoucherTemplate;
import com.company.daizhang.module.voucher.vo.VoucherTemplateVO;

/**
 * 凭证模板服务接口
 */
public interface VoucherTemplateService extends IService<VoucherTemplate> {

    /**
     * 分页查询凭证模板
     */
    PageResult<VoucherTemplateVO> pageTemplates(Long accountSetId, String templateName, int pageNum, int pageSize);

    /**
     * 根据ID查询凭证模板（包含明细）
     */
    VoucherTemplateVO getTemplateById(Long id);

    /**
     * 创建凭证模板
     */
    void createTemplate(VoucherTemplateRequest request);

    /**
     * 更新凭证模板
     */
    void updateTemplate(Long id, VoucherTemplateRequest request);

    /**
     * 删除凭证模板（同时删除明细）
     */
    void deleteTemplate(Long id);
}
