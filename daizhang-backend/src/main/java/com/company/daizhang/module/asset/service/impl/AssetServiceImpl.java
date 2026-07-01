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
import com.company.daizhang.module.asset.vo.AssetCategoryStatVO;
import com.company.daizhang.module.asset.vo.AssetCategoryVO;
import com.company.daizhang.module.asset.vo.AssetDepreciationMonthlyVO;
import com.company.daizhang.module.asset.vo.AssetReportVO;
import com.company.daizhang.module.asset.vo.DepreciationRecordVO;
import com.company.daizhang.module.asset.vo.FixedAssetVO;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.voucher.dto.VoucherCreateRequest;
import com.company.daizhang.module.voucher.dto.VoucherDetailRequest;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import com.company.daizhang.module.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetServiceImpl extends ServiceImpl<FixedAssetMapper, FixedAsset> implements AssetService {

    private final AssetCategoryMapper assetCategoryMapper;
    private final DepreciationRecordMapper depreciationRecordMapper;
    private final VoucherService voucherService;
    private final VoucherMapper voucherMapper;
    private final SubjectMapper subjectMapper;

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

        // 排除categoryName:上面按categoryId查库写入的正确分类名会被copyProperties覆盖为null;
        // 同时排除status/accountSetId,状态变更走changeAssetStatus专用方法,accountSetId不可改
        BeanUtil.copyProperties(request, asset, "categoryName", "status", "accountSetId", "id");

        // 如果修改了折旧相关参数，重新计算月折旧额
        if (request.getDepreciationMethod() != null || request.getUsefulLife() != null || request.getResidualValue() != null) {
            BigDecimal monthlyDepreciation = calculateMonthlyDepreciation(
                    asset.getPurchaseAmount(),
                    asset.getNetValue(),
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

            // 并发折旧无锁修复:折旧前用 selectById 重新读取资产最新累计折旧/净值/version,
            // 不用方法入参 asset 列表中的旧值,避免并发触发月度折旧时同一资产被折旧两次、
            // 累计折旧翻倍、净值计算错误
            FixedAsset latest = this.getById(asset.getId());
            if (latest == null) {
                continue;
            }

            // 计算本次折旧
            // 批量折旧遍历账套所有在用资产,任一字段为null会导致整个批次NPE中断。
            // 与calculateMonthlyDepreciation保持一致的null防御。
            BigDecimal residualValue = latest.getResidualValue() != null ? latest.getResidualValue() : BigDecimal.ZERO;
            BigDecimal assetNetValue = latest.getNetValue() != null ? latest.getNetValue() : BigDecimal.ZERO;
            // 已提足折旧(净值<=残值)跳过,避免负折旧导致累计折旧回退、净值虚增
            if (assetNetValue.compareTo(residualValue) <= 0) {
                continue;
            }

            BigDecimal depreciationAmount;
            if ("双倍余额递减法".equals(latest.getDepreciationMethod())) {
                // 双倍余额递减法:基于当前净值计算,体现递减特性(原实现用原值,丧失递减)
                Integer usefulLife = latest.getUsefulLife();
                if (usefulLife == null || usefulLife <= 0) {
                    continue;
                }
                BigDecimal rate = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(usefulLife), 4, RoundingMode.HALF_UP);
                depreciationAmount = assetNetValue.multiply(rate)
                        .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            } else {
                depreciationAmount = latest.getMonthlyDepreciation();
                if (depreciationAmount == null) {
                    continue; // 月折旧额为空,数据不完整,跳过
                }
            }
            BigDecimal accumulatedDepreciation = (latest.getAccumulatedDeprecation() != null ? latest.getAccumulatedDeprecation() : BigDecimal.ZERO).add(depreciationAmount);
            BigDecimal netValue = assetNetValue.subtract(depreciationAmount);

            // 如果净值小于残值，则调整折旧额,使其恰好将净值降至残值
            if (netValue.compareTo(residualValue) < 0) {
                depreciationAmount = assetNetValue.subtract(residualValue);
                // 守卫:确保折旧额非负,避免累计折旧回退、净值虚增
                if (depreciationAmount.compareTo(BigDecimal.ZERO) < 0) {
                    depreciationAmount = BigDecimal.ZERO;
                }
                accumulatedDepreciation = (latest.getAccumulatedDeprecation() != null ? latest.getAccumulatedDeprecation() : BigDecimal.ZERO).add(depreciationAmount);
                netValue = residualValue;
            }

            // 创建折旧记录
            DepreciationRecord record = new DepreciationRecord();
            record.setAccountSetId(request.getAccountSetId());
            record.setAssetId(latest.getId());
            record.setAssetCode(latest.getAssetCode());
            record.setAssetName(latest.getAssetName());
            record.setYear(request.getYear());
            record.setMonth(request.getMonth());
            record.setDepreciationAmount(depreciationAmount);
            record.setAccumulatedDepreciation(accumulatedDepreciation);
            record.setNetValue(netValue);
            depreciationRecordMapper.insert(record);

            // 更新资产的累计折旧和净值,基于 latest 的 version 做乐观锁更新(BaseEntity.@Version
            // + OptimisticLockerInnerInterceptor)。并发折旧时另一线程已改 version,本次 updateById
            // 返回 false(影响行数0),抛异常回滚本次折旧记录插入,避免同一资产被折旧两次
            latest.setAccumulatedDeprecation(accumulatedDepreciation);
            latest.setNetValue(netValue);
            boolean updated = this.updateById(latest);
            if (!updated) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "资产折旧并发冲突，请重试");
            }
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

        // 动态查询折旧相关科目
        Long depreciationExpenseSubjectId = getDepreciationExpenseSubjectId(record.getAccountSetId());
        Long accumulatedDepreciationSubjectId = getAccumulatedDepreciationSubjectId(record.getAccountSetId());

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
        debitDetail.setSubjectId(depreciationExpenseSubjectId);
        debitDetail.setDebit(record.getDepreciationAmount());
        debitDetail.setCredit(BigDecimal.ZERO);
        debitDetail.setLineNo(1);
        details.add(debitDetail);

        // 贷方：累计折旧
        VoucherDetailRequest creditDetail = new VoucherDetailRequest();
        creditDetail.setSummary("计提折旧-" + record.getAssetName());
        creditDetail.setSubjectId(accumulatedDepreciationSubjectId);
        creditDetail.setDebit(BigDecimal.ZERO);
        creditDetail.setCredit(record.getDepreciationAmount());
        creditDetail.setLineNo(2);
        details.add(creditDetail);

        voucherRequest.setDetails(details);

        // 调用凭证服务创建凭证
        voucherService.createVoucher(voucherRequest);

        // 查询刚创建的凭证（按ID降序取最新一张）并回写voucherId到折旧记录，避免重复生成
        LambdaQueryWrapper<Voucher> vw = new LambdaQueryWrapper<>();
        vw.eq(Voucher::getAccountSetId, record.getAccountSetId())
          .eq(Voucher::getYear, record.getYear())
          .eq(Voucher::getMonth, record.getMonth())
          .orderByDesc(Voucher::getId)
          .last("LIMIT 1");
        Voucher createdVoucher = voucherMapper.selectOne(vw);
        if (createdVoucher != null) {
            record.setVoucherId(createdVoucher.getId());
            depreciationRecordMapper.updateById(record);
        }
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

    @Override
    public AssetReportVO getAssetReport(Long accountSetId, Integer year) {
        // 查询该账套所有资产
        LambdaQueryWrapper<FixedAsset> assetWrapper = new LambdaQueryWrapper<>();
        assetWrapper.eq(FixedAsset::getAccountSetId, accountSetId);
        List<FixedAsset> assets = this.list(assetWrapper);

        AssetReportVO vo = new AssetReportVO();
        vo.setTotalAssets(assets.size());

        // 原值合计、累计折旧、净值合计
        BigDecimal totalOriginalValue = assets.stream()
                .map(a -> a.getPurchaseAmount() != null ? a.getPurchaseAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAccumulatedDepreciation = assets.stream()
                .map(a -> a.getAccumulatedDeprecation() != null ? a.getAccumulatedDeprecation() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNetValue = assets.stream()
                .map(a -> a.getNetValue() != null ? a.getNetValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        vo.setTotalOriginalValue(totalOriginalValue);
        vo.setTotalAccumulatedDepreciation(totalAccumulatedDepreciation);
        vo.setTotalNetValue(totalNetValue);

        // 按分类统计
        Map<Long, List<FixedAsset>> categoryGroup = assets.stream()
                .filter(a -> a.getCategoryId() != null)
                .collect(Collectors.groupingBy(FixedAsset::getCategoryId));
        List<AssetCategoryStatVO> categoryStats = new ArrayList<>();
        for (Map.Entry<Long, List<FixedAsset>> entry : categoryGroup.entrySet()) {
            AssetCategoryStatVO stat = new AssetCategoryStatVO();
            stat.setCategoryId(entry.getKey());
            // 分类名称取第一条资产的分类名
            String categoryName = entry.getValue().stream()
                    .map(FixedAsset::getCategoryName)
                    .filter(StrUtil::isNotBlank)
                    .findFirst()
                    .orElse(null);
            if (StrUtil.isBlank(categoryName)) {
                AssetCategory category = assetCategoryMapper.selectById(entry.getKey());
                if (category != null) {
                    categoryName = category.getCategoryName();
                }
            }
            stat.setCategoryName(categoryName);
            stat.setAssetCount(entry.getValue().size());
            stat.setOriginalValue(entry.getValue().stream()
                    .map(a -> a.getPurchaseAmount() != null ? a.getPurchaseAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            stat.setAccumulatedDepreciation(entry.getValue().stream()
                    .map(a -> a.getAccumulatedDeprecation() != null ? a.getAccumulatedDeprecation() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            stat.setNetValue(entry.getValue().stream()
                    .map(a -> a.getNetValue() != null ? a.getNetValue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            categoryStats.add(stat);
        }
        vo.setCategoryStats(categoryStats);

        // 月折旧趋势（该年度各月折旧）
        LambdaQueryWrapper<DepreciationRecord> depWrapper = new LambdaQueryWrapper<>();
        depWrapper.eq(DepreciationRecord::getAccountSetId, accountSetId)
                .eq(DepreciationRecord::getYear, year)
                .orderByAsc(DepreciationRecord::getMonth);
        List<DepreciationRecord> depRecords = depreciationRecordMapper.selectList(depWrapper);

        Map<Integer, List<DepreciationRecord>> monthGroup = depRecords.stream()
                .filter(r -> r.getMonth() != null)
                .collect(Collectors.groupingBy(DepreciationRecord::getMonth));
        List<AssetDepreciationMonthlyVO> monthlyDepreciations = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            List<DepreciationRecord> monthRecords = monthGroup.get(m);
            if (monthRecords == null || monthRecords.isEmpty()) {
                continue;
            }
            AssetDepreciationMonthlyVO monthly = new AssetDepreciationMonthlyVO();
            monthly.setYear(year);
            monthly.setMonth(m);
            monthly.setDepreciationAmount(monthRecords.stream()
                    .map(r -> r.getDepreciationAmount() != null ? r.getDepreciationAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            // 累计折旧取该月最后一条记录的累计折旧
            BigDecimal lastAccumulated = monthRecords.stream()
                    .map(r -> r.getAccumulatedDepreciation() != null ? r.getAccumulatedDepreciation() : BigDecimal.ZERO)
                    .reduce((a, b) -> b)
                    .orElse(BigDecimal.ZERO);
            monthly.setAccumulatedDepreciation(lastAccumulated);
            monthlyDepreciations.add(monthly);
        }
        vo.setMonthlyDepreciations(monthlyDepreciations);

        return vo;
    }

    // ==================== 辅助方法 ====================

    /**
     * 查询累计折旧科目ID（编码1602：累计折旧）
     * 查不到时使用默认值1L并记录警告日志
     */
    private Long getAccumulatedDepreciationSubjectId(Long accountSetId) {
        // SubjectServiceImpl.initDefaultSubjects 中 1602=累计折旧, 1702=累计摊销
        return getSubjectIdByCode(accountSetId, "1602", 1L, "累计折旧");
    }

    /**
     * 查询折旧费用科目ID（编码5602：管理费用-折旧）
     * 查不到时使用默认值2L并记录警告日志
     */
    private Long getDepreciationExpenseSubjectId(Long accountSetId) {
        return getSubjectIdByCode(accountSetId, "5602", 2L, "折旧费用");
    }

    /**
     * 通过科目编码查询科目ID
     *
     * @param accountSetId 账套ID
     * @param code         科目编码
     * @param defaultId    查不到时的默认ID（可为null，表示不使用默认值继续向上返回）
     * @param subjectName  科目名称（用于日志）
     * @return 科目ID
     */
    private Long getSubjectIdByCode(Long accountSetId, String code, Long defaultId, String subjectName) {
        Subject subject = subjectMapper.selectOne(new LambdaQueryWrapper<Subject>()
                .eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getCode, code));
        if (subject != null) {
            return subject.getId();
        }
        if (defaultId != null) {
            log.warn("未查询到{}科目（编码: {}），账套ID: {}，使用默认科目ID: {}",
                    subjectName, code, accountSetId, defaultId);
            return defaultId;
        }
        return null;
    }

    /**
     * 计算月折旧额
     */
    private BigDecimal calculateMonthlyDepreciation(BigDecimal purchaseAmount, BigDecimal netValue, BigDecimal residualValue,
                                                     Integer usefulLife, String depreciationMethod) {
        // 参数防御:避免usefulLife为null拆箱NPE、为0除零异常、原值/残值为null NPE
        if (purchaseAmount == null || residualValue == null
                || usefulLife == null || usefulLife <= 0) {
            return BigDecimal.ZERO;
        }
        if ("直线法".equals(depreciationMethod)) {
            // 直线法：月折旧额 = (原值 - 残值) / 使用年限 / 12
            return purchaseAmount.subtract(residualValue)
                    .divide(BigDecimal.valueOf(usefulLife).multiply(BigDecimal.valueOf(12)), 2, RoundingMode.HALF_UP);
        } else if ("双倍余额递减法".equals(depreciationMethod)) {
            // 双倍余额递减法：年折旧率 = 2 / 使用年限 * 100%
            // 月折旧额 = 当前净值 * 年折旧率 / 12 (基于净值,体现递减特性;净值缺省回退原值)
            BigDecimal baseValue = (netValue != null ? netValue : purchaseAmount);
            BigDecimal rate = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(usefulLife), 4, RoundingMode.HALF_UP);
            return baseValue.multiply(rate)
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        } else if ("工作量法".equals(depreciationMethod)) {
            // 工作量法：简化为按直线法，月折旧额 = (原值 - 残值) / 使用年限 / 12
            return purchaseAmount.subtract(residualValue)
                    .divide(BigDecimal.valueOf(usefulLife).multiply(BigDecimal.valueOf(12)), 2, RoundingMode.HALF_UP);
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
