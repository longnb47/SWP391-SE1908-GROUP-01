package com.se1908.group01.controller;

import com.se1908.group01.dto.CreatePlanRequest;
import com.se1908.group01.dto.UpdatePlanRequest;
import com.se1908.group01.entity.SubscriptionPlan;
import com.se1908.group01.service.SubscriptionPlanService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscription-plans")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionPlanController {

    private final SubscriptionPlanService service;

    /**
     * Create new subscription plan
     */
    @PostMapping
    public ResponseEntity<SubscriptionPlan> create(
            @RequestBody CreatePlanRequest request) {

        return ResponseEntity.ok(
                service.create(request)
        );
    }

    /**
     * Get all active plans
     */
    @GetMapping
    public ResponseEntity<List<SubscriptionPlan>> getAll() {

        return ResponseEntity.ok(
                service.getAll()
        );
    }

    /**
     * Get plan by id
     */
    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionPlan> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                service.getById(id)
        );
    }

    /**
     * Update plan
     */
    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionPlan> update(
            @PathVariable Long id,
            @RequestBody UpdatePlanRequest request) {

        return ResponseEntity.ok(
                service.update(id, request)
        );
    }

    /**
     * Soft delete plan
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.ok(
                "Subscription plan deleted successfully"
        );
    }
}