package com.company.daizhang.module.inventory.controller;

import com.company.daizhang.common.annotation.OperationLog;
import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.inventory.service.InventoryVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 存货出入库凭证生成控制器
 */
@Tag(name = "库存凭证管理")
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryVoucherController {

    private final InventoryVoucherService inventoryVoucherService;

    @Operation(summary = "生成入库凭证")
    @PostMapping("/in/{id}/voucher")
    @OperationLog("生成入库凭证")
    // 入参为入库单ID，accountSetId 由 Service 层 checkOwner 兜底校验（参考 InventoryController.auditIn 用法）
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Long> generateInVoucher(@PathVariable Long id) {
        return Result.success(inventoryVoucherService.generateInVoucher(id));
    }

    @Operation(summary = "生成出库凭证")
    @PostMapping("/out/{id}/voucher")
    @OperationLog("生成出库凭证")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Long> generateOutVoucher(@PathVariable Long id) {
        return Result.success(inventoryVoucherService.generateOutVoucher(id));
    }

    @Operation(summary = "批量生成入库凭证")
    @PostMapping("/in/voucher-batch")
    @OperationLog("批量生成入库凭证")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Long> generateInVoucherBatch(@RequestBody List<Long> inIds) {
        validateBatchSize(inIds);
        return Result.success(inventoryVoucherService.generateInVoucherBatch(inIds));
    }

    @Operation(summary = "批量生成出库凭证")
    @PostMapping("/out/voucher-batch")
    @OperationLog("批量生成出库凭证")
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Long> generateOutVoucherBatch(@RequestBody List<Long> outIds) {
        validateBatchSize(outIds);
        return Result.success(inventoryVoucherService.generateOutVoucherBatch(outIds));
    }

    /**
     * 批量操作集合大小限制:防止客户端传入超大列表导致长时间事务占用和内存溢出
     */
    private void validateBatchSize(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "批量操作ID列表不能为空");
        }
        if (ids.size() > 200) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "批量操作数量超过限制(最大200条)，当前：" + ids.size());
        }
    }
}
