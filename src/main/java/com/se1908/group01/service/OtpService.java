package com.se1908.group01.service;

import com.se1908.group01.dto.request.VerifyOtpRequest;
import com.se1908.group01.dto.response.VerifyOtpResponse;

public interface OtpService {

    String genarateOtp();
    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);


}
