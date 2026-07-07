package com.company.daizhang.module.voucher.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.voucher.dto.VoucherTemplateRequest;
import com.company.daizhang.module.voucher.entity.VoucherTemplate;
import com.company.daizhang.module.voucher.vo.VoucherTemplateVO;

import java.time.LocalDate;

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

    /**
     * 应用模板生成凭证
     * 根据模板明细构建凭证创建请求并调用凭证服务生成凭证
     *
     * @param templateId   模板ID
     * @param accountSetId 账套ID
     * @param voucherDate  凭证日期
     * @param year         年度
     * @param month        月份
     * @return 新生成的凭证ID
     */
    Long applyTemplate(Long templateId, Long accountSetId, LocalDate voucherDate, Integer year, Integer month);
}
