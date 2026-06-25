package com.company.daizhang.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.dto.SysConfigRequest;
import com.company.daizhang.module.system.entity.SysConfig;
import com.company.daizhang.module.system.vo.SysConfigVO;

/**
 * 系统设置服务接口
 */
public interface SysConfigService extends IService<SysConfig> {

    /**
     * 分页查询系统设置
     */
    PageResult<SysConfigVO> pageConfigs(String configKey, String configName, int pageNum, int pageSize);

    /**
     * 根据key获取配置值
     */
    String getConfigValue(String configKey);

    /**
     * 设置配置值（不存在则创建）
     */
    void setConfigValue(String configKey, String configValue);

    /**
     * 创建系统设置
     */
    void createConfig(SysConfigRequest request);

    /**
     * 更新系统设置
     */
    void updateConfig(Long id, SysConfigRequest request);

    /**
     * 删除系统设置
     */
    void deleteConfig(Long id);
}
