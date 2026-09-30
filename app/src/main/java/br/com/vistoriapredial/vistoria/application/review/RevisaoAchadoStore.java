package br.com.vistoriapredial.vistoria.application.review;

import br.com.vistoriapredial.vistoria.application.exception.InvalidReviewException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class RevisaoAchadoStore {

    private static final int VERSION = 1;
    private static final int MAX_REVIEWS = 100;

    private final ObjectMapper objectMapper;

    public RevisaoAchadoStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<RevisaoAchado> read(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            RevisaoDocumento document = objectMapper.readValue(raw, RevisaoDocumento.class);
            if (document.version() != VERSION || document.revisoes() == null
                    || document.revisoes().size() > MAX_REVIEWS) {
                throw invalidDocument(null);
            }
            return document.revisoes().stream()
                    .sorted(reviewOrder())
                    .toList();
        } catch (JsonProcessingException exception) {
            throw invalidDocument(exception);
        }
    }

    public String upsert(String raw, RevisaoAchado review) {
        List<RevisaoAchado> reviews = new ArrayList<>(read(raw));
        reviews.removeIf(existing -> existing.matches(review.imagemId(), review.indiceAchado()));
        reviews.add(review);
        if (reviews.size() > MAX_REVIEWS) {
            throw new InvalidReviewException("A revisão excede o limite de 100 achados.");
        }
        reviews.sort(reviewOrder());
        try {
            return objectMapper.writeValueAsString(new RevisaoDocumento(VERSION, List.copyOf(reviews)));
        } catch (JsonProcessingException exception) {
            throw new InvalidReviewException("Não foi possível persistir a revisão.", exception);
        }
    }

    private Comparator<RevisaoAchado> reviewOrder() {
        return Comparator.comparingLong(RevisaoAchado::imagemId)
                .thenComparingInt(RevisaoAchado::indiceAchado);
    }

    private InvalidReviewException invalidDocument(Throwable cause) {
        return new InvalidReviewException("O documento de revisão persistido é inválido.", cause);
    }

    private record RevisaoDocumento(int version, List<RevisaoAchado> revisoes) {
    }
}
