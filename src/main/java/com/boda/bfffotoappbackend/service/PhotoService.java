package com.boda.bfffotoappbackend.service;

import com.boda.bfffotoappbackend.dto.Photo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PhotoService {

    public List<Photo> getAllPhotos();
    public Photo uploadPhoto(MultipartFile file, String userId);
}
