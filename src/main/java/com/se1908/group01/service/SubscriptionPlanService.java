package com.se1908.group01.service;

import com.se1908.group01.dto.CreatePlanRequest;
import com.se1908.group01.dto.UpdatePlanRequest;
import com.se1908.group01.entity.SubscriptionPlan;
import com.se1908.group01.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository repository;

    // CREATE
    public SubscriptionPlan create(CreatePlanRequest request) {

        if (repository.existsByNameAndActiveTrue(request.getName())) {
            throw new RuntimeException("Subscription plan name already exists.");
        }

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(request.getName())
                .price(request.getPrice())
                .durationDays(request.getDurationDays())
                .description(request.getDescription())

                // ===== STORAGE =====
                .storageLimitGb(request.getStorageLimitGb())
                .allowedFormats(request.getAllowedFormats())
                .maxUploadSizeMb(request.getMaxUploadSizeMb())
                .multipleDocuments(request.getMultipleDocuments())
                .videoUpload(request.getVideoUpload())
                .monthlyTokenLimit(request.getMonthlyTokenLimit())
                .active(true)
                .build();

        return repository.save(plan);
    }

    // GET ALL ACTIVE PLANS
    public List<SubscriptionPlan> getAll() {
        return repository.findByActiveTrue();
    }

    // GET PLAN BY ID
    public SubscriptionPlan getById(Long id) {

        SubscriptionPlan plan = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Subscription plan not found."));

        if (!plan.isActive()) {
            throw new RuntimeException("Subscription plan has been deleted.");
        }

        return plan;
    }

    // UPDATE
    public SubscriptionPlan update(
            Long id,
            UpdatePlanRequest request) {

        SubscriptionPlan plan = getById(id);

        if (!plan.getName().equalsIgnoreCase(request.getName())
                && repository.existsByNameAndActiveTrue(request.getName())) {

            throw new RuntimeException("Subscription plan name already exists.");
        }

        plan.setName(request.getName());
        plan.setPrice(request.getPrice());
        plan.setDurationDays(request.getDurationDays());
        plan.setDescription(request.getDescription());

        // ===== STORAGE =====
        plan.setStorageLimitGb(request.getStorageLimitGb());
        plan.setAllowedFormats(request.getAllowedFormats());
        plan.setMaxUploadSizeMb(request.getMaxUploadSizeMb());
        plan.setMultipleDocuments(request.getMultipleDocuments());
        plan.setVideoUpload(request.getVideoUpload());
        plan.setMonthlyTokenLimit(
                request.getMonthlyTokenLimit()
        );
        return repository.save(plan);
    }

    // SOFT DELETE
    public void delete(Long id) {

        SubscriptionPlan plan = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Subscription plan not found."));

        if (!plan.isActive()) {
            throw new RuntimeException("Subscription plan already deleted.");
        }

        plan.setActive(false);

        repository.save(plan);
    }
}