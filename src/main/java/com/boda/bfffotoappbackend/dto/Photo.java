package com.boda.bfffotoappbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class Photo {

    private Long id;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("storage_path") // Mapea el nombre de la columna en Supabase al campo de la clase
    private String storagePath;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    // Podemos añadir un campo transitorio para la URL completa si el frontend la necesita
    @JsonProperty("url")
    private String url;
}
