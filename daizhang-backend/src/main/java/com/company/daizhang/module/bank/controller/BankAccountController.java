package com.company.daizhang.module.bank.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.bank.dto.BankAccountQueryRequest;
import com.company.daizhang.module.bank.dto.BankAccountRequest;
import com.company.daizhang.module.bank.service.BankAccountService;
import com.company.daizhang.module.bank.vo.BankAccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 银行账户主数据管理控制器
 */
@Slf4j
@Tag(name = "银行账户管理")
@RestController
@RequestMapping("/bank/account")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @Operation(summary = "分页查询银行账户")
    @GetMapping("/page")
    public Result<PageResult<BankAccountVO>> page(BankAccountQueryRequest request) {
        PageResult<BankAccountVO> page = bankAccountService.pageBankAccounts(request);
        return Result.success(page);
    }

    @Operation(summary = "查询账套下所有银行账户")
    @GetMapping("/list")
    public Result<List<BankAccountVO>> list(@RequestParam Long accountSetId) {
        List<BankAccountVO> list = bankAccountService.listByAccountSetId(accountSetId);
        return Result.success(list);
    }

    @Operation(summary = "根据ID查询银行账户")
    @GetMapping("/{id}")
    public Result<BankAccountVO> getById(@PathVariable Long id) {
        BankAccountVO vo = bankAccountService.getBankAccountById(id);
        return Result.success(vo);
    }

    @Operation(summary = "创建银行账户")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody BankAccountRequest request) {
        bankAccountService.createBankAccount(request);
        return Result.success();
    }

    @Operation(summary = "更新银行账户")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody BankAccountRequest request) {
        bankAccountService.updateBankAccount(id, request);
        return Result.success();
    }

    @Operation(summary = "删除银行账户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bankAccountService.deleteBankAccount(id);
        return Result.success();
    }

    @Operation(summary = "更新银行账户状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        bankAccountService.updateStatus(id, status);
        return Result.success();
    }
}
