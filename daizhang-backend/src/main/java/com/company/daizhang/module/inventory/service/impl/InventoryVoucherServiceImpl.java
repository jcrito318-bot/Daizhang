package com.company.daizhang.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.inventory.entity.InventoryIn;
import com.company.daizhang.module.inventory.entity.InventoryInDetail;
import com.company.daizhang.module.inventory.entity.InventoryOut;
import com.company.daizhang.module.inventory.entity.InventoryOutDetail;
import com.company.daizhang.module.inventory.entity.InventoryStock;
import com.company.daizhang.module.inventory.mapper.InventoryInDetailMapper;
import com.company.daizhang.module.inventory.mapper.InventoryInMapper;
import com.company.daizhang.module.inventory.mapper.InventoryItemMapper;
import com.company.daizhang.module.inventory.mapper.InventoryOutDetailMapper;
import com.company.daizhang.module.inventory.mapper.InventoryOutMapper;
import com.company.daizhang.module.inventory.mapper.InventoryStockMapper;
import com.company.daizhang.module.inventory.service.InventoryVoucherService;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.voucher.dto.VoucherCreateRequest;
import com.company.daizhang.module.voucher.dto.VoucherDetailRequest;
import com.company.daizhang.module.voucher.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 存货出入库凭证生成服务实现
 * <p>
 * 入库凭证：借 库存商品(1405)，贷 应付账款(2202)（采购入库默认赊购）
 * 出库凭证：借 主营业务成本(6401)，贷 库存商品(1405)，成本沿用审核时按加权平均法计算的 costAmount
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryVoucherServiceImpl implements InventoryVoucherService {

    private final VoucherService voucherService;
    private final InventoryInMapper inMapper;
    private final InventoryInDetailMapper inDetailMapper;
    private final InventoryOutMapper outMapper;
    private final InventoryOutDetailMapper outDetailMapper;
    private final InventoryItemMapper itemMapper;
    private final InventoryStockMapper stockMapper;
    private final SubjectMapper subjectMapper;
    private final AccountSetAccessService accountSetAccessService;

    // 自注入代理引用：批量方法内部循环调用单条方法时，this.xxx() 是自调用，会绕过 Spring AOP 代理，
    // 导致单条方法上的 @Transactional(REQUIRES_NEW) 失效。通过 self 代理调用确保事务传播生效，
    // 使每条单据在独立事务中执行，单条失败仅回滚自身，不影响批次内其他单据。
    @Lazy
    @Autowired
    private InventoryVoucherService self;

    // 科目编码常量
    private static final String CODE_INVENTORY = "1405";         // 库存商品
    private static final String CODE_ACCOUNTS_PAYABLE = "2202";  // 应付账款
    private static final String CODE_MAIN_COST = "5401";         // 主营业务成本(与标准科目模板一致)

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public Long generateInVoucher(Long inId) {
        // 查询入库单
        InventoryIn in = inMapper.selectById(inId);
        if (in == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "入库单不存在");
        }
        // 越权校验：仅账套所有者可生成凭证
        accountSetAccessService.checkOwner(in.getAccountSetId());

        // 校验是否已生成凭证，避免重复生成
        if (in.getVoucherId() != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该入库单已生成凭证");
        }

        // 查询入库明细
        LambdaQueryWrapper<InventoryInDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(InventoryInDetail::getInId, inId);
        List<InventoryInDetail> details = inDetailMapper.selectList(detailWrapper);
        if (details == null || details.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "入库单明细为空，无法生成凭证");
        }

        // 按明细汇总入库金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (InventoryInDetail detail : details) {
            totalAmount = totalAmount.add(nvl(detail.getAmount()));
        }
        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "入库金额必须大于零");
        }

        // 入库日期缺省取当前日期
        LocalDate voucherDate = in.getInDate() != null ? in.getInDate() : LocalDate.now();
        int year = voucherDate.getYear();
        int month = voucherDate.getMonthValue();

        // 解析科目ID：借方库存商品(1405)，贷方应付账款(2202)
        Long inventorySubjectId = getSubjectIdByCode(in.getAccountSetId(), CODE_INVENTORY, "库存商品");
        Long payableSubjectId = getSubjectIdByCode(in.getAccountSetId(), CODE_ACCOUNTS_PAYABLE, "应付账款");

        String summary = "采购入库-" + (in.getInNo() != null ? in.getInNo() : inId);

        // 构建凭证请求
        VoucherCreateRequest voucherRequest = new VoucherCreateRequest();
        voucherRequest.setAccountSetId(in.getAccountSetId());
        voucherRequest.setVoucherDate(voucherDate);
        voucherRequest.setYear(year);
        voucherRequest.setMonth(month);
        voucherRequest.setAttachmentCount(0);

        List<VoucherDetailRequest> voucherDetails = new ArrayList<>();

        // 借：库存商品
        VoucherDetailRequest debitDetail = new VoucherDetailRequest();
        debitDetail.setLineNo(1);
        debitDetail.setSummary(summary);
        debitDetail.setSubjectId(inventorySubjectId);
        debitDetail.setSubjectCode(CODE_INVENTORY);
        debitDetail.setDebit(totalAmount);
        debitDetail.setCredit(BigDecimal.ZERO);
        debitDetail.setSortOrder(1);
        voucherDetails.add(debitDetail);

        // 贷：应付账款
        VoucherDetailRequest creditDetail = new VoucherDetailRequest();
        creditDetail.setLineNo(2);
        creditDetail.setSummary(summary);
        creditDetail.setSubjectId(payableSubjectId);
        creditDetail.setSubjectCode(CODE_ACCOUNTS_PAYABLE);
        creditDetail.setDebit(BigDecimal.ZERO);
        creditDetail.setCredit(totalAmount);
        creditDetail.setSortOrder(2);
        voucherDetails.add(creditDetail);

        voucherRequest.setDetails(voucherDetails);

        // 调用凭证服务创建凭证（内部会校验会计期间、借贷平衡等），直接返回新凭证ID
        Long voucherId = voucherService.createVoucher(voucherRequest);

        if (voucherId != null) {
            in.setVoucherId(voucherId);
            inMapper.updateById(in);
        }
        log.info("入库单生成凭证成功，入库单ID: {}, 凭证ID: {}", inId, voucherId);
        return voucherId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public Long generateOutVoucher(Long outId) {
        // 查询出库单
        InventoryOut out = outMapper.selectById(outId);
        if (out == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "出库单不存在");
        }
        // 越权校验
        accountSetAccessService.checkOwner(out.getAccountSetId());

        // 校验是否已生成凭证
        if (out.getVoucherId() != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该出库单已生成凭证");
        }

        // 查询出库明细
        LambdaQueryWrapper<InventoryOutDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(InventoryOutDetail::getOutId, outId);
        List<InventoryOutDetail> details = outDetailMapper.selectList(detailWrapper);
        if (details == null || details.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "出库单明细为空，无法生成凭证");
        }

        // 成本按加权平均法计算：优先使用审核时已回写的明细 costAmount（auditOut 中按月末库存加权平均计算），
        // 若明细 costAmount 为 0（未审核或历史数据），则按出库日期所在月份的库存加权平均成本重算。
        // 注意:不能用 detail.getUnitPrice() 兜底——那是售价,售价通常高于成本价,会导致
        // 主营业务成本被高估、利润被低估,同时库存商品贷方金额不等于实际库存成本。
        LocalDate voucherDate = out.getOutDate() != null ? out.getOutDate() : LocalDate.now();
        int year = voucherDate.getYear();
        int month = voucherDate.getMonthValue();

        BigDecimal totalCost = BigDecimal.ZERO;
        for (InventoryOutDetail detail : details) {
            BigDecimal costAmt = nvl(detail.getCostAmount());
            if (costAmt.compareTo(BigDecimal.ZERO) == 0) {
                // 明细未回写成本(未审核),按库存加权平均成本重算,而非售价
                InventoryStock stock = getStock(out.getAccountSetId(), detail.getItemId(), year, month);
                if (stock != null
                        && nvl(stock.getEndQuantity()).compareTo(BigDecimal.ZERO) > 0
                        && stock.getEndAmount() != null) {
                    BigDecimal unitCost = stock.getEndAmount()
                            .divide(nvl(stock.getEndQuantity()), 4, RoundingMode.HALF_UP);
                    costAmt = nvl(detail.getQuantity()).multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
                } else {
                    // 库存也无记录:拒绝生成,避免用售价兜底导致成本错误
                    throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                            "出库明细无成本数据且库存无记录,请先审核出库单再生成凭证");
                }
            }
            totalCost = totalCost.add(costAmt);
        }
        totalCost = totalCost.setScale(2, RoundingMode.HALF_UP);
        if (totalCost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "出库成本金额必须大于零");
        }

        // 出库日期/年月已在上方成本计算前确定(voucherDate/year/month)

        // 解析科目ID：借方主营业务成本(6401)，贷方库存商品(1405)
        Long costSubjectId = getSubjectIdByCode(out.getAccountSetId(), CODE_MAIN_COST, "主营业务成本");
        Long inventorySubjectId = getSubjectIdByCode(out.getAccountSetId(), CODE_INVENTORY, "库存商品");

        String summary = "销售出库-" + (out.getOutNo() != null ? out.getOutNo() : outId);

        // 构建凭证请求
        VoucherCreateRequest voucherRequest = new VoucherCreateRequest();
        voucherRequest.setAccountSetId(out.getAccountSetId());
        voucherRequest.setVoucherDate(voucherDate);
        voucherRequest.setYear(year);
        voucherRequest.setMonth(month);
        voucherRequest.setAttachmentCount(0);

        List<VoucherDetailRequest> voucherDetails = new ArrayList<>();

        // 借：主营业务成本
        VoucherDetailRequest debitDetail = new VoucherDetailRequest();
        debitDetail.setLineNo(1);
        debitDetail.setSummary(summary);
        debitDetail.setSubjectId(costSubjectId);
        debitDetail.setSubjectCode(CODE_MAIN_COST);
        debitDetail.setDebit(totalCost);
        debitDetail.setCredit(BigDecimal.ZERO);
        debitDetail.setSortOrder(1);
        voucherDetails.add(debitDetail);

        // 贷：库存商品
        VoucherDetailRequest creditDetail = new VoucherDetailRequest();
        creditDetail.setLineNo(2);
        creditDetail.setSummary(summary);
        creditDetail.setSubjectId(inventorySubjectId);
        creditDetail.setSubjectCode(CODE_INVENTORY);
        creditDetail.setDebit(BigDecimal.ZERO);
        creditDetail.setCredit(totalCost);
        creditDetail.setSortOrder(2);
        voucherDetails.add(creditDetail);

        voucherRequest.setDetails(voucherDetails);

        // 调用凭证服务创建凭证，直接返回新凭证ID
        Long voucherId = voucherService.createVoucher(voucherRequest);

        if (voucherId != null) {
            out.setVoucherId(voucherId);
            outMapper.updateById(out);
        }
        log.info("出库单生成凭证成功，出库单ID: {}, 凭证ID: {}", outId, voucherId);
        return voucherId;
    }

    @Override
    public Long generateInVoucherBatch(List<Long> inIds) {
        if (inIds == null || inIds.isEmpty()) {
            return 0L;
        }
        long successCount = 0;
        // 收集失败项ID,便于运维定位需重试的单据(方法签名返回 successCount,此处至少保证日志可查)
        List<Long> failedIds = new ArrayList<>();
        for (Long inId : inIds) {
            try {
                // 通过 self 代理调用，使 generateInVoucher 的 REQUIRES_NEW 事务传播生效，
                // 单条失败仅回滚自身事务，不影响批次内其他单据（避免外层 catch 掩盖 rollback-only）
                self.generateInVoucher(inId);
                successCount++;
            } catch (BusinessException e) {
                // 业务异常(如单据状态不符、科目缺失)单笔失败不影响整批,记录告警后继续
                log.warn("批量生成入库凭证跳过，入库单ID: {}, 原因: {}", inId, e.getMessage());
                failedIds.add(inId);
            } catch (Exception e) {
                // 非业务异常(NPE/DB异常等)同样不中断整批,但需 error 级别记录便于排查
                log.error("批量生成入库凭证异常，入库单ID: {}", inId, e);
                failedIds.add(inId);
            }
        }
        if (!failedIds.isEmpty()) {
            log.warn("批量生成入库凭证存在失败项,失败数量: {}/{}, failedIds={}",
                    failedIds.size(), inIds.size(), failedIds);
        }
        log.info("批量生成入库凭证完成，成功数量: {}/{}", successCount, inIds.size());
        return successCount;
    }

    @Override
    public Long generateOutVoucherBatch(List<Long> outIds) {
        if (outIds == null || outIds.isEmpty()) {
            return 0L;
        }
        long successCount = 0;
        // 收集失败项ID,便于运维定位需重试的单据(方法签名返回 successCount,此处至少保证日志可查)
        List<Long> failedIds = new ArrayList<>();
        for (Long outId : outIds) {
            try {
                // 通过 self 代理调用，使 generateOutVoucher 的 REQUIRES_NEW 事务传播生效
                self.generateOutVoucher(outId);
                successCount++;
            } catch (BusinessException e) {
                log.warn("批量生成出库凭证跳过，出库单ID: {}, 原因: {}", outId, e.getMessage());
                failedIds.add(outId);
            } catch (Exception e) {
                // 非业务异常(NPE/DB异常等)同样不中断整批,但需 error 级别记录便于排查
                log.error("批量生成出库凭证异常，出库单ID: {}", outId, e);
                failedIds.add(outId);
            }
        }
        if (!failedIds.isEmpty()) {
            log.warn("批量生成出库凭证存在失败项,失败数量: {}/{}, failedIds={}",
                    failedIds.size(), outIds.size(), failedIds);
        }
        log.info("批量生成出库凭证完成，成功数量: {}/{}", successCount, outIds.size());
        return successCount;
    }

    // ==================== 辅助方法 ====================

    /**
     * 通过科目编码查询科目ID
     */
    private Long getSubjectIdByCode(Long accountSetId, String code, String subjectName) {
        Subject subject = subjectMapper.selectOne(new LambdaQueryWrapper<Subject>()
                .eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getCode, code));
        if (subject == null) {
            throw new BusinessException(ErrorCode.VOUCHER_SUBJECT_INVALID.getCode(),
                    "未查询到科目[" + subjectName + "]，编码: " + code);
        }
        return subject.getId();
    }

    /**
     * null 转 0
     */
    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    /**
     * 查询指定账套/商品/年月的库存记录(加权平均成本来源)。
     * 用于出库凭证成本兜底计算:当出库明细未回写 costAmount 时,从库存取 endAmount/endQuantity 计算单位成本。
     */
    private InventoryStock getStock(Long accountSetId, Long itemId, int year, int month) {
        LambdaQueryWrapper<InventoryStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryStock::getAccountSetId, accountSetId)
                .eq(InventoryStock::getItemId, itemId)
                .eq(InventoryStock::getYear, year)
                .eq(InventoryStock::getMonth, month);
        return stockMapper.selectOne(wrapper);
    }
}
