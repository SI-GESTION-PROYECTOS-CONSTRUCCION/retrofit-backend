package com.retrofit.backend.service.impl;

import com.retrofit.backend.service.StorageService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//@Service
public class LocalStorageServiceImpl implements StorageService {

    private final String UPLOAD_DIR = "uploads/";

    public LocalStorageServiceImpl() {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de subidas", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        try {
            // Generamos un nombre único para evitar que fotos con el mismo nombre se
            // chanquen
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString() + extension;

            Path filePath = Paths.get(UPLOAD_DIR + newFilename);
            Files.copy(file.getInputStream(), filePath);

            // Devolvemos la URL local que el Frontend usará para mostrar la imagen
            return "http://localhost:8080/api/v1/uploads/" + newFilename;

        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el archivo localmente", e);
        }
    }

    @Override
    public List<String> storeMultiple(List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty())
                    urls.add(store(file));
            }
        }
        return urls;
    }
}