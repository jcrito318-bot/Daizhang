package com.company.daizhang.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.entity.SysOperationLog;

/**
 * 操作日志服务接口
 */
public interface SysOperationLogService extends IService<SysOperationLog> {
    
    /**
     * 分页查询操作日志
     */
    PageResult<SysOperationLog> pageLogs(String username, String operation, String startDate, String endDate, int pageNum, int pageSize);
    
    /**
     * 保存操作日志
     */
    void saveLog(SysOperationLog log);
}
