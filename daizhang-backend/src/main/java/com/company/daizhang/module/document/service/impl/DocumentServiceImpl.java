package com.company.daizhang.module.document.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.document.dto.DocumentCreateRequest;
import com.company.daizhang.module.document.dto.DocumentQueryRequest;
import com.company.daizhang.module.document.dto.DocumentUpdateRequest;
import com.company.daizhang.module.document.entity.Document;
import com.company.daizhang.module.document.mapper.DocumentMapper;
import com.company.daizhang.module.document.service.DocumentService;
import com.company.daizhang.module.document.vo.DocumentVO;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 票据服务实现
 */
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {

    private final VoucherMapper voucherMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<DocumentVO> pageDocuments(DocumentQueryRequest request) {
        Page<Document> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getAccountSetId, request.getAccountSetId())
               .eq(request.getDocumentType() != null, Document::getDocumentType, request.getDocumentType())
               .eq(request.getStatus() != null, Document::getStatus, request.getStatus())
               .like(StrUtil.isNotBlank(request.getDocumentNo()), Document::getDocumentNo, request.getDocumentNo())
               .like(StrUtil.isNotBlank(request.getSellerName()), Document::getSellerName, request.getSellerName())
               .like(StrUtil.isNotBlank(request.getBuyerName()), Document::getBuyerName, request.getBuyerName())
               .ge(request.getStartDate() != null, Document::getDocumentDate, request.getStartDate())
               .le(request.getEndDate() != null, Document::getDocumentDate, request.getEndDate())
               .eq(request.getVoucherId() != null, Document::getVoucherId, request.getVoucherId())
               .orderByDesc(Document::getCreateTime);

        Page<Document> result = this.page(page, wrapper);

        List<DocumentVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public DocumentVO getDocumentById(Long id) {
        Document document = this.getById(id);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
        return convertToVO(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDocument(DocumentCreateRequest request) {
        Document document = new Document();
        BeanUtil.copyProperties(request, document);

        // 如果没有传入票据编号，自动生成
        if (StrUtil.isBlank(document.getDocumentNo())) {
            document.setDocumentNo(generateDocumentNo());
        }

        document.setStatus(0);
        this.save(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDocument(Long id, DocumentUpdateRequest request) {
        Document document = this.getById(id);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        // 已关联凭证的票据不允许修改
        if (document.getStatus() != null && document.getStatus() == 1) {
            throw new BusinessException(ErrorCode.DOCUMENT_ALREADY_LINKED);
        }

        BeanUtil.copyProperties(request, document, "id", "accountSetId", "status", "voucherId");
        this.updateById(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long id) {
        Document document = this.getById(id);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        // 已关联凭证的票据不允许删除
        if (document.getStatus() != null && document.getStatus() == 1) {
            throw new BusinessException(ErrorCode.DOCUMENT_ALREADY_LINKED);
        }

        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkVoucher(Long id, Long voucherId) {
        Document document = this.getById(id);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.VOUCHER_NOT_FOUND);
        }

        document.setVoucherId(voucherId);
        document.setStatus(1);
        this.updateById(document);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlinkVoucher(Long id) {
        Document document = this.getById(id);
        if (document == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        if (document.getStatus() == null || document.getStatus() != 1) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_LINKED);
        }

        document.setVoucherId(null);
        document.setStatus(0);
        this.updateById(document);
    }

    /**
     * 生成票据编号：格式 DOC-yyyyMMdd-序号
     */
    private String generateDocumentNo() {
        String dateStr = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "DOC-" + dateStr + "-";

        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Document::getDocumentNo, prefix)
               .orderByDesc(Document::getDocumentNo);
        List<Document> documents = this.list(wrapper);

        int sequence = 1;
        if (!documents.isEmpty()) {
            String lastNo = documents.get(0).getDocumentNo();
            if (StrUtil.isNotBlank(lastNo) && lastNo.length() > prefix.length()) {
                try {
                    sequence = Integer.parseInt(lastNo.substring(prefix.length())) + 1;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return prefix + String.format("%03d", sequence);
    }

    /**
     * 票据实体转VO
     */
    private DocumentVO convertToVO(Document document) {
        DocumentVO vo = new DocumentVO();
        BeanUtil.copyProperties(document, vo);

        // 查询创建人名称
        if (document.getCreateBy() != null) {
            SysUser createUser = sysUserMapper.selectById(document.getCreateBy());
            if (createUser != null) {
                vo.setCreateByName(createUser.getRealName() != null ? createUser.getRealName() : createUser.getUsername());
            }
        }

        return vo;
    }
}
