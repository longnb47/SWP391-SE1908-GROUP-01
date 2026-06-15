package com.se1908.group01.dto;
import lombok.Data;

@Data
public class PurchaseRequest {

    private Long planId;
    private String paymentMethod;
}
