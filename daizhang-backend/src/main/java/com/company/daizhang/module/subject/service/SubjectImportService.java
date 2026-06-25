package com.company.daizhang.module.subject.service;

import com.company.daizhang.common.vo.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 科目Excel导入服务接口
 */
public interface SubjectImportService {

    /**
     * 从Excel批量导入科目
     *
     * @param accountSetId 账套ID
     * @param file        Excel文件
     * @return 导入结果
     */
    ImportResultVO importSubjects(Long accountSetId, MultipartFile file);

    /**
     * 下载科目导入模板
     *
     * @return 模板Excel字节数组
     */
    byte[] downloadTemplate();
}
