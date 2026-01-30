package com.omega.shop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path uploadPath;

    public FileStorageService(@Value("${omega.upload.dir}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.uploadPath);
            log.info("Директория для загрузки создана: {}", this.uploadPath);
        } catch (IOException e) {
            log.error("Не удалось создать директорию для загрузки", e);
            throw new RuntimeException("Не удалось создать директорию для загрузки файлов", e);
        }
    }

    /**
     * Сохранить файл и вернуть путь к нему
     */
    public String saveFile(MultipartFile file) {
        try {
            // Проверка на пустой файл
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Файл пустой");
            }

            // Проверка формата файла
            String contentType = file.getContentType();
            if (contentType == null || !isImageFile(contentType)) {
                throw new IllegalArgumentException("Разрешены только изображения (JPEG, PNG, WEBP)");
            }

            // Проверка размера файла (макс 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("Размер файла не должен превышать 5MB");
            }

            // Генерация уникального имени файла
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String filename = UUID.randomUUID().toString() + extension;

            // Сохранение файла
            Path targetLocation = this.uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("Файл успешно сохранён: {}", filename);
            return filename;

        } catch (IOException e) {
            log.error("Ошибка при сохранении файла", e);
            throw new RuntimeException("Не удалось сохранить файл", e);
        }
    }

    /**
     * Удалить файл
     */
    public void deleteFile(String filename) {
        try {
            if (filename != null && !filename.isEmpty()) {
                Path filePath = this.uploadPath.resolve(filename).normalize();
                Files.deleteIfExists(filePath);
                log.info("Файл удалён: {}", filename);
            }
        } catch (IOException e) {
            log.error("Ошибка при удалении файла: {}", filename, e);
        }
    }

    /**
     * Проверка типа файла
     */
    private boolean isImageFile(String contentType) {
        return contentType.equals("image/jpeg")
                || contentType.equals("image/png")
                || contentType.equals("image/webp")
                || contentType.equals("image/jpg");
    }

    /**
     * Получить путь к директории загрузок
     */
    public Path getUploadPath() {
        return uploadPath;
    }
}