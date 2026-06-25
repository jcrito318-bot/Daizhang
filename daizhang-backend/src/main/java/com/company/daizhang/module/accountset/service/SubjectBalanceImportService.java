package com.company.daizhang.module.accountset.service;

import com.company.daizhang.common.vo.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 期初余额Excel导入服务接口
 */
public interface SubjectBalanceImportService {

    /**
     * 从Excel批量导入期初余额
     *
     * @param accountSetId 账套ID
     * @param year         年度
     * @param file         Excel文件
     * @return 导入结果
     */
    ImportResultVO importBalances(Long accountSetId, Integer year, MultipartFile file);

    /**
     * 下载期初余额导入模板
     *
     * @return 模板Excel字节数组
     */
    byte[] downloadTemplate();
}
