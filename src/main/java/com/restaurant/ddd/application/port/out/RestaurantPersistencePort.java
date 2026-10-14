package com.restaurant.ddd.application.port.out;

import com.restaurant.ddd.application.command.RestaurantCommands.*;
import com.restaurant.ddd.domain.order.RestaurantOrder;
import com.restaurant.ddd.domain.branch.Branch;
import com.restaurant.ddd.domain.pricing.BranchPrice;
import com.restaurant.ddd.domain.shared.CustomerType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Outbound application port implemented by the persistence adapter. */
public interface RestaurantPersistencePort {
    Map<String,Object> createBranch(Branch branch); Map<String,Object> updateBranch(UUID id,Branch branch); List<Map<String,Object>> branches(); Map<String,Object> getBranch(UUID id);
    Map<String,Object> createMaster(MasterInput input); List<Map<String,Object>> masters(); Map<String,Object> getMaster(UUID id);
    Map<String,Object> enable(UUID branchId,UUID masterId); void disable(UUID branchId,UUID masterId); List<Map<String,Object>> assigned(UUID branchId);
    Map<String,Object> createMenu(MenuInput input); Map<String,Object> category(UUID menuId,CategoryInput input); Map<String,Object> item(UUID categoryId,ItemInput input); Map<String,Object> variant(UUID itemId,VariantInput input);
    List<Map<String,Object>> menu(UUID branchId);
    Map<String,Object> setPrice(UUID branchId,BranchPrice price); Map<String,Object> updatePrice(UUID branchId,UUID priceId,BranchPrice price); List<Map<String,Object>> prices(UUID branchId);
    boolean branchExists(UUID branchId); boolean variantEnabled(UUID branchId,UUID variantId); BigDecimal currentPrice(UUID branchId,UUID variantId,CustomerType customerType);
    void saveOrder(RestaurantOrder order); List<Map<String,Object>> orders(UUID branchId); Map<String,Object> orderDetails(UUID branchId,UUID orderId);
    boolean orderBelongsToBranch(UUID branchId,UUID orderId); Map<String,Object> savePayment(UUID branchId,PaymentInput input); List<Map<String,Object>> payments(UUID branchId); List<Map<String,Object>> allPayments();
    Map<String,Object> report(UUID branchId); List<Map<String,Object>> reports();
}


//eg:
//I dont care who is the db . I need to create a branch 