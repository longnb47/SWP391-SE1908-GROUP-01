package com.se1908.group01.service;

import com.se1908.group01.dto.VerifyOtpRequest;
import com.se1908.group01.dto.RegisterResponse;
import com.se1908.group01.dto.RegisterRequest;
import com.se1908.group01.dto.VerifyOtpResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);
    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);
}
