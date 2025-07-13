package com.boda.bfffotoappbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePhotoRequest {
    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("storage_path")
    private String storagePath;
}
