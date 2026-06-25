package com.company.daizhang.module.voucher.service;

import com.company.daizhang.common.vo.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 凭证Excel导入服务接口
 */
public interface VoucherImportService {

    /**
     * 从Excel批量导入凭证
     *
     * @param accountSetId 账套ID
     * @param file        Excel文件
     * @return 导入结果
     */
    ImportResultVO importVouchers(Long accountSetId, MultipartFile file);

    /**
     * 下载凭证导入模板
     *
     * @return 模板Excel字节数组
     */
    byte[] downloadTemplate();
}
