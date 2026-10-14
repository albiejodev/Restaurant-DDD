package com.restaurant.ddd.domain.branch;

import java.util.UUID;

/** Explicit branch-specific availability of centrally owned master data. */
public record BranchMasterAssignment(UUID id, UUID branchId, UUID masterId, boolean enabled) {}
