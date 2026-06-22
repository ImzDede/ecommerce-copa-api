package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.exception.AppException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadService {

    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path uploadRoot;

    public UploadService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String saveImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Arquivo de imagem é obrigatório", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        String extension = ALLOWED_IMAGE_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new AppException("UNSUPPORTED_MEDIA_TYPE", "Formato de imagem não suportado", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        String filename = UUID.randomUUID() + extension;
        Path targetDirectory = uploadRoot.resolve(folder).normalize();
        Path targetFile = targetDirectory.resolve(filename).normalize();

        if (!targetFile.startsWith(targetDirectory)) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Caminho de upload inválido", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        try {
            Files.createDirectories(targetDirectory);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new AppException("INTERNAL_ERROR", "Não foi possível salvar a imagem", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return "/uploads/" + folder + "/" + filename;
    }

    public void deleteByPublicPath(String publicPath) {
        if (publicPath == null || publicPath.isBlank() || !publicPath.startsWith("/uploads/")) {
            return;
        }

        Path targetFile = uploadRoot.resolve(publicPath.substring("/uploads/".length())).normalize();
        if (!targetFile.startsWith(uploadRoot)) {
            return;
        }

        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException ignored) {
            // A falha de limpeza não deve impedir a atualização do cadastro.
        }
    }
}
