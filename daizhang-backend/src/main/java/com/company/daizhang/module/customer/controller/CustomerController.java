package com.company.daizhang.module.customer.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.customer.dto.CustomerCreateRequest;
import com.company.daizhang.module.customer.dto.CustomerQueryRequest;
import com.company.daizhang.module.customer.dto.CustomerUpdateRequest;
import com.company.daizhang.module.customer.service.CustomerService;
import com.company.daizhang.module.customer.vo.CustomerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
