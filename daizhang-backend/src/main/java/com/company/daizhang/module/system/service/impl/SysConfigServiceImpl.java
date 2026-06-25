package com.company.daizhang.module.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.dto.SysConfigRequest;
import com.company.daizhang.module.system.entity.SysConfig;
import com.company.daizhang.module.system.mapper.SysConfigMapper;
import com.company.daizhang.module.system.service.SysConfigService;
import com.company.daizhang.module.system.vo.SysConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统设置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public PageResult<SysConfigVO> pageConfigs(String configKey, String configName, int pageNum, int pageSize) {
        Page<SysConfig> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(configKey), SysConfig::getConfigKey, configKey)
               .like(StrUtil.isNotBlank(configName), SysConfig::getConfigName, configName)
               .orderByDesc(SysConfig::getCreateTime);

        Page<SysConfig> result = this.page(page, wrapper);

        List<SysConfigVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public String getConfigValue(String configKey) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey);
        SysConfig sysConfig = this.getOne(wrapper);
        return sysConfig == null ? null : sysConfig.getConfigValue();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setConfigValue(String configKey, String configValue) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey);
        SysConfig existing = this.getOne(wrapper);
        if (existing == null) {
            SysConfig sysConfig = new SysConfig();
            sysConfig.setConfigKey(configKey);
            sysConfig.setConfigValue(configValue);
            this.save(sysConfig);
            log.info("创建系统设置，key: {}", configKey);
        } else {
            existing.setConfigValue(configValue);
            this.updateById(existing);
            log.info("更新系统设置，key: {}", configKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createConfig(SysConfigRequest request) {
        // 检查key是否已存在
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, request.getConfigKey());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数键已存在");
        }

        SysConfig sysConfig = new SysConfig();
        BeanUtil.copyProperties(request, sysConfig);
        this.save(sysConfig);
        log.info("创建系统设置成功，key: {}", request.getConfigKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(Long id, SysConfigRequest request) {
        SysConfig sysConfig = this.getById(id);
        if (sysConfig == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "系统设置不存在");
        }

        // 检查key是否与其他记录冲突
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, request.getConfigKey())
               .ne(SysConfig::getId, id);
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数键已存在");
        }

        BeanUtil.copyProperties(request, sysConfig);
        sysConfig.setId(id);
        this.updateById(sysConfig);
        log.info("更新系统设置成功，id: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        SysConfig sysConfig = this.getById(id);
        if (sysConfig == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "系统设置不存在");
        }
        this.removeById(id);
        log.info("删除系统设置成功，id: {}", id);
    }

    private SysConfigVO convertToVO(SysConfig sysConfig) {
        SysConfigVO vo = new SysConfigVO();
        BeanUtil.copyProperties(sysConfig, vo);
        return vo;
    }
}
