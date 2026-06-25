package com.company.daizhang.module.system.controller;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.system.service.EmployeePerformanceService;
import com.company.daizhang.module.system.vo.EmployeePerformanceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 员工绩效管理控制器
 */
@Slf4j
@Tag(name = "员工绩效管理")
@RestController
@RequestMapping("/system/performance")
@RequiredArgsConstructor
public class EmployeePerformanceController {

    private final EmployeePerformanceService employeePerformanceService;

    @Operation(summary = "分页查询员工绩效")
    @GetMapping("/page")
    public Result<PageResult<EmployeePerformanceVO>> page(@RequestParam(required = false) Long userId,
                                                          @RequestParam(required = false) Integer year,
                                                          @RequestParam(required = false) Integer month,
                                                          @RequestParam(defaultValue = "1") int pageNum,
                                                          @RequestParam(defaultValue = "10") int pageSize) {
        PageResult<EmployeePerformanceVO> page = employeePerformanceService.pagePerformances(userId, year, month, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "查询某员工某月绩效")
    @GetMapping
    public Result<EmployeePerformanceVO> get(@RequestParam Long userId,
                                            @RequestParam Integer year,
                                            @RequestParam Integer month) {
        EmployeePerformanceVO vo = employeePerformanceService.getPerformance(userId, year, month);
        return Result.success(vo);
    }

    @Operation(summary = "自动生成员工绩效")
    @PostMapping("/generate")
    public Result<Void> generate(@RequestParam Integer year, @RequestParam Integer month) {
        employeePerformanceService.generatePerformance(year, month);
        return Result.success();
    }
}
