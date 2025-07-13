package com.boda.bfffotoappbackend.service.impl;

import com.boda.bfffotoappbackend.dto.CreatePhotoRequest;
import com.boda.bfffotoappbackend.dto.Photo;
import com.boda.bfffotoappbackend.service.PhotoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PhotoServiceImpl implements PhotoService {

    private final WebClient supabaseWebClient;
    private final String supabaseServiceKey;

    @Value("${supabase.url}")
    private String supabaseUrl;
    private final String BUCKET_NAME = "wedding-photos";

    public PhotoServiceImpl(WebClient supabaseWebClient, @Value("${supabase.service.key}") String supabaseServiceKey) {
        this.supabaseWebClient = supabaseWebClient;
        this.supabaseServiceKey = supabaseServiceKey;
    }

    @Override
    public List<Photo> getAllPhotos() {
        // Usamos Flux para recibir una lista de objetos Photo
        Flux<Photo> photosFlux = supabaseWebClient.get()
                .uri("/rest/v1/photos?select=*") // Endpoint REST para la tabla "photos"
                .header("Authorization", "Bearer "+ this.supabaseServiceKey) // Usamos la Service Key para autenticar esta llamada
                .retrieve()
                .bodyToFlux(Photo.class);

        // Bloqueamos y convertimos el Flux a una lista para este caso de uso
        return photosFlux.collectList().block();
    }

    @Override
    public Photo uploadPhoto(MultipartFile file, String userId) {
        try {
            // 1. Generar un nombre de archivo único para evitar colisiones
            String originalFileName = Objects.requireNonNull(file.getOriginalFilename(), "El nombre del archivo no puede ser nulo");
            String storagePath = userId + "/" + Instant.now().toEpochMilli() + "_" + originalFileName;

            // 2. Subir el archivo a Supabase Storage
            supabaseWebClient.post()
                    .uri("/storage/v1/object/" + BUCKET_NAME + "/" + storagePath)
                    .header("Authorization", "Bearer " + this.supabaseServiceKey) // Usamos la Service Key para subir
                    .contentType(MediaType.parseMediaType(Objects.requireNonNull(file.getContentType())))
                    .bodyValue(file.getBytes())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            Mono.error(new ResponseStatusException(response.statusCode(), "Error al subir el archivo al storage.")))
                    .toBodilessEntity()
                    .block(); // Esperamos a que la subida se complete

            // 4. Crear el objeto DTO para la inserción en la base de datos
            CreatePhotoRequest newPhotoData = new CreatePhotoRequest();
            newPhotoData.setUserId(userId);
            newPhotoData.setStoragePath(storagePath);

            // 5. Guardar los metadatos en la tabla 'photos' de la base de datos
            Photo savedPhoto = supabaseWebClient.post()
                    .uri("/rest/v1/photos?select=*") // "select=*" para que nos devuelva el objeto creado
                    .header("Authorization", "Bearer " + this.supabaseServiceKey)
                    .header("Prefer", "return=representation") // Importante para que devuelva el objeto
                    .bodyValue(newPhotoData)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            Mono.error(new ResponseStatusException(response.statusCode(), "Error al guardar metadatos de la foto.")))
                    .bodyToFlux(Photo.class)
                    .next()
                    .block(); // Esperamos a que se complete la inserción

            if(savedPhoto != null) {
                String publicUrl = supabaseUrl + "/storage/v1/object/" + BUCKET_NAME + "/" + savedPhoto.getStoragePath();
                savedPhoto.setUrl(publicUrl);
            }
            return savedPhoto;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer el archivo.", e);
        }
    }

    @Override
    public void deletePhoto(Long photoId) {
        Photo photoToDelete = supabaseWebClient.get()
                .uri("/rest/v1/photos?select=storage_path&id=eq." + photoId)
                .header("Authorization", "Bearer " + this.supabaseServiceKey)
                .retrieve()
                .bodyToFlux(Photo.class)
                .singleOrEmpty()
                .block();

        if (photoToDelete == null || photoToDelete.getStoragePath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la foto con el ID proporcionado.");
        }

        Map<String, List<String>> body = new HashMap<>();
        body.put("prefixes", List.of(photoToDelete.getStoragePath()));

        // 2. Borrar el archivo de Supabase Storage
        String storagePath = photoToDelete.getStoragePath();
        supabaseWebClient.method(HttpMethod.DELETE)
                .uri("/storage/v1/object/" + BUCKET_NAME + "/" + storagePath)
                .header("Authorization", "Bearer " + this.supabaseServiceKey)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();

        // 3. Borrar el registro de la base de datos
        supabaseWebClient.delete()
                .uri("/rest/v1/photos?id=eq." + photoId)
                .header("Authorization", "Bearer " + this.supabaseServiceKey)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
