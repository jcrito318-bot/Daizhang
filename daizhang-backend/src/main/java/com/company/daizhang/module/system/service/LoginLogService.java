package com.company.daizhang.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.entity.LoginLog;
import com.company.daizhang.module.system.vo.LoginLogVO;

/**
 * 登录日志服务接口
 */
public interface LoginLogService extends IService<LoginLog> {

    /**
     * 分页查询登录日志
     */
    PageResult<LoginLogVO> pageLogs(String username, Integer loginStatus, String startDate, String endDate, int pageNum, int pageSize);

    /**
     * 保存登录日志
     */
    void saveLog(LoginLog loginLog);

    /**
     * 清理指定日期之前的日志
     */
    void deleteLogs(String beforeDate);
}
