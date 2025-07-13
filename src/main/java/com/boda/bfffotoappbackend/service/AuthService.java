package com.boda.bfffotoappbackend.service;

import com.boda.bfffotoappbackend.auth.dto.LoginRequest;

public interface AuthService {
    public String login(LoginRequest request);
}
