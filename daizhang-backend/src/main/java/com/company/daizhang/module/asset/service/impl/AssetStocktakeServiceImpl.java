package com.company.daizhang.module.asset.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.asset.dto.AssetStocktakeQueryRequest;
import com.company.daizhang.module.asset.dto.AssetStocktakeRequest;
import com.company.daizhang.module.asset.entity.AssetStocktake;
import com.company.daizhang.module.asset.entity.AssetStocktakeDetail;
import com.company.daizhang.module.asset.entity.FixedAsset;
import com.company.daizhang.module.asset.mapper.AssetStocktakeDetailMapper;
import com.company.daizhang.module.asset.mapper.AssetStocktakeMapper;
import com.company.daizhang.module.asset.mapper.FixedAssetMapper;
import com.company.daizhang.module.asset.service.AssetStocktakeService;
import com.company.daizhang.module.asset.vo.AssetStocktakeDetailVO;
import com.company.daizhang.module.asset.vo.AssetStocktakeVO;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.voucher.dto.VoucherCreateRequest;
import com.company.daizhang.module.voucher.dto.VoucherDetailRequest;
import com.company.daizhang.module.voucher.mapper.VoucherDetailMapper;
import com.company.daizhang.module.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资产盘点服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetStocktakeServiceImpl implements AssetStocktakeService {

    private final AssetStocktakeMapper assetStocktakeMapper;
    private final AssetStocktakeDetailMapper assetStocktakeDetailMapper;
    private final FixedAssetMapper fixedAssetMapper;
    private final SubjectMapper subjectMapper;
    private final VoucherService voucherService;
    private final VoucherDetailMapper voucherDetailMapper;

    // 待处理财产损溢科目编码(1901)，固定资产科目编码(1601)，累计折旧科目编码(1602)
    private static final String CODE_PENDING_ASSET_LOSS = "1901";
    private static final String CODE_FIXED_ASSET = "1601";
    private static final String CODE_ACCUMULATED_DEP = "1602";

    private static final String SCOPE_ALL = "ALL";
    private static final String SCOPE_CATEGORY = "CATEGORY";
    private static final String SCOPE_SPECIFIC = "SPECIFIC";

    private static final String RESULT_MATCH = "MATCH";
    private static final String RESULT_LOSS = "LOSS";
    private static final String RESULT_GAIN = "GAIN";

    @Override
    public PageResult<AssetStocktakeVO> pageStocktakes(AssetStocktakeQueryRequest request) {
        Page<AssetStocktake> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<AssetStocktake> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getAccountSetId() != null, AssetStocktake::getAccountSetId, request.getAccountSetId())
                .like(StrUtil.isNotBlank(request.getStocktakeNo()), AssetStocktake::getStocktakeNo, request.getStocktakeNo())
                .like(StrUtil.isNotBlank(request.getStocktakeName()), AssetStocktake::getStocktakeName, request.getStocktakeName())
                .eq(request.getStatus() != null, AssetStocktake::getStatus, request.getStatus())
                .orderByDesc(AssetStocktake::getCreateTime);

        Page<AssetStocktake> result = assetStocktakeMapper.selectPage(page, wrapper);

        List<AssetStocktakeVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public AssetStocktakeVO getStocktakeById(Long id) {
        AssetStocktake stocktake = assetStocktakeMapper.selectById(id);
        if (stocktake == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "盘点单不存在");
        }
        AssetStocktakeVO vo = convertToVO(stocktake);

        // 查询明细
        LambdaQueryWrapper<AssetStocktakeDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(AssetStocktakeDetail::getStocktakeId, id)
                .orderByAsc(AssetStocktakeDetail::getId);
        List<AssetStocktakeDetail> details = assetStocktakeDetailMapper.selectList(detailWrapper);
        vo.setDetails(details.stream().map(this::convertDetailToVO).collect(Collectors.toList()));

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStocktake(AssetStocktakeRequest request) {
        // 查询符合条件的资产
        List<FixedAsset> assets = queryAssetsForStocktake(request);
        if (assets.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "所选范围内无可用固定资产");
        }

        // 创建盘点单
        AssetStocktake stocktake = new AssetStocktake();
        BeanUtil.copyProperties(request, stocktake);
        if (stocktake.getStocktakeDate() == null) {
            stocktake.setStocktakeDate(LocalDate.now());
        }
        if (StrUtil.isBlank(stocktake.getScope())) {
            stocktake.setScope(SCOPE_ALL);
        }
        stocktake.setStatus(0); // 进行中
        stocktake.setTotalCount(assets.size());
        stocktake.setLossCount(0);
        stocktake.setGainCount(0);
        stocktake.setMatchCount(0);
        // 生成盘点单号
        stocktake.setStocktakeNo(generateStocktakeNo());

        assetStocktakeMapper.insert(stocktake);
        Long stocktakeId = stocktake.getId();

        // 生成明细，账面数据带入
        for (FixedAsset asset : assets) {
            AssetStocktakeDetail detail = new AssetStocktakeDetail();
            detail.setStocktakeId(stocktakeId);
            detail.setAssetId(asset.getId());
            detail.setAssetCode(asset.getAssetCode());
            detail.setAssetName(asset.getAssetName());
            // 账面数量默认1台
            detail.setBookQuantity(BigDecimal.ONE);
            detail.setActualQuantity(BigDecimal.ONE);
            // 账面原值=购入金额
            BigDecimal bookValue = asset.getPurchaseAmount() != null ? asset.getPurchaseAmount() : BigDecimal.ZERO;
            detail.setBookValue(bookValue);
            detail.setActualValue(bookValue);
            detail.setDiffQuantity(BigDecimal.ZERO);
            detail.setDiffAmount(BigDecimal.ZERO);
            detail.setResult(RESULT_MATCH);
            detail.setHandleOpinion("");
            assetStocktakeDetailMapper.insert(detail);
        }

        return stocktakeId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStocktake(Long id) {
        AssetStocktake stocktake = assetStocktakeMapper.selectById(id);
        if (stocktake == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "盘点单不存在");
        }
        if (stocktake.getStatus() != null && stocktake.getStatus() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "已完成的盘点单不可删除");
        }
        // 删除明细
        LambdaQueryWrapper<AssetStocktakeDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(AssetStocktakeDetail::getStocktakeId, id);
        assetStocktakeDetailMapper.delete(detailWrapper);
        assetStocktakeMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inputActualData(Long detailId, BigDecimal actualQuantity, BigDecimal actualValue, String handleOpinion) {
        AssetStocktakeDetail detail = assetStocktakeDetailMapper.selectById(detailId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "盘点明细不存在");
        }

        BigDecimal bookQty = detail.getBookQuantity() != null ? detail.getBookQuantity() : BigDecimal.ZERO;
        BigDecimal bookVal = detail.getBookValue() != null ? detail.getBookValue() : BigDecimal.ZERO;
        BigDecimal actQty = actualQuantity != null ? actualQuantity : BigDecimal.ZERO;
        BigDecimal actVal = actualValue != null ? actualValue : BigDecimal.ZERO;

        // 计算差异
        BigDecimal diffQty = actQty.subtract(bookQty);
        BigDecimal diffAmt = actVal.subtract(bookVal);

        // 计算结果：优先按数量差异判断,数量一致时按金额差异判断(价值重估场景)
        String result;
        int cmpQty = diffQty.compareTo(BigDecimal.ZERO);
        int cmpAmt = diffAmt.compareTo(BigDecimal.ZERO);
        if (cmpQty < 0 || (cmpQty == 0 && cmpAmt < 0)) {
            result = RESULT_LOSS;
        } else if (cmpQty > 0 || (cmpQty == 0 && cmpAmt > 0)) {
            result = RESULT_GAIN;
        } else {
            result = RESULT_MATCH;
        }

        detail.setActualQuantity(actQty);
        detail.setActualValue(actVal);
        detail.setDiffQuantity(diffQty);
        detail.setDiffAmount(diffAmt);
        detail.setResult(result);
        if (handleOpinion != null) {
            detail.setHandleOpinion(handleOpinion);
        }
        assetStocktakeDetailMapper.updateById(detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeStocktake(Long id) {
        AssetStocktake stocktake = assetStocktakeMapper.selectById(id);
        if (stocktake == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "盘点单不存在");
        }
        if (stocktake.getStatus() != null && stocktake.getStatus() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "盘点单已完成");
        }

        // 汇总盘盈盘亏数量
        LambdaQueryWrapper<AssetStocktakeDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(AssetStocktakeDetail::getStocktakeId, id);
        List<AssetStocktakeDetail> details = assetStocktakeDetailMapper.selectList(detailWrapper);

        int lossCount = 0;
        int gainCount = 0;
        int matchCount = 0;
        for (AssetStocktakeDetail d : details) {
            if (RESULT_LOSS.equals(d.getResult())) {
                lossCount++;
            } else if (RESULT_GAIN.equals(d.getResult())) {
                gainCount++;
            } else {
                matchCount++;
            }
        }

        stocktake.setTotalCount(details.size());
        stocktake.setLossCount(lossCount);
        stocktake.setGainCount(gainCount);
        stocktake.setMatchCount(matchCount);
        stocktake.setStatus(1); // 已完成
        assetStocktakeMapper.updateById(stocktake);
    }

    // ==================== 辅助方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateStocktakeVoucher(Long id) {
        AssetStocktake stocktake = assetStocktakeMapper.selectById(id);
        if (stocktake == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "盘点单不存在");
        }
        if (stocktake.getStatus() == null || stocktake.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "盘点单未完成，无法生成调账凭证");
        }

        // 校验是否已生成过调账凭证,避免重复生成(通过盘点名称匹配摘要)
        LambdaQueryWrapper<com.company.daizhang.module.voucher.entity.VoucherDetail> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.like(com.company.daizhang.module.voucher.entity.VoucherDetail::getSummary,
                "-" + stocktake.getStocktakeName());
        Long existCount = voucherDetailMapper.selectCount(existWrapper);
        if (existCount != null && existCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该盘点单已生成调账凭证,不能重复生成");
        }


        // 查询差异明细
        LambdaQueryWrapper<AssetStocktakeDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(AssetStocktakeDetail::getStocktakeId, id);
        List<AssetStocktakeDetail> details = assetStocktakeDetailMapper.selectList(detailWrapper);

        // 分别汇总盘盈、盘亏差异金额
        BigDecimal gainAmount = BigDecimal.ZERO;
        BigDecimal lossAmount = BigDecimal.ZERO;
        for (AssetStocktakeDetail d : details) {
            BigDecimal diff = d.getDiffAmount() != null ? d.getDiffAmount() : BigDecimal.ZERO;
            if (RESULT_GAIN.equals(d.getResult()) && diff.compareTo(BigDecimal.ZERO) > 0) {
                gainAmount = gainAmount.add(diff);
            } else if (RESULT_LOSS.equals(d.getResult()) && diff.compareTo(BigDecimal.ZERO) < 0) {
                lossAmount = lossAmount.add(diff.abs());
            }
        }

        if (gainAmount.compareTo(BigDecimal.ZERO) == 0 && lossAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "无盘盈盘亏差异，无需生成调账凭证");
        }

        // 获取科目
        Long pendingLossSubjectId = getSubjectIdByCode(stocktake.getAccountSetId(), CODE_PENDING_ASSET_LOSS);
        Long fixedAssetSubjectId = getSubjectIdByCode(stocktake.getAccountSetId(), CODE_FIXED_ASSET);

        // 构建凭证
        VoucherCreateRequest voucherRequest = new VoucherCreateRequest();
        voucherRequest.setAccountSetId(stocktake.getAccountSetId());
        LocalDate voucherDate = stocktake.getStocktakeDate() != null ? stocktake.getStocktakeDate() : LocalDate.now();
        voucherRequest.setVoucherDate(voucherDate);
        voucherRequest.setYear(voucherDate.getYear());
        voucherRequest.setMonth(voucherDate.getMonthValue());
        voucherRequest.setAttachmentCount(0);

        List<VoucherDetailRequest> voucherDetails = new ArrayList<>();
        int lineNo = 1;

        // 盘亏：借-待处理财产损溢  贷-固定资产
        if (lossAmount.compareTo(BigDecimal.ZERO) > 0) {
            VoucherDetailRequest debit = new VoucherDetailRequest();
            debit.setSummary("资产盘亏转入待处理-" + stocktake.getStocktakeName());
            debit.setSubjectId(pendingLossSubjectId);
            debit.setDebit(lossAmount);
            debit.setCredit(BigDecimal.ZERO);
            debit.setLineNo(lineNo++);
            voucherDetails.add(debit);

            VoucherDetailRequest credit = new VoucherDetailRequest();
            credit.setSummary("资产盘亏减少固定资产-" + stocktake.getStocktakeName());
            credit.setSubjectId(fixedAssetSubjectId);
            credit.setDebit(BigDecimal.ZERO);
            credit.setCredit(lossAmount);
            credit.setLineNo(lineNo++);
            voucherDetails.add(credit);
        }

        // 盘盈：借-固定资产  贷-待处理财产损溢
        if (gainAmount.compareTo(BigDecimal.ZERO) > 0) {
            VoucherDetailRequest debit = new VoucherDetailRequest();
            debit.setSummary("资产盘盈增加固定资产-" + stocktake.getStocktakeName());
            debit.setSubjectId(fixedAssetSubjectId);
            debit.setDebit(gainAmount);
            debit.setCredit(BigDecimal.ZERO);
            debit.setLineNo(lineNo++);
            voucherDetails.add(debit);

            VoucherDetailRequest credit = new VoucherDetailRequest();
            credit.setSummary("资产盘盈转入待处理-" + stocktake.getStocktakeName());
            credit.setSubjectId(pendingLossSubjectId);
            credit.setDebit(BigDecimal.ZERO);
            credit.setCredit(gainAmount);
            credit.setLineNo(lineNo++);
            voucherDetails.add(credit);
        }

        voucherRequest.setDetails(voucherDetails);
        voucherService.createVoucher(voucherRequest);

        // 查询刚创建的凭证ID(通过摘要匹配盘点名称)
        LambdaQueryWrapper<com.company.daizhang.module.voucher.entity.VoucherDetail> createdWrapper = new LambdaQueryWrapper<>();
        createdWrapper.like(com.company.daizhang.module.voucher.entity.VoucherDetail::getSummary,
                "-" + stocktake.getStocktakeName())
                .orderByDesc(com.company.daizhang.module.voucher.entity.VoucherDetail::getVoucherId)
                .last("LIMIT 1");
        com.company.daizhang.module.voucher.entity.VoucherDetail createdDetail = voucherDetailMapper.selectOne(createdWrapper);
        Long voucherId = createdDetail != null ? createdDetail.getVoucherId() : 0L;

        log.info("盘点单 {} 生成调账凭证成功，凭证ID={}, 盘亏={}, 盘盈={}", id, voucherId, lossAmount, gainAmount);
        return voucherId;
    }

    /**
     * 按科目编码获取科目ID（含前缀匹配容错）
     */
    private Long getSubjectIdByCode(Long accountSetId, String code) {
        // 精确匹配
        LambdaQueryWrapper<Subject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getCode, code)
                .eq(Subject::getStatus, 1);
        Subject subject = subjectMapper.selectOne(wrapper);
        if (subject != null) {
            return subject.getId();
        }
        // 前缀匹配（取编码第一段）
        String prefix = code.substring(0, 4);
        LambdaQueryWrapper<Subject> prefixWrapper = new LambdaQueryWrapper<>();
        prefixWrapper.eq(Subject::getAccountSetId, accountSetId)
                .likeRight(Subject::getCode, prefix)
                .eq(Subject::getStatus, 1)
                .orderByAsc(Subject::getCode)
                .last("LIMIT 1");
        Subject fallback = subjectMapper.selectOne(prefixWrapper);
        if (fallback == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(),
                    "未找到科目编码" + code + "（或前缀" + prefix + "）对应的科目，请先初始化科目");
        }
        return fallback.getId();
    }

    // ==================== 原辅助方法 ====================

    private List<FixedAsset> queryAssetsForStocktake(AssetStocktakeRequest request) {
        LambdaQueryWrapper<FixedAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FixedAsset::getAccountSetId, request.getAccountSetId())
                .ne(FixedAsset::getStatus, 2); // 排除报废资产

        String scope = StrUtil.isBlank(request.getScope()) ? SCOPE_ALL : request.getScope();
        switch (scope) {
            case SCOPE_CATEGORY:
                if (request.getCategoryId() == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "按分类盘点时分类ID不能为空");
                }
                wrapper.eq(FixedAsset::getCategoryId, request.getCategoryId());
                break;
            case SCOPE_SPECIFIC:
                if (request.getAssetIds() == null || request.getAssetIds().isEmpty()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "指定资产盘点时资产ID列表不能为空");
                }
                wrapper.in(FixedAsset::getId, request.getAssetIds());
                break;
            case SCOPE_ALL:
            default:
                // 全部资产，不加额外条件
                break;
        }
        return fixedAssetMapper.selectList(wrapper);
    }

    private String generateStocktakeNo() {
        return "PD" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + System.currentTimeMillis() % 10000;
    }

    private AssetStocktakeVO convertToVO(AssetStocktake stocktake) {
        AssetStocktakeVO vo = new AssetStocktakeVO();
        BeanUtil.copyProperties(stocktake, vo);
        vo.setScopeDesc(convertScopeDesc(stocktake.getScope()));
        vo.setStatusDesc(convertStatusDesc(stocktake.getStatus()));
        return vo;
    }

    private AssetStocktakeDetailVO convertDetailToVO(AssetStocktakeDetail detail) {
        AssetStocktakeDetailVO vo = new AssetStocktakeDetailVO();
        BeanUtil.copyProperties(detail, vo);
        vo.setResultDesc(convertResultDesc(detail.getResult()));
        return vo;
    }

    private String convertScopeDesc(String scope) {
        if (scope == null) return "";
        switch (scope) {
            case SCOPE_ALL: return "全部资产";
            case SCOPE_CATEGORY: return "按分类";
            case SCOPE_SPECIFIC: return "指定资产";
            default: return scope;
        }
    }

    private String convertStatusDesc(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "进行中";
            case 1: return "已完成";
            case 2: return "已作废";
            default: return String.valueOf(status);
        }
    }

    private String convertResultDesc(String result) {
        if (result == null) return "";
        switch (result) {
            case RESULT_MATCH: return "一致";
            case RESULT_LOSS: return "盘亏";
            case RESULT_GAIN: return "盘盈";
            default: return result;
        }
    }
}
