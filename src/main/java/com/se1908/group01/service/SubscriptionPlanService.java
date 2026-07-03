package com.se1908.group01.service;

import com.se1908.group01.dto.CreatePlanRequest;
import com.se1908.group01.dto.SubscriptionPlanResponse;
import com.se1908.group01.dto.UpdatePlanRequest;
import com.se1908.group01.entity.SubscriptionPlan;
import com.se1908.group01.exception.ConflictException;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionPlanService {

    private final SubscriptionPlanRepository repository;

    public SubscriptionPlanResponse create(CreatePlanRequest request) {
        String normalizedName = normalizeName(request.getName());

        if (repository.existsByNameIgnoreCaseAndActiveTrue(normalizedName)) {
            throw new ConflictException("An active subscription plan with this name already exists");
        }

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(normalizedName)
                .price(request.getPrice())
                .durationDays(request.getDurationDays())
                .description(normalizeOptionalText(request.getDescription()))
                .storageLimitGb(request.getStorageLimitGb())
                .allowedFormats(request.getAllowedFormats().trim())
                .maxUploadSizeMb(request.getMaxUploadSizeMb())
                .multipleDocuments(request.getMultipleDocuments())
                .videoUpload(request.getVideoUpload())
                .monthlyTokenLimit(request.getMonthlyTokenLimit())
                .active(true)
                .build();

        return toResponse(repository.save(plan));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getAll() {
        return repository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getById(Long id) {
        return toResponse(findActivePlan(id));
    }

    public SubscriptionPlanResponse update(
            Long id,
            UpdatePlanRequest request) {

        SubscriptionPlan plan = findActivePlan(id);
        String normalizedName = normalizeName(request.getName());

        if (repository.existsByNameIgnoreCaseAndActiveTrueAndIdNot(
                normalizedName,
                id)) {
            throw new ConflictException("An active subscription plan with this name already exists");
        }

        plan.setName(normalizedName);
        plan.setPrice(request.getPrice());
        plan.setDurationDays(request.getDurationDays());
        plan.setDescription(normalizeOptionalText(request.getDescription()));
        plan.setStorageLimitGb(request.getStorageLimitGb());
        plan.setAllowedFormats(request.getAllowedFormats().trim());
        plan.setMaxUploadSizeMb(request.getMaxUploadSizeMb());
        plan.setMultipleDocuments(request.getMultipleDocuments());
        plan.setVideoUpload(request.getVideoUpload());
        plan.setMonthlyTokenLimit(request.getMonthlyTokenLimit());

        return toResponse(repository.save(plan));
    }

    public void delete(Long id) {
        SubscriptionPlan plan = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subscription plan not found"));

        if (!plan.isActive()) {
            throw new ConflictException("Subscription plan is already deleted");
        }

        plan.setActive(false);
        repository.save(plan);
    }

    private SubscriptionPlan findActivePlan(Long id) {
        SubscriptionPlan plan = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subscription plan not found"));

        if (!plan.isActive()) {
            throw new ResourceNotFoundException("Subscription plan not found");
        }

        return plan;
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private SubscriptionPlanResponse toResponse(SubscriptionPlan plan) {
        return SubscriptionPlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .price(plan.getPrice())
                .durationDays(plan.getDurationDays())
                .description(plan.getDescription())
                .storageLimitGb(plan.getStorageLimitGb())
                .allowedFormats(plan.getAllowedFormats())
                .maxUploadSizeMb(plan.getMaxUploadSizeMb())
                .multipleDocuments(plan.getMultipleDocuments())
                .videoUpload(plan.getVideoUpload())
                .monthlyTokenLimit(plan.getMonthlyTokenLimit())
                .active(plan.isActive())
                .build();
    }
}
