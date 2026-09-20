package br.com.vistoriapredial.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários de {@link LocalStorageService}.
 *
 * <p>Usa {@code @TempDir} para criar um diretório temporário isolado por teste,
 * garantindo que nenhum arquivo persista entre os cenários.</p>
 */
class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageService(tempDir.toString());
    }

    @Test
    void store_shouldPersistFileAndReturnRelativePath() throws IOException {
        // arrange
        byte[] content = "conteudo-de-foto-simulado".getBytes();
        MockMultipartFile file = new MockMultipartFile("foto", "cozinha.jpg", "image/jpeg", content);

        // act
        String relativePath = storageService.store(file, "1_cozinha.jpg");

        // assert
        assertThat(relativePath).endsWith("1_cozinha.jpg");

        Path storedFile = tempDir.resolve("1_cozinha.jpg");
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(Files.readAllBytes(storedFile)).isEqualTo(content);
    }

    @Test
    void store_shouldOverwriteExistingFileWithSameName() throws IOException {
        // arrange
        MockMultipartFile first  = new MockMultipartFile("foto", "f.jpg", "image/jpeg", "original".getBytes());
        MockMultipartFile second = new MockMultipartFile("foto", "f.jpg", "image/jpeg", "atualizado".getBytes());

        storageService.store(first, "duplicado.jpg");

        // act
        storageService.store(second, "duplicado.jpg");

        // assert
        Path storedFile = tempDir.resolve("duplicado.jpg");
        assertThat(new String(Files.readAllBytes(storedFile))).isEqualTo("atualizado");
    }

    @Test
    void store_shouldThrowStorageException_whenFileIsEmpty() {
        // arrange
        MockMultipartFile emptyFile = new MockMultipartFile("foto", new byte[0]);

        // act + assert
        assertThatThrownBy(() -> storageService.store(emptyFile, "vazio.jpg"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("vazio");
    }

    @Test
    void store_shouldThrowStorageException_onPathTraversalAttempt() {
        // arrange
        byte[] content = "conteudo".getBytes();
        MockMultipartFile file = new MockMultipartFile("foto", "../etc/passwd", "image/jpeg", content);

        // act + assert
        assertThatThrownBy(() -> storageService.store(file, "../etc/passwd"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("path traversal");
    }

    @Test
    void delete_shouldRemoveExistingFile() throws IOException {
        // arrange
        byte[] content = "conteudo".getBytes();
        MockMultipartFile file = new MockMultipartFile("foto", "para-excluir.jpg", "image/jpeg", content);
        String relativePath = storageService.store(file, "para-excluir.jpg");

        // act
        storageService.delete(relativePath);

        // assert
        Path deletedFile = tempDir.resolve("para-excluir.jpg");
        assertThat(Files.exists(deletedFile)).isFalse();
    }

    @Test
    void delete_shouldNotThrow_whenFileDoesNotExist() {
        // act + assert: delete é idempotente para arquivos inexistentes
        String pathInexistente = tempDir.resolve("inexistente.jpg").toString();
        assertThatCode(() -> storageService.delete(pathInexistente))
                .doesNotThrowAnyException();
    }

    @Test
    void load_shouldReturnContentTypeAndLengthFromControlledExtension() throws IOException {
        byte[] content = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01};
        Files.write(tempDir.resolve("vistoria.jpg"), content);

        StoredFile storedFile = storageService.load(tempDir.getFileName() + "/vistoria.jpg");

        assertThat(storedFile.resource().getContentAsByteArray()).isEqualTo(content);
        assertThat(storedFile.mediaType()).isEqualTo(MediaType.IMAGE_JPEG);
        assertThat(storedFile.length()).isEqualTo(content.length);
    }

    @Test
    void load_shouldRejectPathTraversal() {
        assertThatThrownBy(() -> storageService.load("../segredo.jpg"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    void load_shouldReturnTypedNotFoundWithoutPhysicalPath() {
        assertThatThrownBy(() -> storageService.load("uploads/inexistente.jpg"))
                .isInstanceOf(StorageFileNotFoundException.class)
                .hasMessage("Arquivo de evidência não encontrado.")
                .hasMessageNotContaining(tempDir.toString());
    }

    @Test
    void load_shouldRejectUnknownExtension() throws IOException {
        Files.writeString(tempDir.resolve("vistoria.gif"), "gif");

        assertThatThrownBy(() -> storageService.load("vistoria.gif"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("tipo");
    }
}
