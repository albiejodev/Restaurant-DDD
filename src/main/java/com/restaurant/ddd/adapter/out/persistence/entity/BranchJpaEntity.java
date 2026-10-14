package com.restaurant.ddd.adapter.out.persistence.entity;

import com.restaurant.ddd.domain.branch.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Database representation of Branch. Kept outside the domain package. */
@Entity
@Table(name = "branch")
public class BranchJpaEntity {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true, length = 30)
    private String code;
    @Column(nullable = false, length = 160)
    private String name;
    @Column(nullable = false, length = 100)
    private String city;
    @Column(length = 300)
    private String address;
    @Column(nullable = false)
    private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BranchJpaEntity() {}

    public static BranchJpaEntity from(Branch branch) {
        BranchJpaEntity entity = new BranchJpaEntity();
        entity.id = branch.id();
        entity.code = branch.code();
        entity.name = branch.name();
        entity.city = branch.city();
        entity.address = branch.address();
        entity.active = branch.active();
        return entity;
    }

    @PrePersist
    void onInsert() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public void updateFrom(Branch branch) {
        code = branch.code();
        name = branch.name();
        city = branch.city();
        address = branch.address();
        active = branch.active();
    }
}
