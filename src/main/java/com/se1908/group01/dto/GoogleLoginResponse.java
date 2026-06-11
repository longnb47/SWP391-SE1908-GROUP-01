package com.se1908.group01.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GoogleLoginResponse {

    private String messgae;
    private String email;
    private  String fullName;

}
