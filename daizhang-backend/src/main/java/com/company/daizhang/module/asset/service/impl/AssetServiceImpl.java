package com.company.daizhang.module.asset.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.asset.dto.*;
import com.company.daizhang.module.asset.entity.AssetCategory;
import com.company.daizhang.module.asset.entity.DepreciationRecord;
import com.company.daizhang.module.asset.entity.FixedAsset;
import com.company.daizhang.module.asset.mapper.AssetCategoryMapper;
import com.company.daizhang.module.asset.mapper.DepreciationRecordMapper;
import com.company.daizhang.module.asset.mapper.FixedAssetMapper;
import com.company.daizhang.module.asset.service.AssetService;
import com.company.daizhang.module.asset.vo.AssetCategoryVO;
import com.company.daizhang.module.asset.vo.DepreciationRecordVO;
import com.company.daizhang.module.asset.vo.FixedAssetVO;
import com.company.daizhang.module.voucher.dto.VoucherCreateRequest;
import com.company.daizhang.module.voucher.dto.VoucherDetailRequest;
import com.company.daizhang.module.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 资产服务实现
 */
@Service
@RequiredArgsConstructor
public class AssetServiceImpl extends ServiceImpl<FixedAssetMapper, FixedAsset> implements AssetService {

    private final AssetCategoryMapper assetCategoryMapper;
    private final DepreciationRecordMapper depreciationRecordMapper;
    private final VoucherService voucherService;

    // ==================== 资产分类管理 ====================

    @Override
    public PageResult<AssetCategoryVO> pageCategories(AssetCategoryQueryRequest request) {
        Page<AssetCategory> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<AssetCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getAccountSetId() != null, AssetCategory::getAccountSetId, request.getAccountSetId())
               .like(StrUtil.isNotBlank(request.getCategoryCode()), AssetCategory::getCategoryCode, request.getCategoryCode())
               .like(StrUtil.isNotBlank(request.getCategoryName()), AssetCategory::getCategoryName, request.getCategoryName())
               .eq(StrUtil.isNotBlank(request.getDepreciationMethod()), AssetCategory::getDepreciationMethod, request.getDepreciationMethod())
               .eq(request.getParentId() != null, AssetCategory::getParentId, request.getParentId())
               .orderByAsc(AssetCategory::getCategoryCode);

        Page<AssetCategory> result = assetCategoryMapper.selectPage(page, wrapper);

