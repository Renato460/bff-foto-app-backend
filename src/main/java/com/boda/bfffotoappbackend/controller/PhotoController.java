package com.boda.bfffotoappbackend.controller;

import com.boda.bfffotoappbackend.dto.Photo;
import com.boda.bfffotoappbackend.security.JwtService;
import com.boda.bfffotoappbackend.service.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {

    private final PhotoService photoService;
    private final JwtService jwtService;

    @Autowired
    public PhotoController(PhotoService photoService, JwtService jwtService) {
        this.photoService = photoService;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<List<Photo>> listAllPhotos() {
        List<Photo> photos = photoService.getAllPhotos();
        return ResponseEntity.ok(photos);
    }

    @PostMapping("/upload")
    public ResponseEntity<Photo> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(name = "Authorization") String authHeader) { // Spring nos inyecta al usuario autenticado

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Extraemos el token del header "Bearer <token>"
        String token = authHeader.substring(7);
        // Extraemos la claim "userId" del token
        String userId = jwtService.extractAllClaims(token).get("userId", String.class);

        Photo newPhoto = photoService.uploadPhoto(file, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(newPhoto);
    }
}
