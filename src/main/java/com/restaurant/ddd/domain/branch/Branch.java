package com.restaurant.ddd.domain.branch;

import java.util.Objects;
import java.util.UUID;

/** Branch identity and core details. */
public record Branch(UUID id, String code, String name, String city, String address, boolean active) {
    public Branch {
        Objects.requireNonNull(id, "id");
        code = required(code, "Branch code").toUpperCase();
        name = required(name, "Branch name");
        city = required(city, "Branch city");
    }
    public static Branch create(String code, String name, String city, String address, boolean active) {
        return new Branch(UUID.randomUUID(), code, name, city, address, active);
    }
    private static String required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value.trim();
    }
}
