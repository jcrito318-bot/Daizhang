package com.company.daizhang.module.salary.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.salary.dto.SocialSecurityConfigRequest;
import com.company.daizhang.module.salary.entity.SocialSecurityConfig;
import com.company.daizhang.module.salary.mapper.SocialSecurityConfigMapper;
import com.company.daizhang.module.salary.service.SocialSecurityConfigService;
import com.company.daizhang.module.salary.vo.SocialSecurityCalculationVO;
import com.company.daizhang.module.salary.vo.SocialSecurityConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 社保公积金配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialSecurityConfigServiceImpl extends ServiceImpl<SocialSecurityConfigMapper, SocialSecurityConfig> implements SocialSecurityConfigService {

    @Override
    public SocialSecurityConfigVO getConfig(Long accountSetId, Integer year) {
        LambdaQueryWrapper<SocialSecurityConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SocialSecurityConfig::getAccountSetId, accountSetId)
               .eq(SocialSecurityConfig::getYear, year);
        SocialSecurityConfig config = this.getOne(wrapper);
        if (config == null) {
            return null;
        }
        return convertToVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(SocialSecurityConfigRequest request) {
        // 查询是否已存在配置
        LambdaQueryWrapper<SocialSecurityConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SocialSecurityConfig::getAccountSetId, request.getAccountSetId())
               .eq(SocialSecurityConfig::getYear, request.getYear());
        SocialSecurityConfig existing = this.getOne(wrapper);

        if (existing == null) {
            // 新增
            SocialSecurityConfig config = new SocialSecurityConfig();
            BeanUtil.copyProperties(request, config);
            this.save(config);
            log.info("新增社保公积金配置成功，账套ID: {}, 年度: {}", request.getAccountSetId(), request.getYear());
        } else {
            // 更新
            BeanUtil.copyProperties(request, existing);
            this.updateById(existing);
            log.info("更新社保公积金配置成功，账套ID: {}, 年度: {}", request.getAccountSetId(), request.getYear());
        }
    }

    @Override
    public SocialSecurityCalculationVO calculate(Long accountSetId, Integer year, BigDecimal baseSalary) {
        SocialSecurityConfigVO config = getConfig(accountSetId, year);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "社保公积金配置不存在");
        }

        BigDecimal base = baseSalary;
        if (base == null) {
            base = BigDecimal.ZERO;
        }
        // 基数受上下限限制
        if (config.getBaseLower() != null && base.compareTo(config.getBaseLower()) < 0) {
            base = config.getBaseLower();
        }
        if (config.getBaseUpper() != null && base.compareTo(config.getBaseUpper()) > 0) {
            base = config.getBaseUpper();
        }

        SocialSecurityCalculationVO vo = new SocialSecurityCalculationVO();
        vo.setBase(base.setScale(2, RoundingMode.HALF_UP));

        // 计算各险种
        vo.setPensionEmployer(calcAmount(base, config.getPensionEmployer()));
        vo.setPensionEmployee(calcAmount(base, config.getPensionEmployee()));
        vo.setMedicalEmployer(calcAmount(base, config.getMedicalEmployer()));
        vo.setMedicalEmployee(calcAmount(base, config.getMedicalEmployee()));
        vo.setUnemploymentEmployer(calcAmount(base, config.getUnemploymentEmployer()));
        vo.setUnemploymentEmployee(calcAmount(base, config.getUnemploymentEmployee()));
        vo.setInjuryEmployer(calcAmount(base, config.getInjuryEmployer()));
        vo.setMaternityEmployer(calcAmount(base, config.getMaternityEmployer()));
        vo.setHousingFundEmployer(calcAmount(base, config.getHousingFundEmployer()));
        vo.setHousingFundEmployee(calcAmount(base, config.getHousingFundEmployee()));

        // 单位部分合计 = 养老单位 + 医疗单位 + 失业单位 + 工伤单位 + 生育单位 + 公积金单位
        BigDecimal employerTotal = vo.getPensionEmployer()
                .add(vo.getMedicalEmployer())
                .add(vo.getUnemploymentEmployer())
                .add(vo.getInjuryEmployer())
                .add(vo.getMaternityEmployer())
                .add(vo.getHousingFundEmployer());
        vo.setEmployerTotal(employerTotal.setScale(2, RoundingMode.HALF_UP));

        // 个人部分合计 = 养老个人 + 医疗个人 + 失业个人 + 公积金个人
        BigDecimal employeeTotal = vo.getPensionEmployee()
                .add(vo.getMedicalEmployee())
                .add(vo.getUnemploymentEmployee())
                .add(vo.getHousingFundEmployee());
        vo.setEmployeeTotal(employeeTotal.setScale(2, RoundingMode.HALF_UP));

        // 总计
        vo.setTotal(employerTotal.add(employeeTotal).setScale(2, RoundingMode.HALF_UP));

        return vo;
    }

    /**
     * 计算单项金额 = 基数 * 比例
     */
    private BigDecimal calcAmount(BigDecimal base, BigDecimal rate) {
        if (rate == null) {
            return BigDecimal.ZERO;
        }
        return base.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private SocialSecurityConfigVO convertToVO(SocialSecurityConfig config) {
        SocialSecurityConfigVO vo = new SocialSecurityConfigVO();
        BeanUtil.copyProperties(config, vo);
        return vo;
    }
}
