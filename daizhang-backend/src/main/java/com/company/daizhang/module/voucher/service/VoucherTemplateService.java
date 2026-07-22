package com.company.daizhang.module.voucher.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.voucher.dto.VoucherTemplateQueryRequest;
import com.company.daizhang.module.voucher.dto.VoucherTemplateRequest;
import com.company.daizhang.module.voucher.entity.VoucherTemplate;
import com.company.daizhang.module.voucher.vo.VoucherTemplateVO;

import java.util.List;

/**
 * 凭证模板服务接口
 * <p>
 * 提供凭证模板的增删改查及"应用模板"能力。
 * 模板分录明细以 JSON 形式存储在 {@link VoucherTemplate#getDetailJson()} 字段中,
 * 由 Service 层使用 Jackson 序列化/反序列化为 {@code List<VoucherTemplateDetail>}。
 */
public interface VoucherTemplateService extends IService<VoucherTemplate> {

    /**
     * 分页查询凭证模板
     *
     * @param request 查询条件(包含 accountSetId/模板名称/分类等)
     * @return 分页结果
     */
    PageResult<VoucherTemplateVO> pageTemplates(VoucherTemplateQueryRequest request);

    /**
     * 不分页查询凭证模板(用于下拉选择)
     *
     * @param accountSetId 账套ID
     * @return 模板列表(不含明细,仅返回基础字段)
     */
    List<VoucherTemplateVO> listTemplates(Long accountSetId);

    /**
     * 根据ID查询凭证模板(包含明细)
     *
     * @param id 模板ID
     * @return 模板视图对象
     */
    VoucherTemplateVO getTemplateById(Long id);

    /**
     * 创建凭证模板
     *
     * @param request 模板请求
     */
    void createTemplate(VoucherTemplateRequest request);

    /**
     * 更新凭证模板
     *
     * @param id      模板ID
     * @param request 模板请求
     */
    void updateTemplate(Long id, VoucherTemplateRequest request);

    /**
     * 删除凭证模板(逻辑删除)
     *
     * @param id 模板ID
     */
    void deleteTemplate(Long id);

    /**
     * 应用模板,返回构造好的凭证数据(不直接保存,由前端调 voucherApi.create 保存)
     * <p>
     * 返回的 VO 包含模板基础信息及分录明细(subjectCode/subjectName/debitAmount/creditAmount/summary),
     * 前端根据 subjectCode 在已加载的科目树中解析 subjectId,组装 VoucherCreateRequest 后调用 voucherApi.create。
     *
     * @param id 模板ID
     * @return 模板视图对象(含明细)
     */
    VoucherTemplateVO applyTemplate(Long id);

    /**
     * 跨账套复制凭证模板(P5.0.1)
     * 将源账套的全部凭证模板复制到目标账套。
     * detailJson 用 subjectCode 引用科目,无需 ID 重映射。
     * @param sourceAccountSetId 源账套ID
     * @param targetAccountSetId 目标账套ID
     * @return 复制的模板数量
     */
    int copyTemplatesFromAccountSet(Long sourceAccountSetId, Long targetAccountSetId);
}
