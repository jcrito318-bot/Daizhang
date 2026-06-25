package com.company.daizhang.module.voucher.service;

import com.company.daizhang.module.voucher.vo.VoucherAttachmentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 凭证附件服务接口
 */
public interface VoucherAttachmentService {

    /**
     * 根据凭证ID查询附件列表
     */
    List<VoucherAttachmentVO> listByVoucherId(Long voucherId);

    /**
     * 上传附件
     */
    VoucherAttachmentVO uploadAttachment(Long voucherId, MultipartFile file);

    /**
     * 删除附件（同时删除文件）
     */
    void deleteAttachment(Long id);
}
