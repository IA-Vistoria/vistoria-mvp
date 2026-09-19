package br.com.vistoriapredial.vistoria.application;

import br.com.vistoriapredial.vistoria.application.exception.InvalidEvidenceException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class EvidenceFileValidator {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    public ValidatedEvidence validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidEvidenceException("O arquivo de evidência não pode estar vazio.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidEvidenceException("O arquivo de evidência deve ter no máximo 10 MB.");
        }

        String contentType = file.getContentType();
        byte[] content = readContent(file);

        if (MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
            requireSignature(content, JPEG_SIGNATURE);
            return new ValidatedEvidence(".jpg", MediaType.IMAGE_JPEG);
        }
        if (MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
            requireSignature(content, PNG_SIGNATURE);
            return new ValidatedEvidence(".png", MediaType.IMAGE_PNG);
        }
        if ("image/webp".equals(contentType)) {
            requireWebpSignature(content);
            return new ValidatedEvidence(".webp", MediaType.parseMediaType("image/webp"));
        }

        throw new InvalidEvidenceException("Envie uma imagem JPEG, PNG ou WebP.");
    }

    private byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new InvalidEvidenceException("Não foi possível validar o arquivo enviado.", exception);
        }
    }

    private void requireSignature(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            throw invalidSignature();
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) {
                throw invalidSignature();
            }
        }
    }

    private void requireWebpSignature(byte[] content) {
        if (content.length < 12
                || content[0] != 'R'
                || content[1] != 'I'
                || content[2] != 'F'
                || content[3] != 'F'
                || content[8] != 'W'
                || content[9] != 'E'
                || content[10] != 'B'
                || content[11] != 'P') {
            throw invalidSignature();
        }
    }

    private InvalidEvidenceException invalidSignature() {
        return new InvalidEvidenceException("O conteúdo não corresponde ao tipo de imagem informado.");
    }
}
