package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class JwtAuthResponse {
    private String accessToken;
    private String tokenType = "Bearer"; // Quy chuẩn quốc tế cho JWT

    public JwtAuthResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}