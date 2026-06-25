package com.company.daizhang.module.customer.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.customer.dto.CustomerCreateRequest;
import com.company.daizhang.module.customer.dto.CustomerQueryRequest;
import com.company.daizhang.module.customer.dto.CustomerUpdateRequest;
import com.company.daizhang.module.customer.service.CustomerService;
import com.company.daizhang.module.customer.vo.ArrearsVO;
import com.company.daizhang.module.customer.vo.CustomerProfileVO;
import com.company.daizhang.module.customer.vo.CustomerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 客户管理控制器
 */
@Tag(name = "客户管理")
@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "分页查询客户")
    @GetMapping("/page")
    public Result<PageResult<CustomerVO>> page(CustomerQueryRequest request) {
        PageResult<CustomerVO> page = customerService.pageCustomers(request);
        return Result.success(page);
    }

    @Operation(summary = "查询所有客户")
    @GetMapping("/list")
    public Result<List<CustomerVO>> list() {
        List<CustomerVO> list = customerService.listAllCustomers();
        return Result.success(list);
    }

    @Operation(summary = "根据ID查询客户")
    @GetMapping("/{id}")
    public Result<CustomerVO> getById(@PathVariable Long id) {
        CustomerVO customer = customerService.getCustomerById(id);
        return Result.success(customer);
    }

    @Operation(summary = "创建客户")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CustomerCreateRequest request) {
        customerService.createCustomer(request);
        return Result.success();
    }

    @Operation(summary = "更新客户")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest request) {
        customerService.updateCustomer(id, request);
        return Result.success();
    }

    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return Result.success();
    }

    @Operation(summary = "按等级/状态/行业分页查询客户")
    @GetMapping("/page-by-level")
    public Result<PageResult<CustomerVO>> pageByLevel(
            @RequestParam(required = false) String customerLevel,
            @RequestParam(required = false) Integer customerStatus,
            @RequestParam(required = false) String industryType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<CustomerVO> page = customerService.pageCustomersByLevel(
                customerLevel, customerStatus, industryType, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "客户360度画像")
    @GetMapping("/profile/{id}")
    public Result<CustomerProfileVO> profile(@PathVariable Long id) {
        CustomerProfileVO profile = customerService.getCustomerProfile(id);
        return Result.success(profile);
    }

    @Operation(summary = "更新客户状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        customerService.updateCustomerStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "更新客户等级")
    @PutMapping("/{id}/level")
    public Result<Void> updateLevel(@PathVariable Long id, @RequestParam String level) {
        customerService.updateCustomerLevel(id, level);
        return Result.success();
    }

    @Operation(summary = "客户统计(按等级/状态/行业分组)")
    @GetMapping("/statistics")
    public Result<List<Map<String, Object>>> statistics() {
        List<Map<String, Object>> statistics = customerService.getCustomerStatistics();
        return Result.success(statistics);
    }

    @Operation(summary = "欠款列表分页查询")
    @GetMapping("/arrears/page")
    public Result<PageResult<ArrearsVO>> pageArrears(
            @RequestParam(required = false) String customerName,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<ArrearsVO> page = customerService.pageArrears(customerName, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "欠款详情")
    @GetMapping("/arrears/{customerId}")
    public Result<ArrearsVO> getArrearsDetail(@PathVariable Long customerId) {
        ArrearsVO vo = customerService.getArrearsDetail(customerId);
        return Result.success(vo);
    }

    @Operation(summary = "催收提醒列表")
    @GetMapping("/collection-reminders")
    public Result<List<Map<String, Object>>> collectionReminders() {
        List<Map<String, Object>> reminders = customerService.getCollectionReminders();
        return Result.success(reminders);
    }
}
