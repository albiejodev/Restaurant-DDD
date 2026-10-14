package com.restaurant.ddd.domain.order;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** One immutable purchase-time price snapshot for an order line. */
public record OrderItem(UUID menuVariantId, int quantity, BigDecimal unitPrice) {
    public OrderItem {
        Objects.requireNonNull(menuVariantId, "menuVariantId");
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (quantity <= 0) throw new IllegalArgumentException("Order quantity must be positive");
        if (unitPrice.signum() < 0) throw new IllegalArgumentException("Unit price cannot be negative");
    }
    public BigDecimal lineTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
