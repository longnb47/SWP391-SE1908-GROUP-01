package com.se1908.group01.service;

import com.se1908.group01.dto.request.VerifyOtpRequest;
import com.se1908.group01.dto.response.RegisterResponse;
import com.se1908.group01.dto.request.RegisterRequest;
import com.se1908.group01.dto.response.VerifyOtpResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);
    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);

}
