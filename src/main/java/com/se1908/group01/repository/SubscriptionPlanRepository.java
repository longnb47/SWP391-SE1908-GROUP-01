package com.se1908.group01.repository;

import com.se1908.group01.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository
        extends JpaRepository<SubscriptionPlan, Long> {
}