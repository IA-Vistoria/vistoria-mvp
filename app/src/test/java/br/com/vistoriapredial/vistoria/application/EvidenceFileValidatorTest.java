package br.com.vistoriapredial.vistoria.application;

import br.com.vistoriapredial.vistoria.application.exception.InvalidEvidenceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvidenceFileValidatorTest {

    private final EvidenceFileValidator validator = new EvidenceFileValidator();

    @ParameterizedTest
    @MethodSource("validEvidenceFiles")
    void shouldAcceptAllowedImageSignatures(String contentType, byte[] content, String extension) {
        MockMultipartFile file = new MockMultipartFile("file", "arquivo", contentType, content);

        ValidatedEvidence result = validator.validate(file);

        assertThat(result.extension()).isEqualTo(extension);
        assertThat(result.mediaType()).isEqualTo(MediaType.parseMediaType(contentType));
    }

    @Test
    void shouldRejectPngMimeWithTextBytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fraude.png", MediaType.IMAGE_PNG_VALUE, "não é png".getBytes());

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(InvalidEvidenceException.class)
                .hasMessageContaining("conteúdo não corresponde");
    }

    @Test
    void shouldRejectFileLargerThanTenMebibytes() {
        byte[] content = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file", "grande.jpg", MediaType.IMAGE_JPEG_VALUE, content);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(InvalidEvidenceException.class)
                .hasMessageContaining("10 MB");
    }

    @Test
    void shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "vazio.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0]);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(InvalidEvidenceException.class)
                .hasMessageContaining("vazio");
    }

    @Test
    void shouldRejectUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "arquivo.gif", MediaType.IMAGE_GIF_VALUE, new byte[] {'G', 'I', 'F'});

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(InvalidEvidenceException.class)
                .hasMessageContaining("JPEG, PNG ou WebP");
    }

    private static Stream<Arguments> validEvidenceFiles() {
        return Stream.of(
                Arguments.of(MediaType.IMAGE_JPEG_VALUE,
                        new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}, ".jpg"),
                Arguments.of(MediaType.IMAGE_PNG_VALUE,
                        new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, ".png"),
                Arguments.of("image/webp",
                        new byte[] {'R', 'I', 'F', 'F', 0x00, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P'}, ".webp")
        );
    }
}
