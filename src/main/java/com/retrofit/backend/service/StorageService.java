package com.retrofit.backend.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface StorageService {
    String store(MultipartFile file);
    List<String> storeMultiple(List<MultipartFile> files);
}