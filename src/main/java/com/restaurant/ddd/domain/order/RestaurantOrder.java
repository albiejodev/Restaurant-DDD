package com.restaurant.ddd.domain.order;

import com.restaurant.ddd.domain.shared.CustomerType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Order aggregate: owns its immutable line-price snapshots and total. */
public record RestaurantOrder(UUID id, UUID branchId, CustomerType customerType, List<OrderItem> items) {
    public RestaurantOrder {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(branchId, "branchId");
        Objects.requireNonNull(customerType, "customerType");
        items = List.copyOf(items);
        if (items.isEmpty()) throw new IllegalArgumentException("An order must contain at least one item");
    }
    public BigDecimal totalAmount() {
        return items.stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
