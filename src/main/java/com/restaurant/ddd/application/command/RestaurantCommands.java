package com.restaurant.ddd.application.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import com.restaurant.ddd.domain.shared.CustomerType;
import com.restaurant.ddd.domain.payment.PaymentMethod;
import com.restaurant.ddd.domain.payment.PaymentStatus;

/** HTTP/application input shapes. Domain invariants are still checked by the use cases. */
public final class RestaurantCommands {
    private RestaurantCommands() {}
    public record BranchInput(@NotBlank String code, @NotBlank String name, @NotBlank String city, String address, Boolean active) {}
    public record MasterInput(@NotBlank String code, @NotBlank String name, @NotBlank String type) {}
    public record MasterMenuBranchInput(@NotNull UUID branchId,
            @NotNull @PositiveOrZero BigDecimal guestPrice,
            @NotNull @PositiveOrZero BigDecimal staffPrice) {}
    public record MasterMenuInput(@NotBlank String code, @NotBlank String masterName,
            @NotBlank String menuName, @NotBlank String categoryName,
            @NotBlank String itemName, String description,
            @NotBlank String variantName, String size, String flavour,
            @NotEmpty List<@Valid MasterMenuBranchInput> branches) {}
    public record MenuInput(@NotNull UUID masterId, @NotBlank String name) {}
    public record CategoryInput(@NotBlank String name, Integer sortOrder) {}
    public record ItemInput(@NotBlank String name, String description) {}
    public record VariantInput(@NotBlank String name, String size, String flavour) {}
    public record PriceInput(@NotNull UUID menuVariantId, @NotNull CustomerType customerType, @NotNull @PositiveOrZero BigDecimal amount, String currency) {}
    public record OrderLine(@NotNull UUID menuVariantId, @Positive int quantity) {}
    public record OrderInput(@NotNull CustomerType customerType, @NotEmpty List<@Valid OrderLine> items) {}
    public record PaymentInput(@NotNull UUID orderId, @NotNull @Positive BigDecimal amount, @NotNull PaymentMethod method, PaymentStatus status) {}
}


//think of this as nestjs dto 
// this is just request data structures 
