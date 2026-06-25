package com.company.daizhang.module.customer.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 客户360度画像视图对象
 */
@Data
public class CustomerProfileVO {

    /**
     * 客户基本信息(含分类、状态、等级)
     */
    private CustomerVO customer;

    /**
     * 合同数
     */
    private Integer contractCount;

    /**
     * 生效合同数
     */
    private Integer activeContractCount;

    /**
     * 合同总金额
     */
    private BigDecimal totalContractAmount;

    /**
     * 即将到期合同数(30天内)
     */
    private Integer expiringContractCount;

    /**
     * 已收款总额
     */
    private BigDecimal totalPaidAmount;

    /**
     * 未收款总额(欠款)
     */
    private BigDecimal totalUnpaidAmount;

    /**
     * 凭证数(本月)
     */
    private Integer voucherCount;

    /**
     * 发票数(本月)
     */
    private Integer invoiceCount;

    /**
     * 近期收款记录
     */
    private List<Map<String, Object>> recentPayments;

    /**
     * 近期合同
     */
    private List<Map<String, Object>> recentContracts;

    /**
     * 服务信息(服务流程进度)
     */
    private ServiceProgressVO serviceProgress;

    /**
     * 风险信息
     */
    private RiskInfoVO riskInfo;

    /**
     * 服务流程进度视图对象
     */
    @Data
    public static class ServiceProgressVO {
        /**
         * 当前期间(年-月)
         */
        private String currentPeriod;

        /**
         * 总任务数
         */
        private Integer totalTaskCount;

        /**
         * 已完成任务数
         */
        private Integer completedTaskCount;

        /**
         * 进行中任务数
         */
        private Integer inProgressTaskCount;

        /**
         * 待处理任务数
         */
        private Integer pendingTaskCount;

        /**
         * 完成进度(%)
         */
        private BigDecimal progressPercent;

        /**
         * 当前节点名称
         */
        private String currentNodeName;
    }

    /**
     * 风险信息视图对象
     */
    @Data
    public static class RiskInfoVO {
        /**
         * 欠款总额
         */
        private BigDecimal arrearsAmount;

        /**
         * 逾期月数
         */
        private Integer overdueMonths;

        /**
         * 风险等级(低风险/中风险/高风险)
         */
        private String riskLevel;

        /**
         * 账龄(天)
         */
        private Integer accountAgeDays;

        /**
         * 风险描述
         */
        private String riskDescription;
    }
}
