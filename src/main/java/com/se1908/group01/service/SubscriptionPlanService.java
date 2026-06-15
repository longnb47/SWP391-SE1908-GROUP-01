package com.se1908.group01.service;

import com.se1908.group01.dto.CreatePlanRequest;
import com.se1908.group01.entity.SubscriptionPlan;
import com.se1908.group01.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository repository;

    public SubscriptionPlan create(
            CreatePlanRequest request) {

        SubscriptionPlan plan =
                SubscriptionPlan.builder()
                        .name(request.getName())
                        .price(request.getPrice())
                        .durationDays(request.getDurationDays())
                        .description(request.getDescription())
                        .active(true)
                        .build();

        return repository.save(plan);
    }
}