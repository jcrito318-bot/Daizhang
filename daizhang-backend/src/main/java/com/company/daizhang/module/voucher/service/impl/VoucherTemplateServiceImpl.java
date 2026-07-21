package com.company.daizhang.module.voucher.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.voucher.dto.VoucherTemplateQueryRequest;
import com.company.daizhang.module.voucher.dto.VoucherTemplateRequest;
import com.company.daizhang.module.voucher.entity.VoucherTemplate;
import com.company.daizhang.module.voucher.entity.VoucherTemplateDetail;
import com.company.daizhang.module.voucher.mapper.VoucherTemplateMapper;
import com.company.daizhang.module.voucher.service.VoucherTemplateService;
import com.company.daizhang.module.voucher.vo.VoucherTemplateVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 凭证模板服务实现
 * <p>
 * 模板分录明细以 JSON 形式存储在 {@link VoucherTemplate#getDetailJson()} 字段中,
 * 创建/更新时将 {@link VoucherTemplateRequest#getDetails()} 序列化为 JSON,
 * 查询时将 JSON 反序列化为 {@link VoucherTemplateDetail} 列表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherTemplateServiceImpl extends ServiceImpl<VoucherTemplateMapper, VoucherTemplate> implements VoucherTemplateService {

    private final AccountSetAccessService accountSetAccessService;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<VoucherTemplateVO> pageTemplates(VoucherTemplateQueryRequest request) {
        // IDOR治理:校验当前用户对该账套的访问权(列表查询)
        accountSetAccessService.checkAccess(request.getAccountSetId());

        Page<VoucherTemplate> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<VoucherTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoucherTemplate::getAccountSetId, request.getAccountSetId())
               .like(StrUtil.isNotBlank(request.getTemplateName()), VoucherTemplate::getTemplateName, request.getTemplateName())
               .like(StrUtil.isNotBlank(request.getTemplateCode()), VoucherTemplate::getTemplateCode, request.getTemplateCode())
               .eq(StrUtil.isNotBlank(request.getTemplateCategory()), VoucherTemplate::getTemplateCategory, request.getTemplateCategory())
               .orderByDesc(VoucherTemplate::getCreateTime);

        Page<VoucherTemplate> result = this.page(page, wrapper);

        // 列表查询不返回明细(性能考虑),仅返回基础字段
        List<VoucherTemplateVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public List<VoucherTemplateVO> listTemplates(Long accountSetId) {
        // IDOR治理:校验当前用户对该账套的访问权
        accountSetAccessService.checkAccess(accountSetId);

        LambdaQueryWrapper<VoucherTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoucherTemplate::getAccountSetId, accountSetId)
               .orderByDesc(VoucherTemplate::getCreateTime);

        List<VoucherTemplate> templates = this.list(wrapper);
        return templates.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public VoucherTemplateVO getTemplateById(Long id) {
        VoucherTemplate template = this.getById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.VOUCHER_TEMPLATE_NOT_FOUND);
        }
        // IDOR治理:校验当前用户对该模板所属账套的访问权
        accountSetAccessService.checkAccess(template.getAccountSetId());

        VoucherTemplateVO vo = convertToVO(template);
        vo.setDetails(convertDetailsToVO(parseDetails(template.getDetailJson())));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTemplate(VoucherTemplateRequest request) {
        // IDOR治理:校验当前用户对该账套的所有者权限(写操作)
        accountSetAccessService.checkOwner(request.getAccountSetId());

        // 业务校验:模板明细不能为空
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new BusinessException(ErrorCode.VOUCHER_TEMPLATE_DETAIL_EMPTY);
        }

        // 业务校验:模板编码在账套内唯一
        checkTemplateCodeUnique(request.getAccountSetId(), request.getTemplateCode(), null);

        // 构造实体并保存
        VoucherTemplate template = new VoucherTemplate();
        BeanUtil.copyProperties(request, template, "details");
        template.setDetailJson(serializeDetails(convertRequestToDetails(request.getDetails())));
        this.save(template);

        log.info("创建凭证模板成功,模板ID: {}, 模板编码: {}, 模板名称: {}",
                template.getId(), template.getTemplateCode(), template.getTemplateName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(Long id, VoucherTemplateRequest request) {
        VoucherTemplate template = this.getById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.VOUCHER_TEMPLATE_NOT_FOUND);
        }
        // IDOR治理:校验当前用户对该模板所属账套的所有者权限
        accountSetAccessService.checkOwner(template.getAccountSetId());

        // 业务校验:模板明细不能为空
        if (request.getDetails() == null || request.getDetails().isEmpty()) {
            throw new BusinessException(ErrorCode.VOUCHER_TEMPLATE_DETAIL_EMPTY);
        }

        // 业务校验:模板编码在账套内唯一(排除自身)
        checkTemplateCodeUnique(template.getAccountSetId(), request.getTemplateCode(), id);

        // copyProperties 前保存原 accountSetId, copy 后还原, 不允许通过 update 修改所属账套
        // (否则新 accountSetId 未鉴权, 可将模板偷换到他人账套)
        Long originalAccountSetId = template.getAccountSetId();
        BeanUtil.copyProperties(request, template, "details");
        template.setAccountSetId(originalAccountSetId);
        template.setId(id);
        template.setDetailJson(serializeDetails(convertRequestToDetails(request.getDetails())));
        this.updateById(template);

        log.info("更新凭证模板成功,模板ID: {}, 模板编码: {}, 模板名称: {}",
                id, template.getTemplateCode(), template.getTemplateName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        VoucherTemplate template = this.getById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.VOUCHER_TEMPLATE_NOT_FOUND);
        }
        // IDOR治理:校验当前用户对该模板所属账套的所有者权限
        accountSetAccessService.checkOwner(template.getAccountSetId());

        // 逻辑删除(MyBatis-Plus @TableLogic 自动处理)
        this.removeById(id);

        log.info("删除凭证模板成功,模板ID: {}, 模板编码: {}", id, template.getTemplateCode());
    }

    @Override
    public VoucherTemplateVO applyTemplate(Long id) {
        VoucherTemplate template = this.getById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.VOUCHER_TEMPLATE_NOT_FOUND);
        }
        // IDOR治理:校验当前用户对该模板所属账套的访问权
        accountSetAccessService.checkAccess(template.getAccountSetId());

        VoucherTemplateVO vo = convertToVO(template);
        List<VoucherTemplateDetail> details = parseDetails(template.getDetailJson());
        if (details.isEmpty()) {
            throw new BusinessException(ErrorCode.VOUCHER_TEMPLATE_DETAIL_EMPTY);
        }
        vo.setDetails(convertDetailsToVO(details));

        log.info("应用凭证模板,模板ID: {}, 模板名称: {}", id, template.getTemplateName());
        return vo;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 校验模板编码在账套内唯一
     *
     * @param accountSetId 账套ID
     * @param templateCode 模板编码
     * @param excludeId    排除的模板ID(更新时传入自身ID,创建时传 null)
     */
    private void checkTemplateCodeUnique(Long accountSetId, String templateCode, Long excludeId) {
        LambdaQueryWrapper<VoucherTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoucherTemplate::getAccountSetId, accountSetId)
               .eq(VoucherTemplate::getTemplateCode, templateCode);
        if (excludeId != null) {
            wrapper.ne(VoucherTemplate::getId, excludeId);
        }
        Long count = this.baseMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.VOUCHER_TEMPLATE_CODE_DUPLICATE);
        }
    }

    /**
     * 将请求中的明细列表转换为实体 POJO 列表
     */
    private List<VoucherTemplateDetail> convertRequestToDetails(List<VoucherTemplateRequest.VoucherTemplateDetailRequest> detailRequests) {
        return detailRequests.stream().map(req -> {
            VoucherTemplateDetail detail = new VoucherTemplateDetail();
            detail.setSubjectCode(req.getSubjectCode());
            detail.setSubjectName(req.getSubjectName());
            detail.setDebitAmount(req.getDebitAmount() != null ? req.getDebitAmount() : BigDecimal.ZERO);
            detail.setCreditAmount(req.getCreditAmount() != null ? req.getCreditAmount() : BigDecimal.ZERO);
            detail.setSummary(req.getSummary());
            return detail;
        }).collect(Collectors.toList());
    }

    /**
     * 序列化明细列表为 JSON 字符串
     */
    private String serializeDetails(List<VoucherTemplateDetail> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.error("序列化模板明细失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "模板明细序列化失败");
        }
    }

    /**
     * 反序列化 JSON 字符串为明细列表
     */
    private List<VoucherTemplateDetail> parseDetails(String detailJson) {
        if (StrUtil.isBlank(detailJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(detailJson, new TypeReference<List<VoucherTemplateDetail>>() {});
        } catch (JsonProcessingException e) {
            log.error("反序列化模板明细失败, detailJson: {}", detailJson, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "模板明细反序列化失败");
        }
    }

    /**
     * 模板实体转 VO(不包含明细,明细需单独设置)
     */
    private VoucherTemplateVO convertToVO(VoucherTemplate template) {
        VoucherTemplateVO vo = new VoucherTemplateVO();
        BeanUtil.copyProperties(template, vo);
        return vo;
    }

    /**
     * 明细 POJO 列表转 VO 列表
     */
    private List<VoucherTemplateVO.VoucherTemplateDetailVO> convertDetailsToVO(List<VoucherTemplateDetail> details) {
        return details.stream().map(detail -> {
            VoucherTemplateVO.VoucherTemplateDetailVO vo = new VoucherTemplateVO.VoucherTemplateDetailVO();
            BeanUtil.copyProperties(detail, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
