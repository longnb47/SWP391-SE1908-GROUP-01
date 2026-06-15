package com.se1908.group01.controller;

import com.se1908.group01.dto.CreatePlanRequest;
import com.se1908.group01.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService service;

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody CreatePlanRequest request) {

        return ResponseEntity.ok(
                service.create(request));
    }
}