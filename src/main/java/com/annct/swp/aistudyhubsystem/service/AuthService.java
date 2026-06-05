package com.annct.swp.aistudyhubsystem.service;

import com.annct.swp.aistudyhubsystem.dto.LoginDto;

public interface AuthService {
    LoginDto.Response login(LoginDto.Request request);
}
