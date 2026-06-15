package com.se1908.group01.repository;

import com.se1908.group01.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository
        extends JpaRepository<Subscription, Long> {
}