package com.restaurant.ddd.application.service;

import com.restaurant.ddd.application.command.RestaurantCommands.*;
import com.restaurant.ddd.application.port.in.RestaurantUseCases;
import com.restaurant.ddd.application.port.out.RestaurantPersistencePort;
import com.restaurant.ddd.domain.order.OrderItem;
import com.restaurant.ddd.domain.order.RestaurantOrder;
import com.restaurant.ddd.domain.branch.Branch;
import com.restaurant.ddd.domain.pricing.BranchPrice;
import com.restaurant.ddd.domain.shared.CustomerType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

/** Application boundary: coordinates API use cases through an outbound persistence port. */
@Service
public class RestaurantService implements RestaurantUseCases {
    private final RestaurantPersistencePort persistence;
    public RestaurantService(RestaurantPersistencePort persistence) { this.persistence = persistence; }

    public Map<String,Object> createBranch(BranchInput input) {
        Branch branch = Branch.create(input.code(),input.name(),input.city(),input.address(),input.active()==null||input.active());
        return persistence.createBranch(branch);
    }
    
    public Map<String,Object> updateBranch(UUID id,BranchInput input) {
        Branch branch = new Branch(id,input.code(),input.name(),input.city(),input.address(),input.active()==null||input.active());
        return persistence.updateBranch(id,branch);
    }
    public List<Map<String,Object>> branches() { return persistence.branches(); }
    public Map<String,Object> getBranch(UUID id) { return persistence.getBranch(id); }
    public Map<String,Object> createMaster(MasterInput input) { return persistence.createMaster(input); }
    @Transactional
    public Map<String,Object> createMasterMenu(MasterMenuInput input) {
        Set<UUID> branchIds = new HashSet<>();
        for (MasterMenuBranchInput branch : input.branches()) {
            if (!branchIds.add(branch.branchId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A branch can only be selected once");
            if (!persistence.branchExists(branch.branchId()))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found: " + branch.branchId());
        }

        Map<String,Object> master = persistence.createMaster(new MasterInput(input.code(), input.masterName(), "MENU"));
        UUID masterId = UUID.fromString(master.get("id").toString());
        Map<String,Object> menu = persistence.createMenu(new MenuInput(masterId, input.menuName()));
        UUID menuId = UUID.fromString(menu.get("id").toString());
        Map<String,Object> category = persistence.category(menuId, new CategoryInput(input.categoryName(), 0));
        UUID categoryId = UUID.fromString(category.get("id").toString());
        Map<String,Object> item = persistence.item(categoryId, new ItemInput(input.itemName(), input.description()));
        UUID itemId = UUID.fromString(item.get("id").toString());
        Map<String,Object> variant = persistence.variant(itemId,
                new VariantInput(input.variantName(), input.size(), input.flavour()));
        UUID variantId = UUID.fromString(variant.get("id").toString());

        List<Map<String,Object>> branchPricing = new ArrayList<>();
        for (MasterMenuBranchInput branch : input.branches()) {
            persistence.enable(branch.branchId(), masterId);
            Map<String,Object> guest = setPrice(branch.branchId(),
                    new PriceInput(variantId, CustomerType.GUEST, branch.guestPrice(), "INR"));
            Map<String,Object> staff = setPrice(branch.branchId(),
                    new PriceInput(variantId, CustomerType.STAFF, branch.staffPrice(), "INR"));
            branchPricing.add(Map.of("branchId", branch.branchId(), "guestPrice", guest, "staffPrice", staff));
        }
        return Map.of("master", master, "menu", menu, "variant", variant, "branchPricing", branchPricing);
    }
    public List<Map<String,Object>> masters() { return persistence.masters(); }
    public Map<String,Object> getMaster(UUID id) { return persistence.getMaster(id); }
    public Map<String,Object> enable(UUID branchId,UUID masterId) { return persistence.enable(branchId,masterId); }
    public void disable(UUID branchId,UUID masterId) { persistence.disable(branchId,masterId); }
    public List<Map<String,Object>> assigned(UUID branchId) { return persistence.assigned(branchId); }
    public Map<String,Object> createMenu(MenuInput input) { return persistence.createMenu(input); }
    public Map<String,Object> category(UUID menuId,CategoryInput input) { return persistence.category(menuId,input); }
    public Map<String,Object> item(UUID categoryId,ItemInput input) { return persistence.item(categoryId,input); }
    public Map<String,Object> variant(UUID itemId,VariantInput input) { return persistence.variant(itemId,input); }
    public List<Map<String,Object>> menu(UUID branchId) { return persistence.menu(branchId); }
    public Map<String,Object> setPrice(UUID branchId,PriceInput input) {
        if (!persistence.branchExists(branchId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Branch not found");
        if (!persistence.variantEnabled(branchId,input.menuVariantId())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Menu variant is not enabled for this branch");
        BranchPrice price = BranchPrice.create(branchId,input.menuVariantId(),input.customerType(),input.amount(),input.currency());
        return persistence.setPrice(branchId,price);
    }
    public Map<String,Object> updatePrice(UUID branchId,UUID priceId,PriceInput input) {
        if (!persistence.branchExists(branchId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Branch not found");
        if (!persistence.variantEnabled(branchId,input.menuVariantId())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Menu variant is not enabled for this branch");
        BranchPrice price = new BranchPrice(priceId,branchId,input.menuVariantId(),input.customerType(),input.amount(),input.currency(),true);
        return persistence.updatePrice(branchId,priceId,price);
    }
    public List<Map<String,Object>> prices(UUID branchId) { return persistence.prices(branchId); }
    @Transactional
    public Map<String,Object> order(UUID branchId,OrderInput input) {
        if (!persistence.branchExists(branchId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Branch not found");
        List<OrderItem> items = new ArrayList<>();
        for (OrderLine requested : input.items()) {
            if (!persistence.variantEnabled(branchId,requested.menuVariantId()))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Menu variant is not enabled for this branch");
            BigDecimal price = persistence.currentPrice(branchId,requested.menuVariantId(),input.customerType());
            items.add(new OrderItem(requested.menuVariantId(),requested.quantity(),price));
        }
        RestaurantOrder order = new RestaurantOrder(UUID.randomUUID(),branchId,input.customerType(),items);
        persistence.saveOrder(order);
        return persistence.orderDetails(branchId,order.id());
    }
    public List<Map<String,Object>> orders(UUID branchId) { return persistence.orders(branchId); }
    public Map<String,Object> getOrder(UUID branchId,UUID orderId) { return persistence.orderDetails(branchId,orderId); }
    public Map<String,Object> payment(UUID branchId,PaymentInput input) {
        if (!persistence.branchExists(branchId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Branch not found");
        if (!persistence.orderBelongsToBranch(branchId,input.orderId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Order not found");
        return persistence.savePayment(branchId,input);
    }
    public List<Map<String,Object>> payments(UUID branchId) { return persistence.payments(branchId); }
    public List<Map<String,Object>> centralPayments() { return persistence.allPayments(); }
    public Map<String,Object> report(UUID branchId) { return persistence.report(branchId); }
    public List<Map<String,Object>> reports() { return persistence.reports(); }
}