        List<AssetCategoryVO> voList = result.getRecords().stream()
                .map(this::convertCategoryToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public AssetCategoryVO getCategoryById(Long id) {
        AssetCategory category = assetCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "资产分类不存在");
        }
        return convertCategoryToVO(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCategory(AssetCategoryCreateRequest request) {
        // 检查分类编码是否已存在
        LambdaQueryWrapper<AssetCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetCategory::getAccountSetId, request.getAccountSetId())
               .eq(AssetCategory::getCategoryCode, request.getCategoryCode());
        Long count = assetCategoryMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "分类编码已存在");
        }

        AssetCategory category = new AssetCategory();
        BeanUtil.copyProperties(request, category);
        assetCategoryMapper.insert(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long id, AssetCategoryUpdateRequest request) {
        AssetCategory category = assetCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "资产分类不存在");
        }

        BeanUtil.copyProperties(request, category);
        assetCategoryMapper.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        AssetCategory category = assetCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "资产分类不存在");
        }

        // 检查是否有子分类
        LambdaQueryWrapper<AssetCategory> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(AssetCategory::getParentId, id);
        Long childCount = assetCategoryMapper.selectCount(childWrapper);
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该分类下存在子分类，无法删除");
        }

        // 检查是否有资产使用该分类
        LambdaQueryWrapper<FixedAsset> assetWrapper = new LambdaQueryWrapper<>();
        assetWrapper.eq(FixedAsset::getCategoryId, id);
        Long assetCount = this.count(assetWrapper);
        if (assetCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该分类下存在资产，无法删除");
        }

        assetCategoryMapper.deleteById(id);
    }

    @Override
    public List<AssetCategoryVO> listCategoryTree(Long accountSetId) {
        LambdaQueryWrapper<AssetCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetCategory::getAccountSetId, accountSetId)
               .orderByAsc(AssetCategory::getCategoryCode);
        List<AssetCategory> categories = assetCategoryMapper.selectList(wrapper);

        return categories.stream()
                .map(this::convertCategoryToVO)
                .collect(Collectors.toList());
    }

    // ==================== 固定资产管理 ====================

    @Override
    public PageResult<FixedAssetVO> pageAssets(FixedAssetQueryRequest request) {
        Page<FixedAsset> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<FixedAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getAccountSetId() != null, FixedAsset::getAccountSetId, request.getAccountSetId())
               .like(StrUtil.isNotBlank(request.getAssetCode()), FixedAsset::getAssetCode, request.getAssetCode())
               .like(StrUtil.isNotBlank(request.getAssetName()), FixedAsset::getAssetName, request.getAssetName())
               .eq(request.getCategoryId() != null, FixedAsset::getCategoryId, request.getCategoryId())
               .eq(request.getStatus() != null, FixedAsset::getStatus, request.getStatus())
               .like(StrUtil.isNotBlank(request.getDepartment()), FixedAsset::getDepartment, request.getDepartment())
               .orderByDesc(FixedAsset::getCreateTime);

        Page<FixedAsset> result = this.page(page, wrapper);

        List<FixedAssetVO> voList = result.getRecords().stream()
                .map(this::convertAssetToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public FixedAssetVO getAssetById(Long id) {
        FixedAsset asset = this.getById(id);
        if (asset == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "固定资产不存在");
        }
        return convertAssetToVO(asset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAsset(FixedAssetCreateRequest request) {
        // 检查资产编码是否已存在
        LambdaQueryWrapper<FixedAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FixedAsset::getAccountSetId, request.getAccountSetId())
               .eq(FixedAsset::getAssetCode, request.getAssetCode());
        Long count = this.count(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "资产编码已存在");
        }

        // 获取分类信息
        AssetCategory category = assetCategoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "资产分类不存在");
        }

        FixedAsset asset = new FixedAsset();
        BeanUtil.copyProperties(request, asset);
        asset.setCategoryName(category.getCategoryName());
        asset.setStatus(0); // 在用
        asset.setAccumulatedDeprecation(BigDecimal.ZERO);
        asset.setNetValue(request.getPurchaseAmount());

        // 计算月折旧额
        BigDecimal monthlyDepreciation = calculateMonthlyDepreciation(
                request.getPurchaseAmount(),
                request.getResidualValue(),
                request.getUsefulLife(),
                request.getDepreciationMethod()
        );
        asset.setMonthlyDepreciation(monthlyDepreciation);

        this.save(asset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAsset(Long id, FixedAssetUpdateRequest request) {
        FixedAsset asset = this.getById(id);
        if (asset == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "固定资产不存在");
        }

        // 如果修改了分类，更新分类名称
        if (request.getCategoryId() != null && !request.getCategoryId().equals(asset.getCategoryId())) {
            AssetCategory category = assetCategoryMapper.selectById(request.getCategoryId());
            if (category == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "资产分类不存在");
            }
            asset.setCategoryName(category.getCategoryName());
        }

        BeanUtil.copyProperties(request, asset);

        // 如果修改了折旧相关参数，重新计算月折旧额
        if (request.getDepreciationMethod() != null || request.getUsefulLife() != null || request.getResidualValue() != null) {
            BigDecimal monthlyDepreciation = calculateMonthlyDepreciation(
                    asset.getPurchaseAmount(),
                    asset.getResidualValue(),
                    asset.getUsefulLife(),
                    asset.getDepreciationMethod()
            );
            asset.setMonthlyDepreciation(monthlyDepreciation);
        }

        this.updateById(asset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAsset(Long id) {
        FixedAsset asset = this.getById(id);
        if (asset == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "固定资产不存在");
        }

        // 检查是否有折旧记录
        LambdaQueryWrapper<DepreciationRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(DepreciationRecord::getAssetId, id);
        Long recordCount = depreciationRecordMapper.selectCount(recordWrapper);
        if (recordCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该资产存在折旧记录，无法删除");
        }

        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeAssetStatus(AssetStatusChangeRequest request) {
        FixedAsset asset = this.getById(request.getAssetId());
        if (asset == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "固定资产不存在");
        }

        asset.setStatus(request.getTargetStatus());
        if (StrUtil.isNotBlank(request.getRemark())) {
            asset.setRemark(request.getRemark());
        }
        this.updateById(asset);
    }

    // ==================== 折旧管理 ====================

    @Override
    public PageResult<DepreciationRecordVO> pageDepreciationRecords(DepreciationRecordQueryRequest request) {
        Page<DepreciationRecord> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<DepreciationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getAccountSetId() != null, DepreciationRecord::getAccountSetId, request.getAccountSetId())
               .eq(request.getAssetId() != null, DepreciationRecord::getAssetId, request.getAssetId())
               .like(StrUtil.isNotBlank(request.getAssetCode()), DepreciationRecord::getAssetCode, request.getAssetCode())
               .like(StrUtil.isNotBlank(request.getAssetName()), DepreciationRecord::getAssetName, request.getAssetName())
               .eq(request.getYear() != null, DepreciationRecord::getYear, request.getYear())
               .eq(request.getMonth() != null, DepreciationRecord::getMonth, request.getMonth())
               .orderByDesc(DepreciationRecord::getYear)
               .orderByDesc(DepreciationRecord::getMonth);

        Page<DepreciationRecord> result = depreciationRecordMapper.selectPage(page, wrapper);

        List<DepreciationRecordVO> voList = result.getRecords().stream()
                .map(this::convertRecordToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public DepreciationRecordVO getDepreciationRecordById(Long id) {
        DepreciationRecord record = depreciationRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "折旧记录不存在");
        }
        return convertRecordToVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateDepreciation(DepreciationRequest request) {
        // 查询所有在用资产
        LambdaQueryWrapper<FixedAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FixedAsset::getAccountSetId, request.getAccountSetId())
               .eq(FixedAsset::getStatus, 0); // 在用
        List<FixedAsset> assets = this.list(wrapper);

        for (FixedAsset asset : assets) {
            // 检查是否已存在该月的折旧记录
            LambdaQueryWrapper<DepreciationRecord> recordWrapper = new LambdaQueryWrapper<>();
            recordWrapper.eq(DepreciationRecord::getAssetId, asset.getId())
                        .eq(DepreciationRecord::getYear, request.getYear())
                        .eq(DepreciationRecord::getMonth, request.getMonth());
            Long count = depreciationRecordMapper.selectCount(recordWrapper);
            if (count > 0) {
                continue; // 已存在折旧记录，跳过
            }

            // 计算本次折旧
            BigDecimal depreciationAmount = asset.getMonthlyDepreciation();
            BigDecimal accumulatedDepreciation = asset.getAccumulatedDeprecation().add(depreciationAmount);
            BigDecimal netValue = asset.getNetValue().subtract(depreciationAmount);

            // 如果净值小于残值，则调整折旧额
            if (netValue.compareTo(asset.getResidualValue()) < 0) {
                depreciationAmount = asset.getNetValue().subtract(asset.getResidualValue());
                accumulatedDepreciation = asset.getAccumulatedDeprecation().add(depreciationAmount);
                netValue = asset.getResidualValue();
            }

            // 创建折旧记录
            DepreciationRecord record = new DepreciationRecord();
            record.setAccountSetId(request.getAccountSetId());
            record.setAssetId(asset.getId());
            record.setAssetCode(asset.getAssetCode());
            record.setAssetName(asset.getAssetName());
            record.setYear(request.getYear());
            record.setMonth(request.getMonth());
            record.setDepreciationAmount(depreciationAmount);
            record.setAccumulatedDepreciation(accumulatedDepreciation);
            record.setNetValue(netValue);
            depreciationRecordMapper.insert(record);

            // 更新资产的累计折旧和净值
            asset.setAccumulatedDeprecation(accumulatedDepreciation);
            asset.setNetValue(netValue);
            this.updateById(asset);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateDepreciationVoucher(Long recordId) {
        DepreciationRecord record = depreciationRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "折旧记录不存在");
        }

        if (record.getVoucherId() != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该折旧记录已生成凭证");
        }

        // 创建凭证
        VoucherCreateRequest voucherRequest = new VoucherCreateRequest();
        voucherRequest.setAccountSetId(record.getAccountSetId());
        voucherRequest.setVoucherDate(LocalDate.of(record.getYear(), record.getMonth(), 1));
        voucherRequest.setYear(record.getYear());
        voucherRequest.setMonth(record.getMonth());
        voucherRequest.setAttachmentCount(0);

        List<VoucherDetailRequest> details = new ArrayList<>();

        // 借方：管理费用-折旧费
        VoucherDetailRequest debitDetail = new VoucherDetailRequest();
        debitDetail.setSummary("计提折旧-" + record.getAssetName());
        debitDetail.setSubjectId(1L); // 需要查询科目ID，这里简化处理
        debitDetail.setDebit(record.getDepreciationAmount());
        debitDetail.setCredit(BigDecimal.ZERO);
        debitDetail.setLineNo(1);
        details.add(debitDetail);

        // 贷方：累计折旧
        VoucherDetailRequest creditDetail = new VoucherDetailRequest();
        creditDetail.setSummary("计提折旧-" + record.getAssetName());
        creditDetail.setSubjectId(2L); // 需要查询科目ID，这里简化处理
        creditDetail.setDebit(BigDecimal.ZERO);
        creditDetail.setCredit(record.getDepreciationAmount());
        creditDetail.setLineNo(2);
        details.add(creditDetail);

        voucherRequest.setDetails(details);

        // 调用凭证服务创建凭证
        voucherService.createVoucher(voucherRequest);

        // 注意：实际实现中需要获取创建的凭证ID并更新折旧记录
        // 这里简化处理，假设凭证创建成功
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchGenerateDepreciationVoucher(DepreciationRequest request) {
        // 查询该月的所有折旧记录
        LambdaQueryWrapper<DepreciationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DepreciationRecord::getAccountSetId, request.getAccountSetId())
               .eq(DepreciationRecord::getYear, request.getYear())
               .eq(DepreciationRecord::getMonth, request.getMonth())
               .isNull(DepreciationRecord::getVoucherId);
        List<DepreciationRecord> records = depreciationRecordMapper.selectList(wrapper);

        for (DepreciationRecord record : records) {
            generateDepreciationVoucher(record.getId());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 计算月折旧额
     */
    private BigDecimal calculateMonthlyDepreciation(BigDecimal purchaseAmount, BigDecimal residualValue,
                                                     Integer usefulLife, String depreciationMethod) {
        if ("直线法".equals(depreciationMethod)) {
            // 直线法：月折旧额 = (原值 - 残值) / 使用年限
            return purchaseAmount.subtract(residualValue)
                    .divide(BigDecimal.valueOf(usefulLife), 2, RoundingMode.HALF_UP);
        } else if ("双倍余额递减法".equals(depreciationMethod)) {
            // 双倍余额递减法：年折旧率 = 2 / 使用年限 * 100%
            // 月折旧额 = 原值 * 年折旧率 / 12
            BigDecimal rate = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(usefulLife), 4, RoundingMode.HALF_UP);
            return purchaseAmount.multiply(rate)
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        } else if ("工作量法".equals(depreciationMethod)) {
            // 工作量法：通常按实际工作量计算，这里简化为按直线法
            return purchaseAmount.subtract(residualValue)
                    .divide(BigDecimal.valueOf(usefulLife), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private AssetCategoryVO convertCategoryToVO(AssetCategory category) {
        AssetCategoryVO vo = new AssetCategoryVO();
        BeanUtil.copyProperties(category, vo);

        // 获取父分类名称
        if (category.getParentId() != null) {
            AssetCategory parent = assetCategoryMapper.selectById(category.getParentId());
            if (parent != null) {
                vo.setParentName(parent.getCategoryName());
            }
        }

        return vo;
    }

    private FixedAssetVO convertAssetToVO(FixedAsset asset) {
        FixedAssetVO vo = new FixedAssetVO();
        BeanUtil.copyProperties(asset, vo);

        // 状态名称
        if (asset.getStatus() != null) {
            switch (asset.getStatus()) {
                case 0:
                    vo.setStatusName("在用");
                    break;
                case 1:
                    vo.setStatusName("闲置");
                    break;
                case 2:
                    vo.setStatusName("报废");
                    break;
            }
        }

        return vo;
    }

    private DepreciationRecordVO convertRecordToVO(DepreciationRecord record) {
        DepreciationRecordVO vo = new DepreciationRecordVO();
        BeanUtil.copyProperties(record, vo);
        return vo;
    }
}
