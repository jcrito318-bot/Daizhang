package com.company.daizhang.module.inventory.service;

import java.util.List;

/**
 * 存货出入库凭证生成服务
 * <p>
 * 用于将入库单/出库单与总账打通：
 * 入库按采购入库生成"借:库存商品 贷:应付账款/银行存款"凭证；
 * 出库按销售出库生成"借:主营业务成本 贷:库存商品"凭证，成本按加权平均法计算。
 */
public interface InventoryVoucherService {

    /**
     * 生成入库凭证（采购入库：借库存商品，贷应付账款/银行存款）
     *
     * @param inId 入库单ID
     * @return 凭证ID
     */
    Long generateInVoucher(Long inId);

    /**
     * 生成出库凭证（销售出库：借主营业务成本，贷库存商品）
     *
     * @param outId 出库单ID
     * @return 凭证ID
     */
    Long generateOutVoucher(Long outId);

    /**
     * 批量生成入库凭证
     *
     * @param inIds 入库单ID列表
     * @return 成功生成凭证的数量
     */
    Long generateInVoucherBatch(List<Long> inIds);

    /**
     * 批量生成出库凭证
     *
     * @param outIds 出库单ID列表
     * @return 成功生成凭证的数量
     */
    Long generateOutVoucherBatch(List<Long> outIds);
}
