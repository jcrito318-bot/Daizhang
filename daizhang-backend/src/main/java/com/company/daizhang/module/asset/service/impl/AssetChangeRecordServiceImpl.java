package com.company.daizhang.module.asset.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.asset.dto.AssetChangeRecordRequest;
import com.company.daizhang.module.asset.entity.AssetChangeRecord;
import com.company.daizhang.module.asset.entity.FixedAsset;
import com.company.daizhang.module.asset.mapper.AssetChangeRecordMapper;
import com.company.daizhang.module.asset.mapper.FixedAssetMapper;
import com.company.daizhang.module.asset.service.AssetChangeRecordService;
import com.company.daizhang.module.asset.vo.AssetChangeRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资产变动记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetChangeRecordServiceImpl implements AssetChangeRecordService {

    private final AccountSetAccessService accountSetAccessService;
    private final AssetChangeRecordMapper assetChangeRecordMapper;
    private final FixedAssetMapper fixedAssetMapper;

    @Override
    public PageResult<AssetChangeRecordVO> pageRecords(Long accountSetId, Long assetId, String changeType, int pageNum, int pageSize) {
        Page<AssetChangeRecord> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<AssetChangeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(accountSetId != null, AssetChangeRecord::getAccountSetId, accountSetId)
               .eq(assetId != null, AssetChangeRecord::getAssetId, assetId)
               .eq(StrUtil.isNotBlank(changeType), AssetChangeRecord::getChangeType, changeType)
               .orderByDesc(AssetChangeRecord::getChangeDate)
               .orderByDesc(AssetChangeRecord::getCreateTime);

        Page<AssetChangeRecord> result = assetChangeRecordMapper.selectPage(page, wrapper);

        List<AssetChangeRecordVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), pageNum, pageSize);
    }

    @Override
    public List<AssetChangeRecordVO> listByAssetId(Long assetId) {
        LambdaQueryWrapper<AssetChangeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetChangeRecord::getAssetId, assetId)
               .orderByDesc(AssetChangeRecord::getChangeDate)
               .orderByDesc(AssetChangeRecord::getCreateTime);

        List<AssetChangeRecord> list = assetChangeRecordMapper.selectList(wrapper);

        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRecord(AssetChangeRecordRequest request) {
        // IDOR治理:校验当前用户对该账套的所有者权限
        accountSetAccessService.checkOwner(request.getAccountSetId());
        AssetChangeRecord record = new AssetChangeRecord();
        BeanUtil.copyProperties(request, record);
        assetChangeRecordMapper.insert(record);

        // 联动更新固定资产档案：原 createRecord 仅记录日志，资产档案永不更新，导致变动记录形同虚设。
        // 此处按 changeType 同步更新资产档案对应字段（原值/部门/使用状况/使用年限）。
        syncFixedAssetFromChangeRecord(record);
    }

    /**
     * 根据变动记录同步更新固定资产档案对应字段。
     * <p>
     * changeType 支持中文（购入/出售/报废/调拨/盘亏）及语义编码
     * （原值变更/部门变更/使用状况变更/使用年限变更）两种形式，按字段映射更新：
     * <ul>
     *   <li>原值变更/购入 → 更新 FixedAsset.purchaseAmount（取 changeAmount 作为新原值）</li>
     *   <li>部门变更/调拨 → 更新 FixedAsset.department（取 toDepartment 作为新部门）</li>
     *   <li>使用状况变更/报废/出售/盘亏 → 更新 FixedAsset.status（置为已处置=2）</li>
     *   <li>使用年限变更 → 更新 FixedAsset.usefulLife（取 changeAmount 整数部分）</li>
     * </ul>
     * 变动记录本身仍保留，审计逻辑不受影响。
     */
    private void syncFixedAssetFromChangeRecord(AssetChangeRecord record) {
        if (record.getAssetId() == null) {
            return;
        }
        FixedAsset asset = fixedAssetMapper.selectById(record.getAssetId());
        if (asset == null) {
            log.warn("资产变动记录关联的固定资产不存在，assetId: {}", record.getAssetId());
            return;
        }

        String changeType = record.getChangeType();
        if (StrUtil.isBlank(changeType)) {
            return;
        }

        boolean needUpdate = false;

        // 原值变更：更新购入金额（原值）。变动金额作为变动后的新原值
        if (isOriginalValueChange(changeType) && record.getChangeAmount() != null) {
            asset.setPurchaseAmount(record.getChangeAmount());
            // 同步重算净值（原值 - 累计折旧）
            BigDecimal accumulatedDep = asset.getAccumulatedDeprecation() != null
                    ? asset.getAccumulatedDeprecation() : BigDecimal.ZERO;
            asset.setNetValue(record.getChangeAmount().subtract(accumulatedDep));
            needUpdate = true;
        }

        // 部门变更：更新使用部门为变动后部门
        if (isDepartmentChange(changeType) && StrUtil.isNotBlank(record.getToDepartment())) {
            asset.setDepartment(record.getToDepartment());
            needUpdate = true;
        }

        // 使用状况变更：报废/出售/盘亏等处置类变动，状态置为已处置(2)
        if (isUsageStatusChange(changeType)) {
            asset.setStatus(2);
            needUpdate = true;
        }

        // 使用年限变更：变动金额整数部分作为新使用年限
        if (isUsefulLifeChange(changeType) && record.getChangeAmount() != null) {
            asset.setUsefulLife(record.getChangeAmount().intValue());
            needUpdate = true;
        }

        if (needUpdate) {
            fixedAssetMapper.updateById(asset);
            log.info("联动更新固定资产档案，assetId: {}, changeType: {}", record.getAssetId(), changeType);
        }
    }

    /**
     * 是否为原值变更类变动
     */
    private boolean isOriginalValueChange(String changeType) {
        return "原值变更".equals(changeType)
                || "originalValueChange".equalsIgnoreCase(changeType)
                || "购入".equals(changeType)
                || "重估".equals(changeType);
    }

    /**
     * 是否为部门变更类变动
     */
    private boolean isDepartmentChange(String changeType) {
        return "部门变更".equals(changeType)
                || "departmentChange".equalsIgnoreCase(changeType)
                || "调拨".equals(changeType);
    }

    /**
     * 是否为使用状况变更类变动（报废/出售/盘亏等处置类）
     */
    private boolean isUsageStatusChange(String changeType) {
        return "使用状况变更".equals(changeType)
                || "usageStatusChange".equalsIgnoreCase(changeType)
                || "报废".equals(changeType)
                || "出售".equals(changeType)
                || "盘亏".equals(changeType);
    }

    /**
     * 是否为使用年限变更类变动
     */
    private boolean isUsefulLifeChange(String changeType) {
        return "使用年限变更".equals(changeType)
                || "usefulLifeChange".equalsIgnoreCase(changeType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id) {
        AssetChangeRecord record = assetChangeRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "资产变动记录不存在");
        }
        // IDOR治理:校验当前用户对该资产变动记录所属账套的所有者权限
        accountSetAccessService.checkOwner(record.getAccountSetId());
        assetChangeRecordMapper.deleteById(id);
    }

    private AssetChangeRecordVO convertToVO(AssetChangeRecord record) {
        AssetChangeRecordVO vo = new AssetChangeRecordVO();
        BeanUtil.copyProperties(record, vo);
        return vo;
    }
}
