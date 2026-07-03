package com.se1908.group01.dto;

import lombok.Data;

@Data
public class CreatePlanRequest {

    private String name;

    private Double price;

    private Integer durationDays;

    private String description;

    private Integer storageLimitGb;

    private String allowedFormats;

    private Integer maxUploadSizeMb;

    private Boolean multipleDocuments;

    private Boolean videoUpload;

    private Long monthlyTokenLimit;
}