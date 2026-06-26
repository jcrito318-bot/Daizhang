package com.company.daizhang.module.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.module.inventory.dto.*;
import com.company.daizhang.module.inventory.entity.InventoryItem;
import com.company.daizhang.module.inventory.entity.InventoryIn;
import com.company.daizhang.module.inventory.entity.InventoryOut;
import com.company.daizhang.module.inventory.entity.InventoryStock;

import java.util.List;

public interface InventoryService {
    Page<InventoryItem> getItemPage(InventoryItemQueryRequest request);
    InventoryItem getItemById(Long id);
    Long createItem(InventoryItemCreateRequest request);
    void updateItem(InventoryItemUpdateRequest request);
    void deleteItem(Long id);

    Page<InventoryStock> getStockPage(InventoryStockQueryRequest request);
    List<InventoryStock> getStockList(InventoryStockQueryRequest request);

    Page<InventoryIn> getInPage(InventoryInQueryRequest request);
    InventoryIn getInById(Long id);
    Long createIn(InventoryInCreateRequest request);
    void updateIn(InventoryInUpdateRequest request);
    void deleteIn(Long id);
    void auditIn(Long id);

    Page<InventoryOut> getOutPage(InventoryOutQueryRequest request);
    InventoryOut getOutById(Long id);
    Long createOut(InventoryOutCreateRequest request);
    void updateOut(InventoryOutUpdateRequest request);
    void deleteOut(Long id);
    void auditOut(Long id);
}
