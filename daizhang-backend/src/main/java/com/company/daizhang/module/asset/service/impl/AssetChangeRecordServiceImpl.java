package com.company.daizhang.module.asset.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.asset.dto.AssetChangeRecordRequest;
import com.company.daizhang.module.asset.entity.AssetChangeRecord;
import com.company.daizhang.module.asset.mapper.AssetChangeRecordMapper;
import com.company.daizhang.module.asset.service.AssetChangeRecordService;
import com.company.daizhang.module.asset.vo.AssetChangeRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 资产变动记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetChangeRecordServiceImpl implements AssetChangeRecordService {

    private final AssetChangeRecordMapper assetChangeRecordMapper;

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
        AssetChangeRecord record = new AssetChangeRecord();
        BeanUtil.copyProperties(request, record);
        assetChangeRecordMapper.insert(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id) {
        AssetChangeRecord record = assetChangeRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "资产变动记录不存在");
        }
        assetChangeRecordMapper.deleteById(id);
    }

    private AssetChangeRecordVO convertToVO(AssetChangeRecord record) {
        AssetChangeRecordVO vo = new AssetChangeRecordVO();
        BeanUtil.copyProperties(record, vo);
        return vo;
    }
}
