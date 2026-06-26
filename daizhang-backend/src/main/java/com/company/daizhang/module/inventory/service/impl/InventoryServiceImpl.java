package com.company.daizhang.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.inventory.dto.*;
import com.company.daizhang.module.inventory.entity.*;
import com.company.daizhang.module.inventory.mapper.*;
import com.company.daizhang.module.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryItemMapper itemMapper;
    private final InventoryStockMapper stockMapper;
    private final InventoryInMapper inMapper;
    private final InventoryInDetailMapper inDetailMapper;
    private final InventoryOutMapper outMapper;
    private final InventoryOutDetailMapper outDetailMapper;

    @Override
    public Page<InventoryItem> getItemPage(InventoryItemQueryRequest request) {
        LambdaQueryWrapper<InventoryItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryItem::getAccountSetId, request.getAccountSetId());
        if (request.getItemCode() != null) {
            wrapper.like(InventoryItem::getItemCode, request.getItemCode());
        }
        if (request.getItemName() != null) {
            wrapper.like(InventoryItem::getItemName, request.getItemName());
        }
        if (request.getCategory() != null) {
            wrapper.eq(InventoryItem::getCategory, request.getCategory());
        }
        if (request.getStatus() != null) {
            wrapper.eq(InventoryItem::getStatus, request.getStatus());
        }
        wrapper.orderByDesc(InventoryItem::getId);
        Page<InventoryItem> page = new Page<>(request.getPageNum(), request.getPageSize());
        return itemMapper.selectPage(page, wrapper);
    }

    @Override
    public InventoryItem getItemById(Long id) {
        return itemMapper.selectById(id);
    }

    @Override
    public Long createItem(InventoryItemCreateRequest request) {
        InventoryItem item = new InventoryItem();
        item.setAccountSetId(request.getAccountSetId());
        item.setItemCode(request.getItemCode());
        item.setItemName(request.getItemName());
        item.setSpecification(request.getSpecification());
        item.setUnit(request.getUnit());
        item.setUnitPrice(request.getUnitPrice());
        item.setCategory(request.getCategory());
        item.setStatus(1);
        item.setRemark(request.getRemark());
        item.setCreateBy(SecurityUtils.getCurrentUserId());
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        itemMapper.insert(item);
        return item.getId();
    }

    @Override
    public void updateItem(InventoryItemUpdateRequest request) {
        InventoryItem item = itemMapper.selectById(request.getId());
        if (item == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品不存在");
        }
        if (request.getItemName() != null) item.setItemName(request.getItemName());
        if (request.getSpecification() != null) item.setSpecification(request.getSpecification());
        if (request.getUnit() != null) item.setUnit(request.getUnit());
        if (request.getUnitPrice() != null) item.setUnitPrice(request.getUnitPrice());
        if (request.getCategory() != null) item.setCategory(request.getCategory());
        if (request.getStatus() != null) item.setStatus(request.getStatus());
        if (request.getRemark() != null) item.setRemark(request.getRemark());
        item.setUpdateTime(LocalDateTime.now());
        itemMapper.updateById(item);
    }

    @Override
    public void deleteItem(Long id) {
        itemMapper.deleteById(id);
    }

    @Override
    public Page<InventoryStock> getStockPage(InventoryStockQueryRequest request) {
        LambdaQueryWrapper<InventoryStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryStock::getAccountSetId, request.getAccountSetId());
        if (request.getItemId() != null) {
            wrapper.eq(InventoryStock::getItemId, request.getItemId());
        }
        if (request.getYear() != null) {
            wrapper.eq(InventoryStock::getYear, request.getYear());
        }
        if (request.getMonth() != null) {
            wrapper.eq(InventoryStock::getMonth, request.getMonth());
        }
        wrapper.orderByDesc(InventoryStock::getId);
        Page<InventoryStock> page = new Page<>(request.getPageNum(), request.getPageSize());
        return stockMapper.selectPage(page, wrapper);
    }

    @Override
    public List<InventoryStock> getStockList(InventoryStockQueryRequest request) {
        LambdaQueryWrapper<InventoryStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryStock::getAccountSetId, request.getAccountSetId());
        if (request.getItemId() != null) {
            wrapper.eq(InventoryStock::getItemId, request.getItemId());
        }
        if (request.getYear() != null) {
            wrapper.eq(InventoryStock::getYear, request.getYear());
        }
        if (request.getMonth() != null) {
            wrapper.eq(InventoryStock::getMonth, request.getMonth());
        }
        return stockMapper.selectList(wrapper);
    }

    @Override
    public Page<InventoryIn> getInPage(InventoryInQueryRequest request) {
        LambdaQueryWrapper<InventoryIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryIn::getAccountSetId, request.getAccountSetId());
        if (request.getInNo() != null) {
            wrapper.like(InventoryIn::getInNo, request.getInNo());
        }
        if (request.getInType() != null) {
            wrapper.eq(InventoryIn::getInType, request.getInType());
        }
        if (request.getStatus() != null) {
            wrapper.eq(InventoryIn::getStatus, request.getStatus());
        }
        if (request.getStartDate() != null) {
            wrapper.ge(InventoryIn::getInDate, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            wrapper.le(InventoryIn::getInDate, request.getEndDate());
        }
        wrapper.orderByDesc(InventoryIn::getId);
        Page<InventoryIn> page = new Page<>(request.getPageNum(), request.getPageSize());
        return inMapper.selectPage(page, wrapper);
    }

    @Override
    public InventoryIn getInById(Long id) {
        InventoryIn in = inMapper.selectById(id);
        if (in != null) {
            LambdaQueryWrapper<InventoryInDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.eq(InventoryInDetail::getInId, id);
            in.setDetails(inDetailMapper.selectList(detailWrapper));
        }
        return in;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createIn(InventoryInCreateRequest request) {
        String inNo = generateInNo(request.getAccountSetId(), request.getInDate());

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmt = BigDecimal.ZERO;
        if (request.getDetails() != null) {
            for (InventoryInCreateRequest.InDetailDTO d : request.getDetails()) {
                BigDecimal amt = d.getQuantity().multiply(d.getUnitPrice());
                totalQty = totalQty.add(d.getQuantity());
                totalAmt = totalAmt.add(amt);
            }
        }

        InventoryIn in = new InventoryIn();
        in.setAccountSetId(request.getAccountSetId());
        in.setInNo(inNo);
        in.setInType(request.getInType());
        in.setInDate(request.getInDate());
        in.setSupplier(request.getSupplier());
        in.setTotalQuantity(totalQty);
        in.setTotalAmount(totalAmt.setScale(2, RoundingMode.HALF_UP));
        in.setStatus(0);
        in.setRemark(request.getRemark());
        in.setCreateBy(SecurityUtils.getCurrentUserId());
        in.setCreateTime(LocalDateTime.now());
        in.setUpdateTime(LocalDateTime.now());
        inMapper.insert(in);

        if (request.getDetails() != null) {
            for (InventoryInCreateRequest.InDetailDTO d : request.getDetails()) {
                InventoryItem item = itemMapper.selectById(d.getItemId());
                if (item == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "商品不存在: " + d.getItemId());
                }
                InventoryInDetail detail = new InventoryInDetail();
                detail.setInId(in.getId());
                detail.setItemId(d.getItemId());
                detail.setItemCode(item.getItemCode());
                detail.setItemName(item.getItemName());
                detail.setSpecification(item.getSpecification());
                detail.setUnit(item.getUnit());
                detail.setQuantity(d.getQuantity());
                detail.setUnitPrice(d.getUnitPrice());
                detail.setAmount(d.getQuantity().multiply(d.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));
                detail.setRemark(d.getRemark());
                inDetailMapper.insert(detail);
            }
        }
        return in.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateIn(InventoryInUpdateRequest request) {
        InventoryIn in = inMapper.selectById(request.getId());
        if (in == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "入库单不存在");
        }
        if (in.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已审核的入库单不可修改");
        }
        if (request.getInType() != null) in.setInType(request.getInType());
        if (request.getInDate() != null) in.setInDate(request.getInDate());
        if (request.getSupplier() != null) in.setSupplier(request.getSupplier());
        if (request.getRemark() != null) in.setRemark(request.getRemark());

        if (request.getDetails() != null) {
            LambdaQueryWrapper<InventoryInDetail> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(InventoryInDetail::getInId, in.getId());
            inDetailMapper.delete(delWrapper);

            BigDecimal totalQty = BigDecimal.ZERO;
            BigDecimal totalAmt = BigDecimal.ZERO;
            for (InventoryInCreateRequest.InDetailDTO d : request.getDetails()) {
                InventoryItem item = itemMapper.selectById(d.getItemId());
                if (item == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "商品不存在: " + d.getItemId());
                }
                BigDecimal amt = d.getQuantity().multiply(d.getUnitPrice());
                totalQty = totalQty.add(d.getQuantity());
                totalAmt = totalAmt.add(amt);
                InventoryInDetail detail = new InventoryInDetail();
                detail.setInId(in.getId());
                detail.setItemId(d.getItemId());
                detail.setItemCode(item.getItemCode());
                detail.setItemName(item.getItemName());
                detail.setSpecification(item.getSpecification());
                detail.setUnit(item.getUnit());
                detail.setQuantity(d.getQuantity());
                detail.setUnitPrice(d.getUnitPrice());
                detail.setAmount(amt.setScale(2, RoundingMode.HALF_UP));
                detail.setRemark(d.getRemark());
                inDetailMapper.insert(detail);
            }
            in.setTotalQuantity(totalQty);
            in.setTotalAmount(totalAmt.setScale(2, RoundingMode.HALF_UP));
        }
        in.setUpdateTime(LocalDateTime.now());
        inMapper.updateById(in);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteIn(Long id) {
        InventoryIn in = inMapper.selectById(id);
        if (in != null && in.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已审核的入库单不可删除");
        }
        LambdaQueryWrapper<InventoryInDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(InventoryInDetail::getInId, id);
        inDetailMapper.delete(detailWrapper);
        inMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditIn(Long id) {
        InventoryIn in = inMapper.selectById(id);
        if (in == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "入库单不存在");
        }
        if (in.getStatus() != null && in.getStatus() == 1) {
            return;
        }
        if (in.getInDate() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "入库日期不能为空");
        }
        in.setStatus(1);
        in.setUpdateTime(LocalDateTime.now());
        inMapper.updateById(in);

        LambdaQueryWrapper<InventoryInDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(InventoryInDetail::getInId, id);
        List<InventoryInDetail> details = inDetailMapper.selectList(detailWrapper);

        int year = in.getInDate().getYear();
        int month = in.getInDate().getMonthValue();

        for (InventoryInDetail detail : details) {
            updateStockOnIn(in.getAccountSetId(), detail.getItemId(), year, month,
                    detail.getQuantity(), detail.getAmount());
        }
    }

    @Override
    public Page<InventoryOut> getOutPage(InventoryOutQueryRequest request) {
        LambdaQueryWrapper<InventoryOut> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryOut::getAccountSetId, request.getAccountSetId());
        if (request.getOutNo() != null) {
            wrapper.like(InventoryOut::getOutNo, request.getOutNo());
        }
        if (request.getOutType() != null) {
            wrapper.eq(InventoryOut::getOutType, request.getOutType());
        }
        if (request.getStatus() != null) {
            wrapper.eq(InventoryOut::getStatus, request.getStatus());
        }
        if (request.getStartDate() != null) {
            wrapper.ge(InventoryOut::getOutDate, request.getStartDate());
        }
        if (request.getEndDate() != null) {
            wrapper.le(InventoryOut::getOutDate, request.getEndDate());
        }
        wrapper.orderByDesc(InventoryOut::getId);
        Page<InventoryOut> page = new Page<>(request.getPageNum(), request.getPageSize());
        return outMapper.selectPage(page, wrapper);
    }

    @Override
    public InventoryOut getOutById(Long id) {
        InventoryOut out = outMapper.selectById(id);
        if (out != null) {
            LambdaQueryWrapper<InventoryOutDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.eq(InventoryOutDetail::getOutId, id);
            out.setDetails(outDetailMapper.selectList(detailWrapper));
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOut(InventoryOutCreateRequest request) {
        String outNo = generateOutNo(request.getAccountSetId(), request.getOutDate());

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmt = BigDecimal.ZERO;
        if (request.getDetails() != null) {
            for (InventoryOutCreateRequest.OutDetailDTO d : request.getDetails()) {
                BigDecimal amt = d.getQuantity().multiply(d.getUnitPrice());
                totalQty = totalQty.add(d.getQuantity());
                totalAmt = totalAmt.add(amt);
            }
        }

        InventoryOut out = new InventoryOut();
        out.setAccountSetId(request.getAccountSetId());
        out.setOutNo(outNo);
        out.setOutType(request.getOutType());
        out.setOutDate(request.getOutDate());
        out.setCustomer(request.getCustomer());
        out.setTotalQuantity(totalQty);
        out.setTotalAmount(totalAmt.setScale(2, RoundingMode.HALF_UP));
        out.setCostAmount(BigDecimal.ZERO);
        out.setStatus(0);
        out.setRemark(request.getRemark());
        out.setCreateBy(SecurityUtils.getCurrentUserId());
        out.setCreateTime(LocalDateTime.now());
        out.setUpdateTime(LocalDateTime.now());
        outMapper.insert(out);

        if (request.getDetails() != null) {
            for (InventoryOutCreateRequest.OutDetailDTO d : request.getDetails()) {
                InventoryItem item = itemMapper.selectById(d.getItemId());
                if (item == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "商品不存在，ID: " + d.getItemId());
                }
                InventoryOutDetail detail = new InventoryOutDetail();
                detail.setOutId(out.getId());
                detail.setItemId(d.getItemId());
                detail.setItemCode(item.getItemCode());
                detail.setItemName(item.getItemName());
                detail.setSpecification(item.getSpecification());
                detail.setUnit(item.getUnit());
                detail.setQuantity(d.getQuantity());
                detail.setUnitPrice(d.getUnitPrice());
                detail.setAmount(d.getQuantity().multiply(d.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));
                detail.setUnitCost(BigDecimal.ZERO);
                detail.setCostAmount(BigDecimal.ZERO);
                detail.setRemark(d.getRemark());
                outDetailMapper.insert(detail);
            }
        }
        return out.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOut(InventoryOutUpdateRequest request) {
        InventoryOut out = outMapper.selectById(request.getId());
        if (out == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "出库单不存在");
        }
        if (out.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已审核的出库单不可修改");
        }
        if (request.getOutType() != null) out.setOutType(request.getOutType());
        if (request.getOutDate() != null) out.setOutDate(request.getOutDate());
        if (request.getCustomer() != null) out.setCustomer(request.getCustomer());
        if (request.getRemark() != null) out.setRemark(request.getRemark());

        if (request.getDetails() != null) {
            LambdaQueryWrapper<InventoryOutDetail> delWrapper = new LambdaQueryWrapper<>();
            delWrapper.eq(InventoryOutDetail::getOutId, out.getId());
            outDetailMapper.delete(delWrapper);

            BigDecimal totalQty = BigDecimal.ZERO;
            BigDecimal totalAmt = BigDecimal.ZERO;
            for (InventoryOutCreateRequest.OutDetailDTO d : request.getDetails()) {
                InventoryItem item = itemMapper.selectById(d.getItemId());
                if (item == null) continue;
                BigDecimal amt = d.getQuantity().multiply(d.getUnitPrice());
                totalQty = totalQty.add(d.getQuantity());
                totalAmt = totalAmt.add(amt);
                InventoryOutDetail detail = new InventoryOutDetail();
                detail.setOutId(out.getId());
                detail.setItemId(d.getItemId());
                detail.setItemCode(item.getItemCode());
                detail.setItemName(item.getItemName());
                detail.setSpecification(item.getSpecification());
                detail.setUnit(item.getUnit());
                detail.setQuantity(d.getQuantity());
                detail.setUnitPrice(d.getUnitPrice());
                detail.setAmount(amt.setScale(2, RoundingMode.HALF_UP));
                detail.setUnitCost(BigDecimal.ZERO);
                detail.setCostAmount(BigDecimal.ZERO);
                detail.setRemark(d.getRemark());
                outDetailMapper.insert(detail);
            }
            out.setTotalQuantity(totalQty);
            out.setTotalAmount(totalAmt.setScale(2, RoundingMode.HALF_UP));
        }
        out.setUpdateTime(LocalDateTime.now());
        outMapper.updateById(out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOut(Long id) {
        InventoryOut out = outMapper.selectById(id);
        if (out != null && out.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "已审核的出库单不可删除");
        }
        LambdaQueryWrapper<InventoryOutDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(InventoryOutDetail::getOutId, id);
        outDetailMapper.delete(detailWrapper);
        outMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditOut(Long id) {
        InventoryOut out = outMapper.selectById(id);
        if (out == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "出库单不存在");
        }
        if (out.getStatus() != null && out.getStatus() == 1) {
            return;
        }
        if (out.getOutDate() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "出库日期不能为空");
        }

        LambdaQueryWrapper<InventoryOutDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(InventoryOutDetail::getOutId, id);
        List<InventoryOutDetail> details = outDetailMapper.selectList(detailWrapper);

        int year = out.getOutDate().getYear();
        int month = out.getOutDate().getMonthValue();
        BigDecimal totalCost = BigDecimal.ZERO;

        for (InventoryOutDetail detail : details) {
            InventoryStock stock = getCurrentStock(out.getAccountSetId(), detail.getItemId(), year, month);
            // 校验库存充足，避免负库存
            BigDecimal availableQty = (stock != null && stock.getEndQuantity() != null)
                    ? stock.getEndQuantity() : BigDecimal.ZERO;
            if (availableQty.compareTo(detail.getQuantity()) < 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "商品库存不足，可用库存：" + availableQty + "，出库数量：" + detail.getQuantity());
            }

            BigDecimal unitCost = BigDecimal.ZERO;
            BigDecimal costAmt = BigDecimal.ZERO;

            if (stock != null && stock.getEndQuantity() != null
                    && stock.getEndQuantity().compareTo(BigDecimal.ZERO) > 0
                    && stock.getEndAmount() != null) {
                unitCost = stock.getEndAmount().divide(stock.getEndQuantity(), 4, RoundingMode.HALF_UP);
                costAmt = unitCost.multiply(detail.getQuantity()).setScale(2, RoundingMode.HALF_UP);
            }

            detail.setUnitCost(unitCost);
            detail.setCostAmount(costAmt);
            outDetailMapper.updateById(detail);
            totalCost = totalCost.add(costAmt);

            updateStockOnOut(out.getAccountSetId(), detail.getItemId(), year, month,
                    detail.getQuantity(), costAmt);
        }

        out.setCostAmount(totalCost.setScale(2, RoundingMode.HALF_UP));
        out.setStatus(1);
        out.setUpdateTime(LocalDateTime.now());
        outMapper.updateById(out);
    }

    private String generateInNo(Long accountSetId, LocalDate date) {
        String prefix = "RK" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<InventoryIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(InventoryIn::getInNo, prefix);
        Long count = inMapper.selectCount(wrapper);
        int seq = (count != null ? count.intValue() : 0) + 1;
        return prefix + String.format("%04d", seq);
    }

    private String generateOutNo(Long accountSetId, LocalDate date) {
        String prefix = "CK" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<InventoryOut> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(InventoryOut::getOutNo, prefix);
        Long count = outMapper.selectCount(wrapper);
        int seq = (count != null ? count.intValue() : 0) + 1;
        return prefix + String.format("%04d", seq);
    }

    private InventoryStock getCurrentStock(Long accountSetId, Long itemId, int year, int month) {
        LambdaQueryWrapper<InventoryStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryStock::getAccountSetId, accountSetId)
                .eq(InventoryStock::getItemId, itemId)
                .eq(InventoryStock::getYear, year)
                .eq(InventoryStock::getMonth, month);
        return stockMapper.selectOne(wrapper);
    }

    private void saveOrUpdateStock(InventoryStock stock) {
        if (stock.getId() != null) {
            stockMapper.updateById(stock);
        } else {
            stockMapper.insert(stock);
        }
    }

    private void updateStockOnIn(Long accountSetId, Long itemId, int year, int month,
                                 BigDecimal qty, BigDecimal amt) {
        InventoryStock stock = getOrCreateStock(accountSetId, itemId, year, month);
        stock.setInQuantity(nvl(stock.getInQuantity()).add(qty));
        stock.setInAmount(nvl(stock.getInAmount()).add(amt));
        stock.setEndQuantity(nvl(stock.getBeginQuantity()).add(nvl(stock.getInQuantity()))
                .subtract(nvl(stock.getOutQuantity())));
        stock.setEndAmount(nvl(stock.getBeginAmount()).add(nvl(stock.getInAmount()))
                .subtract(nvl(stock.getOutAmount())));
        if (stock.getEndQuantity().compareTo(BigDecimal.ZERO) > 0) {
            stock.setUnitCost(stock.getEndAmount().divide(stock.getEndQuantity(), 4, RoundingMode.HALF_UP));
        }
        saveOrUpdateStock(stock);
    }

    private void updateStockOnOut(Long accountSetId, Long itemId, int year, int month,
                                  BigDecimal qty, BigDecimal cost) {
        InventoryStock stock = getOrCreateStock(accountSetId, itemId, year, month);
        stock.setOutQuantity(nvl(stock.getOutQuantity()).add(qty));
        stock.setOutAmount(nvl(stock.getOutAmount()).add(cost));
        stock.setEndQuantity(nvl(stock.getBeginQuantity()).add(nvl(stock.getInQuantity()))
                .subtract(nvl(stock.getOutQuantity())));
        stock.setEndAmount(nvl(stock.getBeginAmount()).add(nvl(stock.getInAmount()))
                .subtract(nvl(stock.getOutAmount())));
        if (stock.getEndQuantity().compareTo(BigDecimal.ZERO) > 0) {
            stock.setUnitCost(stock.getEndAmount().divide(stock.getEndQuantity(), 4, RoundingMode.HALF_UP));
        }
        saveOrUpdateStock(stock);
    }

    private InventoryStock getOrCreateStock(Long accountSetId, Long itemId, int year, int month) {
        InventoryStock stock = getCurrentStock(accountSetId, itemId, year, month);
        if (stock == null) {
            stock = new InventoryStock();
            stock.setAccountSetId(accountSetId);
            stock.setItemId(itemId);
            stock.setYear(year);
            stock.setMonth(month);
            stock.setBeginQuantity(BigDecimal.ZERO);
            stock.setBeginAmount(BigDecimal.ZERO);
            stock.setInQuantity(BigDecimal.ZERO);
            stock.setInAmount(BigDecimal.ZERO);
            stock.setOutQuantity(BigDecimal.ZERO);
            stock.setOutAmount(BigDecimal.ZERO);
            stock.setEndQuantity(BigDecimal.ZERO);
            stock.setEndAmount(BigDecimal.ZERO);
            stock.setUnitCost(BigDecimal.ZERO);
        }
        return stock;
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }
}
