package com.restaurant.ddd.application.port.in;

import com.restaurant.ddd.application.command.RestaurantCommands.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Inbound port: the application use cases available to HTTP and other adapters. */
public interface RestaurantUseCases {
    Map<String,Object> createBranch(BranchInput input);
    Map<String,Object> updateBranch(UUID id, BranchInput input);
    List<Map<String,Object>> branches();
    Map<String,Object> getBranch(UUID id);
    Map<String,Object> createMaster(MasterInput input);
    Map<String,Object> createMasterMenu(MasterMenuInput input);
    List<Map<String,Object>> masters();
    Map<String,Object> getMaster(UUID id);
    Map<String,Object> enable(UUID branchId, UUID masterId);
    void disable(UUID branchId, UUID masterId);
    List<Map<String,Object>> assigned(UUID branchId);
    Map<String,Object> createMenu(MenuInput input);
    Map<String,Object> category(UUID menuId, CategoryInput input);
    Map<String,Object> item(UUID categoryId, ItemInput input);
    Map<String,Object> variant(UUID itemId, VariantInput input);
    List<Map<String,Object>> menu(UUID branchId);
    Map<String,Object> setPrice(UUID branchId, PriceInput input);
    Map<String,Object> updatePrice(UUID branchId, UUID priceId, PriceInput input);
    List<Map<String,Object>> prices(UUID branchId);
    Map<String,Object> order(UUID branchId, OrderInput input);
    List<Map<String,Object>> orders(UUID branchId);
    Map<String,Object> getOrder(UUID branchId, UUID orderId);
    Map<String,Object> payment(UUID branchId, PaymentInput input);
    List<Map<String,Object>> payments(UUID branchId);
    List<Map<String,Object>> centralPayments();
    Map<String,Object> report(UUID branchId);
    List<Map<String,Object>> reports();
}
