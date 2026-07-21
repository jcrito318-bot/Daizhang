package com.company.daizhang.module.customer.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.customer.dto.PaymentCreateRequest;
import com.company.daizhang.module.customer.dto.PaymentQueryRequest;
import com.company.daizhang.module.customer.dto.PaymentUpdateRequest;
import com.company.daizhang.module.customer.service.PaymentService;
import com.company.daizhang.module.customer.vo.PaymentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收款记录管理控制器
 */
@Tag(name = "收款记录管理")
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "分页查询收款记录")
    @GetMapping("/page")
    public Result<PageResult<PaymentVO>> page(@Valid PaymentQueryRequest request) {
        PageResult<PaymentVO> page = paymentService.pagePayments(request);
        return Result.success(page);
    }

    @Operation(summary = "根据客户ID查询收款记录")
    @GetMapping("/customer/{customerId}")
    public Result<List<PaymentVO>> listByCustomerId(@PathVariable Long customerId) {
        List<PaymentVO> list = paymentService.listPaymentsByCustomerId(customerId);
        return Result.success(list);
    }

    @Operation(summary = "根据合同ID查询收款记录")
    @GetMapping("/contract/{contractId}")
    public Result<List<PaymentVO>> listByContractId(@PathVariable Long contractId) {
        List<PaymentVO> list = paymentService.listPaymentsByContractId(contractId);
        return Result.success(list);
    }

    @Operation(summary = "根据ID查询收款记录")
    @GetMapping("/{id}")
    public Result<PaymentVO> getById(@PathVariable Long id) {
        PaymentVO payment = paymentService.getPaymentById(id);
        return Result.success(payment);
    }

    @Operation(summary = "创建收款记录")
    @PostMapping
    // IDOR 防护(纵深防御):edge-level 预校验,Service 层仍保留 checkOwner 作为兜底
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> create(@Valid @RequestBody PaymentCreateRequest request) {
        paymentService.createPayment(request);
        return Result.success();
    }

    @Operation(summary = "更新收款记录")
    @PutMapping("/{id}")
    // IDOR 防护(纵深防御):edge-level 预校验,Service 层仍保留 checkOwner 作为兜底
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PaymentUpdateRequest request) {
        paymentService.updatePayment(id, request);
        return Result.success();
    }

    @Operation(summary = "删除收款记录")
    @DeleteMapping("/{id}")
    // IDOR 防护(纵深防御):edge-level 预校验,Service 层仍保留 checkOwner 作为兜底
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
    public Result<Void> delete(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return Result.success();
    }
}
