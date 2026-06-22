package com.company.daizhang.module.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.entity.SysOperationLog;
import com.company.daizhang.module.system.mapper.SysOperationLogMapper;
import com.company.daizhang.module.system.service.SysOperationLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 操作日志服务实现
 */
@Service
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog> implements SysOperationLogService {
    
    @Override
    public PageResult<SysOperationLog> pageLogs(String username, String operation, String startDate, String endDate, int pageNum, int pageSize) {
        Page<SysOperationLog> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(username), SysOperationLog::getUsername, username)
               .like(StrUtil.isNotBlank(operation), SysOperationLog::getOperation, operation)
               .ge(StrUtil.isNotBlank(startDate), SysOperationLog::getCreateTime, parseStartDate(startDate))
               .le(StrUtil.isNotBlank(endDate), SysOperationLog::getCreateTime, parseEndDate(endDate))
               .orderByDesc(SysOperationLog::getCreateTime);
        
        Page<SysOperationLog> result = this.page(page, wrapper);
        
        return new PageResult<>(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }
    
    @Override
    public void saveLog(SysOperationLog log) {
        this.save(log);
    }
    
    private LocalDateTime parseStartDate(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return date.atStartOfDay();
    }
    
    private LocalDateTime parseEndDate(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return date.atTime(LocalTime.MAX);
    }
}
