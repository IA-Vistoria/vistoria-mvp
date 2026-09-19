package br.com.vistoriapredial.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Implementação de {@link StorageService} que persiste arquivos no sistema
 * de arquivos local do servidor.
 *
 * <p>Usada em desenvolvimento e testes de integração locais. O diretório
 * base é configurável via {@code storage.upload-dir} em {@code application.properties}.</p>
 */
@Service
public class LocalStorageService implements StorageService {

    private final Path uploadDir;

    public LocalStorageService(@Value("${storage.upload-dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        initializeUploadDir();
    }

    /**
     * {@inheritDoc}
     *
     * <p>O arquivo é gravado atomicamente com {@link StandardCopyOption#REPLACE_EXISTING},
     * portanto uma nova chamada com o mesmo {@code fileName} sobrescreve o anterior.</p>
     */
    @Override
    public String store(MultipartFile file, String fileName) {
        validateFile(file, fileName);

        try {
            Path destination = uploadDir.resolve(fileName).normalize();
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return uploadDir.getFileName().toString() + "/" + fileName;
        } catch (IOException exception) {
            throw new StorageException("Falha ao gravar arquivo: " + fileName, exception);
        }
    }

    @Override
    public void delete(String relativePath) {
        try {
            String fileName = Paths.get(relativePath).getFileName().toString();
            Files.deleteIfExists(uploadDir.resolve(fileName).normalize());
        } catch (IOException exception) {
            throw new StorageException("Falha ao excluir arquivo: " + relativePath, exception);
        }
    }

    private void validateFile(MultipartFile file, String fileName) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Arquivo de foto não pode estar vazio");
        }

        Path destination = uploadDir.resolve(fileName).normalize();
        if (!destination.startsWith(uploadDir)) {
            throw new StorageException(
                    "Nome de arquivo inválido — tentativa de path traversal detectada: " + fileName);
        }
    }

    private void initializeUploadDir() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException exception) {
            throw new StorageException(
                    "Não foi possível criar o diretório de upload: " + uploadDir, exception);
        }
    }
}
