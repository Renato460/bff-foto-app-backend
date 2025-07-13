package com.boda.bfffotoappbackend.auth.dto;

import lombok.Data;

@Data
public class SupabaseUser {
    private String id; // El UUID del usuario que necesitamos
    private String email;
}
