package com.company.daizhang.module.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.module.salary.dto.SocialSecurityConfigRequest;
import com.company.daizhang.module.salary.entity.SocialSecurityConfig;
import com.company.daizhang.module.salary.vo.SocialSecurityCalculationVO;
import com.company.daizhang.module.salary.vo.SocialSecurityConfigVO;

import java.math.BigDecimal;

/**
 * 社保公积金配置服务接口
 */
public interface SocialSecurityConfigService extends IService<SocialSecurityConfig> {

    /**
     * 获取配置
     */
    SocialSecurityConfigVO getConfig(Long accountSetId, Integer year);

    /**
     * 保存或更新配置
     */
    void saveConfig(SocialSecurityConfigRequest request);

    /**
     * 根据基数计算社保
     */
    SocialSecurityCalculationVO calculate(Long accountSetId, Integer year, BigDecimal baseSalary);
}
