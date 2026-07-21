package com.company.daizhang.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 * <p>
 * P3.4 增强:新增 user_agent / before_value / after_value / request_path / request_method 字段,
 * 用于敏感操作的完整审计(操作前后数据快照 + 请求来源信息)。
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String operation;

    private String method;

    private String params;

    private String ip;

    private Integer status;

    private String errorMsg;

    private Long costTime;

    private LocalDateTime createTime;

    // ==================== P3.4 扩展字段 ====================

    /**
     * 客户端 User-Agent,用于追溯操作终端
     */
    private String userAgent;

    /**
     * 操作前值(JSON),用于敏感操作前后对比
     */
    private String beforeValue;

    /**
     * 操作后值(JSON),用于敏感操作前后对比
     */
    private String afterValue;

    /**
     * 请求路径(如 /system/user/1),便于按 URL 维度审计
     */
    private String requestPath;

    /**
     * HTTP 方法(GET/POST/PUT/DELETE)
     */
    private String requestMethod;
}
