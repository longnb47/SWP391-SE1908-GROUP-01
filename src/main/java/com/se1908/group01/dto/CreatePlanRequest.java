package com.se1908.group01.dto;

import lombok.Data;

@Data
public class CreatePlanRequest {

    private String name;
    private Double price;
    private Integer durationDays;
    private String description;
}