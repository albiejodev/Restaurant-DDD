package com.restaurant.ddd.adapter.out.persistence.repository;

import com.restaurant.ddd.adapter.out.persistence.entity.BranchJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for branch identity and lifecycle persistence. */
public interface SpringDataBranchRepository extends JpaRepository<BranchJpaEntity, UUID> {}
