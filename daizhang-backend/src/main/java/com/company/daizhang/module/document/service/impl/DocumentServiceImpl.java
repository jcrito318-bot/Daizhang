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
import com.company.daizhang.module.document.vo.DocumentLedgerVO;
import com.company.daizhang.module.document.vo.DocumentMonthlyStatVO;
import com.company.daizhang.module.document.vo.DocumentTypeStatVO;
import com.company.daizhang.module.document.vo.DocumentVO;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 票据服务实现
 */
@Slf4j
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
        // 发票号唯一性校验:同账套下invoiceCode+invoiceNumber不可重复,否则重复入账/重复抵扣
        if (StrUtil.isNotBlank(request.getInvoiceCode()) && StrUtil.isNotBlank(request.getInvoiceNumber())) {
            LambdaQueryWrapper<Document> dupWrapper = new LambdaQueryWrapper<>();
            dupWrapper.eq(Document::getAccountSetId, request.getAccountSetId())
                    .eq(Document::getInvoiceCode, request.getInvoiceCode())
                    .eq(Document::getInvoiceNumber, request.getInvoiceNumber());
            if (this.count(dupWrapper) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "发票号已存在，不可重复录入");
            }
        }

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

        // 已关联凭证或已归档的票据不允许修改
        if (document.getStatus() != null && document.getStatus() >= 1) {
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

        // 已关联凭证或已归档的票据不允许删除
        if (document.getStatus() != null && document.getStatus() >= 1) {
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

        // 已关联凭证的票据不可重复关联,否则原voucherId被覆盖,丢失原关联且无审计痕迹
        if (document.getStatus() != null && document.getStatus() == 1) {
            throw new BusinessException(ErrorCode.DOCUMENT_ALREADY_LINKED);
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

    @Override
    public DocumentLedgerVO getDocumentLedger(Long accountSetId, Integer year) {
        // 查询该账套该年度所有票据
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getAccountSetId, accountSetId)
                .ge(Document::getDocumentDate, startDate)
                .le(Document::getDocumentDate, endDate);
        List<Document> documents = this.list(wrapper);

        DocumentLedgerVO vo = new DocumentLedgerVO();
        vo.setTotalCount(documents.size());

        // 已关联凭证数（status=1 或 status=2）
        int linkedCount = (int) documents.stream()
                .filter(d -> d.getStatus() != null && d.getStatus() >= 1)
                .count();
        vo.setLinkedCount(linkedCount);
        vo.setUnlinkedCount(documents.size() - linkedCount);

        // 金额合计
        BigDecimal totalAmount = documents.stream()
                .map(d -> d.getTotalAmount() != null ? d.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalAmount(totalAmount);

        // 按类型统计
        Map<Integer, List<Document>> typeGroup = documents.stream()
                .filter(d -> d.getDocumentType() != null)
                .collect(Collectors.groupingBy(Document::getDocumentType, TreeMap::new, Collectors.toList()));
        List<DocumentTypeStatVO> typeStats = new ArrayList<>();
        for (Map.Entry<Integer, List<Document>> entry : typeGroup.entrySet()) {
            DocumentTypeStatVO stat = new DocumentTypeStatVO();
            stat.setDocumentType(entry.getKey());
            stat.setTypeName(getDocumentTypeName(entry.getKey()));
            stat.setCount(entry.getValue().size());
            BigDecimal typeAmount = entry.getValue().stream()
                    .map(d -> d.getTotalAmount() != null ? d.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stat.setTotalAmount(typeAmount);
            typeStats.add(stat);
        }
        vo.setTypeStats(typeStats);

        // 按月统计
        Map<Integer, List<Document>> monthGroup = documents.stream()
                .filter(d -> d.getDocumentDate() != null)
                .collect(Collectors.groupingBy(
                        d -> d.getDocumentDate().getMonthValue(),
                        TreeMap::new,
                        Collectors.toList()));
        List<DocumentMonthlyStatVO> monthlyStats = new ArrayList<>();
        for (Map.Entry<Integer, List<Document>> entry : monthGroup.entrySet()) {
            DocumentMonthlyStatVO stat = new DocumentMonthlyStatVO();
            stat.setYear(year);
            stat.setMonth(entry.getKey());
            stat.setCount(entry.getValue().size());
            BigDecimal monthAmount = entry.getValue().stream()
                    .map(d -> d.getTotalAmount() != null ? d.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            stat.setTotalAmount(monthAmount);
            monthlyStats.add(stat);
        }
        vo.setMonthlyStats(monthlyStats);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveDocuments(Long accountSetId, Integer year, Integer month) {
        // 查询该账套该月所有未完成（status != 2）的票据
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getAccountSetId, accountSetId)
                .ge(Document::getDocumentDate, startDate)
                .le(Document::getDocumentDate, endDate)
                .ne(Document::getStatus, 2);
        List<Document> documents = this.list(wrapper);

        int archivedCount = 0;
        for (Document document : documents) {
            // 已关联凭证的票据标记为已完成（归档）
            if (document.getStatus() != null && document.getStatus() == 1) {
                document.setStatus(2);
                this.updateById(document);
                archivedCount++;
            }
        }
        log.info("票据归档完成，账套ID: {}, 年月: {}-{}, 归档数量: {}", accountSetId, year, month, archivedCount);
    }

    /**
     * 获取票据类型名称
     */
    private String getDocumentTypeName(Integer documentType) {
        if (documentType == null) {
            return "其他";
        }
        switch (documentType) {
            case 1:
                return "发票";
            case 2:
                return "银行回单";
            case 3:
                return "费用单据";
            case 4:
                return "其他";
            default:
                return "其他";
        }
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
