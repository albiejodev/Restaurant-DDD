package com.restaurant.ddd.domain.pricing;

import com.restaurant.ddd.domain.shared.CustomerType;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Branch-specific price for one menu variant and customer type. */
public record BranchPrice(UUID id, UUID branchId, UUID menuVariantId, CustomerType customerType,
        BigDecimal amount, String currency, boolean active) {
    public BranchPrice {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(branchId, "branchId");
        Objects.requireNonNull(menuVariantId, "menuVariantId");
        Objects.requireNonNull(customerType, "customerType");
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) throw new IllegalArgumentException("Price cannot be negative");
        currency = currency == null || currency.isBlank() ? "INR" : currency.trim().toUpperCase();
        if (!currency.matches("[A-Z]{3}")) throw new IllegalArgumentException("Currency must be a three-letter code");
    }
    public static BranchPrice create(UUID branchId, UUID variantId, CustomerType customerType, BigDecimal amount, String currency) {
        return new BranchPrice(UUID.randomUUID(), branchId, variantId, customerType, amount, currency, true);
    }
}
