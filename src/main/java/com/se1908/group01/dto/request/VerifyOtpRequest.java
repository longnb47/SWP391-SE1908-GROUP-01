package com.se1908.group01.dto.request;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class VerifyOtpRequest {

    private String email;
    private String otp;

}
