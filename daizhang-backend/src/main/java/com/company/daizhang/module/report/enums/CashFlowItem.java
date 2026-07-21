package com.company.daizhang.module.report.enums;

import lombok.Getter;

/**
 * 现金流量表项目枚举（直接法）
 * 依据《企业会计准则第31号—现金流量表》编制，共23项。
 * <p>
 * category 取值：operating(经营) / investing(投资) / financing(筹资) / other(其他) / summary(汇总)
 * direction 取值：inflow(流入) / outflow(流出) / net(净额) / balance(余额)
 */
@Getter
public enum CashFlowItem {

    // 一、经营活动产生的现金流量（7项）
    SALES_RECEIPTS("销售商品、提供劳务收到的现金", "operating", "inflow"),
    TAX_REFUNDS("收到的税费返还", "operating", "inflow"),
    OTHER_OPERATING_RECEIPTS("收到其他与经营活动有关的现金", "operating", "inflow"),
    PURCHASE_PAYMENTS("购买商品、接受劳务支付的现金", "operating", "outflow"),
    EMPLOYEE_PAYMENTS("支付给职工以及为职工支付的现金", "operating", "outflow"),
    TAX_PAYMENTS("支付的各项税费", "operating", "outflow"),
    OTHER_OPERATING_PAYMENTS("支付其他与经营活动有关的现金", "operating", "outflow"),

    // 二、投资活动产生的现金流量（6项）
    INVESTMENT_RECEIPTS("收回投资收到的现金", "investing", "inflow"),
    INVESTMENT_INCOME("取得投资收益收到的现金", "investing", "inflow"),
    ASSET_DISPOSAL("处置固定资产、无形资产和其他长期资产收回的现金净额", "investing", "inflow"),
    OTHER_INVESTING_RECEIPTS("收到其他与投资活动有关的现金", "investing", "inflow"),
    ASSET_PURCHASE("购建固定资产、无形资产和其他长期资产支付的现金", "investing", "outflow"),
    INVESTMENT_PAYMENTS("投资支付的现金", "investing", "outflow"),
    OTHER_INVESTING_PAYMENTS("支付其他与投资活动有关的现金", "investing", "outflow"),

    // 三、筹资活动产生的现金流量（6项）
    FINANCING_RECEIPTS("吸收投资收到的现金", "financing", "inflow"),
    LOAN_RECEIPTS("取得借款收到的现金", "financing", "inflow"),
    OTHER_FINANCING_RECEIPTS("收到其他与筹资活动有关的现金", "financing", "inflow"),
    DEBT_REPAYMENT("偿还债务支付的现金", "financing", "outflow"),
    DISTRIBUTION_PAYMENTS("分配股利、利润或偿付利息支付的现金", "financing", "outflow"),
    OTHER_FINANCING_PAYMENTS("支付其他与筹资活动有关的现金", "financing", "outflow"),

    // 四、汇率变动影响
    EXCHANGE_EFFECT("汇率变动对现金的影响", "other", "net"),

    // 五、现金及现金等价物余额
    CASH_BEGINNING("期初现金及现金等价物余额", "summary", "balance"),
    CASH_ENDING("期末现金及现金等价物余额", "summary", "balance");

    /**
     * 项目名称
     */
    private final String itemName;

    /**
     * 分类：operating / investing / financing / other / summary
     */
    private final String category;

    /**
     * 方向：inflow / outflow / net / balance
     */
    private final String direction;

    CashFlowItem(String itemName, String category, String direction) {
        this.itemName = itemName;
        this.category = category;
        this.direction = direction;
    }
}
