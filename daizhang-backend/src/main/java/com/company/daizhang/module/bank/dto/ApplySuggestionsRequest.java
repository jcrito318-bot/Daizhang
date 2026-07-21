package com.company.daizhang.module.bank.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 智能匹配建议批量应用请求
 * <p>
 * 用户在前端确认接受若干条匹配建议后,以此请求批量回写匹配关系。
 * 每项需提供流水ID、凭证ID,Service 层将再次校验流水未匹配且凭证已过账。
 */
@Data
public class ApplySuggestionsRequest {

    /**
     * 账套ID(IDOR 治理必填)
     */
    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    /**
     * 已接受的建议项列表
     */
    @NotEmpty(message = "应用建议列表不能为空")
    private List<ApplyItem> items;

    /**
     * 单条建议项
     */
    @Data
    public static class ApplyItem {

        /**
         * 银行流水ID
         */
        @NotNull(message = "流水ID不能为空")
        private Long transactionId;

        /**
         * 凭证ID
         */
        @NotNull(message = "凭证ID不能为空")
        private Long voucherId;
    }
}
