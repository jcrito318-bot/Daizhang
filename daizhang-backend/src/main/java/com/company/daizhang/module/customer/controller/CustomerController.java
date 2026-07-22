package com.company.daizhang.module.customer.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.customer.dto.CustomerCreateRequest;
import com.company.daizhang.module.customer.dto.CustomerQueryRequest;
import com.company.daizhang.module.customer.dto.CustomerUpdateRequest;
import com.company.daizhang.module.customer.service.ContractService;
import com.company.daizhang.module.customer.service.CustomerService;
import com.company.daizhang.module.customer.vo.ArrearsVO;
import com.company.daizhang.module.customer.vo.ContractRenewalReminderVO;
import com.company.daizhang.module.customer.vo.CustomerProfileVO;
import com.company.daizhang.module.customer.vo.CustomerVO;
import com.company.daizhang.module.system.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
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
    private final ContractService contractService;
    private final NotificationService notificationService;

    /** 通知类型:欠款催收预警 (B3) */
    private static final String NOTIFICATION_TYPE_ARREARS_WARNING = "ARREARS_WARNING";
    /** 通知类型:合同到期预警 (B4) */
    private static final String NOTIFICATION_TYPE_CONTRACT_EXPIRING = "CONTRACT_EXPIRING";

    @Operation(summary = "分页查询客户")
    @GetMapping("/page")
    public Result<PageResult<CustomerVO>> page(@Valid CustomerQueryRequest request) {
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
    // IDOR 防护(纵深防御):edge-level 预校验,Service 层仍保留 checkOwner 作为兜底
    @RequireAccountSetAccess(value = RequireAccountSetAccess.AccessLevel.OWNER, required = false)
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

    @Operation(summary = "扫描欠款并生成催收通知(B3)")
    @PostMapping("/arrears/scan")
    public Result<Map<String, Integer>> scanArrearsAndNotify() {
        // 复用现有欠款查询逻辑(getCollectionReminders 已按可访问账套过滤)
        List<Map<String, Object>> arrearsList = customerService.getCollectionReminders();
        Long userId = SecurityUtils.getCurrentUserIdRequired();

        int notifiedCount = 0;
        for (Map<String, Object> arrears : arrearsList) {
            Object customerIdObj = arrears.get("customerId");
            Long customerId = customerIdObj instanceof Long
                    ? (Long) customerIdObj
                    : (customerIdObj != null ? Long.valueOf(customerIdObj.toString()) : null);

            String customerName = arrears.get("customerName") != null ? arrears.get("customerName").toString() : "";
            BigDecimal totalArrears = toBigDecimal(arrears.get("totalArrearsAmount"));
            Object overdueMonthsObj = arrears.get("overdueMonths");
            int overdueMonths = overdueMonthsObj instanceof Number
                    ? ((Number) overdueMonthsObj).intValue()
                    : 0;
            String riskLevel = arrears.get("riskLevel") != null ? arrears.get("riskLevel").toString() : "";
            String suggestion = arrears.get("suggestion") != null ? arrears.get("suggestion").toString() : "";

            String title = String.format("催收预警:客户 %s 欠款 %s 元", customerName, totalArrears.toPlainString());
            String content = String.format(
                    "客户:%s\n欠款总额:%s 元\n逾期月数:%d\n风险等级:%s\n催收建议:%s",
                    customerName, totalArrears.toPlainString(), overdueMonths, riskLevel, suggestion);
            String level = determineArrearsLevel(overdueMonths, totalArrears);

            notificationService.sendNotification(userId, null, customerId,
                    NOTIFICATION_TYPE_ARREARS_WARNING, title, content, level);
            notifiedCount++;
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("scannedCount", arrearsList.size());
        result.put("notifiedCount", notifiedCount);
        return Result.success(result);
    }

    @Operation(summary = "扫描即将到期合同并生成预警通知(B4)")
    @PostMapping("/contract/expiring/scan")
    public Result<Map<String, Integer>> scanExpiringContracts(
            @RequestParam(defaultValue = "30") int daysBeforeExpire) {
        // 复用现有合同到期提醒逻辑(getRenewalReminders 已按可访问账套过滤)
        List<ContractRenewalReminderVO> contracts = contractService.getRenewalReminders(daysBeforeExpire);
        Long userId = SecurityUtils.getCurrentUserIdRequired();

        int notifiedCount = 0;
        for (ContractRenewalReminderVO contract : contracts) {
            String customerName = contract.getCustomerName() != null ? contract.getCustomerName() : "";
            String contractName = contract.getContractName() != null ? contract.getContractName() : "";
            String endDate = contract.getEndDate() != null ? contract.getEndDate().toString() : "";
            int daysRemaining = contract.getDaysRemaining() != null ? contract.getDaysRemaining() : 0;
            BigDecimal contractAmount = contract.getContractAmount() != null
                    ? contract.getContractAmount() : BigDecimal.ZERO;

            String title = String.format("合同到期预警:%s (客户:%s)", contractName, customerName);
            String content = String.format(
                    "客户:%s\n合同名称:%s\n到期日期:%s\n剩余天数:%d\n合同金额:%s 元",
                    customerName, contractName, endDate, daysRemaining, contractAmount.toPlainString());
            // 剩余天数 ≤ 7 紧急,≤ 30 警告,否则信息
            String level = daysRemaining <= 7 ? "URGENT" : (daysRemaining <= 30 ? "WARN" : "INFO");

            notificationService.sendNotification(userId, null, contract.getCustomerId(),
                    NOTIFICATION_TYPE_CONTRACT_EXPIRING, title, content, level);
            notifiedCount++;
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("scannedCount", contracts.size());
        result.put("notifiedCount", notifiedCount);
        return Result.success(result);
    }

    /**
     * 欠款金额/逾期月数转 BigDecimal,兼容 Number/Object 输入
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 根据逾期月数与欠款金额确定通知级别:
     * 逾期>6个月或欠款≥10万:URGENT;
     * 逾期>3个月或欠款≥1万:WARN;
     * 其他:INFO
     */
    private String determineArrearsLevel(int overdueMonths, BigDecimal totalArrears) {
        BigDecimal urgentThreshold = new BigDecimal("100000");
        BigDecimal warnThreshold = new BigDecimal("10000");
        if (overdueMonths > 6 || totalArrears.compareTo(urgentThreshold) >= 0) {
            return "URGENT";
        }
        if (overdueMonths > 3 || totalArrears.compareTo(warnThreshold) >= 0) {
            return "WARN";
        }
        return "INFO";
    }
}
