package com.company.daizhang.module.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.document.dto.DocumentCreateRequest;
import com.company.daizhang.module.document.dto.DocumentQueryRequest;
import com.company.daizhang.module.document.dto.DocumentUpdateRequest;
import com.company.daizhang.module.document.entity.Document;
import com.company.daizhang.module.document.vo.DocumentLedgerVO;
import com.company.daizhang.module.document.vo.DocumentVO;

/**
 * 票据服务接口
 */
public interface DocumentService extends IService<Document> {

    /**
     * 分页查询票据
     */
    PageResult<DocumentVO> pageDocuments(DocumentQueryRequest request);

    /**
     * 根据ID查询票据
     */
    DocumentVO getDocumentById(Long id);

    /**
     * 创建票据
     */
    void createDocument(DocumentCreateRequest request);

    /**
     * 更新票据
     */
    void updateDocument(Long id, DocumentUpdateRequest request);

    /**
     * 删除票据
     */
    void deleteDocument(Long id);

    /**
     * 关联凭证
     */
    void linkVoucher(Long id, Long voucherId);

    /**
     * 取消关联凭证
     */
    void unlinkVoucher(Long id);

    /**
     * 获取票据台账
     *
     * @param accountSetId 账套ID
     * @param year          年度
     */
    DocumentLedgerVO getDocumentLedger(Long accountSetId, Integer year);

    /**
     * 归档票据
     *
     * @param accountSetId 账套ID
     * @param year          年度
     * @param month         月份
     */
    void archiveDocuments(Long accountSetId, Integer year, Integer month);
}
