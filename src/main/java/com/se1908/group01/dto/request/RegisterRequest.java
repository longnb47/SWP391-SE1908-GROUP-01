package com.se1908.group01.dto.request;


import lombok.Data;

@Data
public class RegisterRequest {

    private String fullName;
    private String email;
    private String password;

}
