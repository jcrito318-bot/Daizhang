package com.company.daizhang.module.crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.crm.dto.OpportunityQueryRequest;
import com.company.daizhang.module.crm.dto.OpportunityRequest;
import com.company.daizhang.module.crm.entity.Opportunity;
import com.company.daizhang.module.crm.mapper.OpportunityMapper;
import com.company.daizhang.module.crm.service.OpportunityService;
import com.company.daizhang.module.crm.vo.OpportunityStatisticsVO;
import com.company.daizhang.module.crm.vo.OpportunityVO;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商机服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpportunityServiceImpl extends ServiceImpl<OpportunityMapper, Opportunity> implements OpportunityService {

    private final SysUserMapper sysUserMapper;

    /**
     * 合法的商机阶段
     */
    private static final List<String> VALID_STAGES = Arrays.asList("线索", "跟进", "报价", "谈判", "成交", "流失");

    @Override
    public PageResult<OpportunityVO> pageOpportunities(OpportunityQueryRequest request) {
        Page<Opportunity> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<Opportunity> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(request.getOpportunityName()), Opportunity::getOpportunityName, request.getOpportunityName())
               .eq(StrUtil.isNotBlank(request.getStage()), Opportunity::getStage, request.getStage())
               .eq(request.getAssigneeId() != null, Opportunity::getAssigneeId, request.getAssigneeId())
               .orderByDesc(Opportunity::getCreateTime);

        Page<Opportunity> result = this.page(page, wrapper);

        List<OpportunityVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public OpportunityVO getOpportunityById(Long id) {
        Opportunity opportunity = this.getById(id);
        if (opportunity == null) {
            throw new BusinessException(404, "商机不存在");
        }
        return convertToVO(opportunity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOpportunity(OpportunityRequest request) {
        // 校验阶段
        validateStage(request.getStage());

        Opportunity opportunity = new Opportunity();
        BeanUtil.copyProperties(request, opportunity);
        // 新建商机默认为线索阶段
        if (StrUtil.isBlank(opportunity.getStage())) {
            opportunity.setStage("线索");
        }
        // 预计金额默认为0
        if (opportunity.getExpectedAmount() == null) {
            opportunity.setExpectedAmount(BigDecimal.ZERO);
        }
        // 根据负责人ID填充负责人姓名
        fillAssigneeName(opportunity);
        this.save(opportunity);

        log.info("创建商机成功，商机名称: {}", opportunity.getOpportunityName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOpportunity(Long id, OpportunityRequest request) {
        Opportunity opportunity = this.getById(id);
        if (opportunity == null) {
            throw new BusinessException(404, "商机不存在");
        }

        // 校验阶段
        validateStage(request.getStage());

        // 使用ignoreNullValue避免request中stage为null时把已有stage清空,导致商机阶段丢失、销售漏斗统计错误
        BeanUtil.copyProperties(request, opportunity, cn.hutool.core.bean.copier.CopyOptions.create().ignoreNullValue());
        opportunity.setId(id);
        // 根据负责人ID填充负责人姓名
        fillAssigneeName(opportunity);
        this.updateById(opportunity);

        log.info("更新商机成功，商机ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOpportunity(Long id) {
        Opportunity opportunity = this.getById(id);
        if (opportunity == null) {
            throw new BusinessException(404, "商机不存在");
        }
        this.removeById(id);
        log.info("删除商机成功，商机ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStage(Long id, String stage) {
        Opportunity opportunity = this.getById(id);
        if (opportunity == null) {
            throw new BusinessException(404, "商机不存在");
        }
        // 校验阶段
        validateStage(stage);

        opportunity.setStage(stage);
        this.updateById(opportunity);
        log.info("变更商机阶段成功，商机ID: {}, 阶段: {}", id, stage);
    }

    @Override
    public OpportunityStatisticsVO getStatistics() {
        // 查询全部商机
        List<Opportunity> list = this.list();

        OpportunityStatisticsVO vo = new OpportunityStatisticsVO();
        vo.setTotalCount((long) list.size());

        long clueCount = list.stream().filter(o -> "线索".equals(o.getStage())).count();
        long followingCount = list.stream().filter(o -> "跟进".equals(o.getStage())).count();
        long quotationCount = list.stream().filter(o -> "报价".equals(o.getStage())).count();
        long negotiationCount = list.stream().filter(o -> "谈判".equals(o.getStage())).count();
        long wonCount = list.stream().filter(o -> "成交".equals(o.getStage())).count();
        long lostCount = list.stream().filter(o -> "流失".equals(o.getStage())).count();

        vo.setClueCount(clueCount);
        vo.setFollowingCount(followingCount);
        vo.setQuotationCount(quotationCount);
        vo.setNegotiationCount(negotiationCount);
        vo.setWonCount(wonCount);
        vo.setLostCount(lostCount);

        // 成交率 = 成交数 / (成交数 + 流失数) * 100
        long closedTotal = wonCount + lostCount;
        BigDecimal winRate = BigDecimal.ZERO;
        if (closedTotal > 0) {
            winRate = BigDecimal.valueOf(wonCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(closedTotal), 2, RoundingMode.HALF_UP);
        }
        vo.setWinRate(winRate);

        // 预计金额合计
        BigDecimal totalExpectedAmount = list.stream()
                .map(o -> o.getExpectedAmount() != null ? o.getExpectedAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalExpectedAmount(totalExpectedAmount);

        // 成交金额合计
        BigDecimal totalWonAmount = list.stream()
                .filter(o -> "成交".equals(o.getStage()))
                .map(o -> o.getExpectedAmount() != null ? o.getExpectedAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalWonAmount(totalWonAmount);

        return vo;
    }

    /**
     * 校验阶段是否合法
     */
    private void validateStage(String stage) {
        if (StrUtil.isNotBlank(stage) && !VALID_STAGES.contains(stage)) {
            throw new BusinessException(400, "商机阶段不合法，可选值：线索/跟进/报价/谈判/成交/流失");
        }
    }

    /**
     * 根据负责人ID填充负责人姓名
     */
    private void fillAssigneeName(Opportunity opportunity) {
        if (opportunity.getAssigneeId() != null && StrUtil.isBlank(opportunity.getAssigneeName())) {
            SysUser user = sysUserMapper.selectById(opportunity.getAssigneeId());
            if (user != null) {
                opportunity.setAssigneeName(user.getRealName() != null ? user.getRealName() : user.getUsername());
            }
        }
    }

    /**
     * 商机实体转VO
     */
    private OpportunityVO convertToVO(Opportunity opportunity) {
        OpportunityVO vo = new OpportunityVO();
        BeanUtil.copyProperties(opportunity, vo);
        return vo;
    }
}
