package com.company.daizhang.module.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.entity.LoginLog;
import com.company.daizhang.module.system.mapper.LoginLogMapper;
import com.company.daizhang.module.system.service.LoginLogService;
import com.company.daizhang.module.system.vo.LoginLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl extends ServiceImpl<LoginLogMapper, LoginLog> implements LoginLogService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public PageResult<LoginLogVO> pageLogs(String username, Integer loginStatus, String startDate, String endDate, int pageNum, int pageSize) {
        Page<LoginLog> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(username), LoginLog::getUsername, username)
               .eq(loginStatus != null, LoginLog::getLoginStatus, loginStatus);

        // 日期条件需先解析为局部变量再传入，避免条件值被无条件求值
        LocalDateTime startDateTime = null;
        if (StrUtil.isNotBlank(startDate)) {
            startDateTime = LocalDate.parse(startDate, DATE_FORMATTER).atStartOfDay();
        }
        LocalDateTime endDateTime = null;
        if (StrUtil.isNotBlank(endDate)) {
            endDateTime = LocalDate.parse(endDate, DATE_FORMATTER).plusDays(1).atStartOfDay();
        }
        wrapper.ge(startDateTime != null, LoginLog::getLoginTime, startDateTime)
               .lt(endDateTime != null, LoginLog::getLoginTime, endDateTime)
               .orderByDesc(LoginLog::getLoginTime);

        Page<LoginLog> result = this.page(page, wrapper);

        List<LoginLogVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public void saveLog(LoginLog loginLog) {
        if (loginLog.getLoginTime() == null) {
            loginLog.setLoginTime(LocalDateTime.now());
        }
        this.save(loginLog);
        log.info("保存登录日志，用户名: {}, 登录状态: {}", loginLog.getUsername(), loginLog.getLoginStatus());
    }

    @Override
    public void deleteLogs(String beforeDate) {
        LocalDateTime beforeDateTime = LocalDate.parse(beforeDate, DATE_FORMATTER).atStartOfDay();
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(LoginLog::getLoginTime, beforeDateTime);
        long count = this.count(wrapper);
        this.remove(wrapper);
        log.info("清理登录日志完成，清理日期前: {}, 清理数量: {}", beforeDate, count);
    }

    private LoginLogVO convertToVO(LoginLog loginLog) {
        LoginLogVO vo = new LoginLogVO();
        BeanUtil.copyProperties(loginLog, vo);
        return vo;
    }
}
