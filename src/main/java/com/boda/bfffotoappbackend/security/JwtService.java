package com.boda.bfffotoappbackend.security;


import io.jsonwebtoken.Claims;

public interface JwtService {
    public String generateToken(String username, String role, String userId);
    public boolean isTokenValid(String token, String username);
    public String extractUsername(String token);
    public Claims extractAllClaims(String token);
}
