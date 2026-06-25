package com.company.daizhang.common.vo;

import lombok.Data;

import java.util.List;

/**
 * 导入结果VO
 */
@Data
public class ImportResultVO {

    /**
     * 总数
     */
    private Integer totalCount;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failCount;

    /**
     * 错误信息列表
     */
    private List<String> errorMessages;

    public static ImportResultVO of(int total, int success, int fail, List<String> errors) {
        ImportResultVO vo = new ImportResultVO();
        vo.setTotalCount(total);
        vo.setSuccessCount(success);
        vo.setFailCount(fail);
        vo.setErrorMessages(errors);
        return vo;
    }
}
