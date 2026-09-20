package br.com.vistoriapredial.vistoria.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(name = "app.ia.provider", havingValue = "mock", matchIfMissing = true)
public class MockIaIntegrationService implements IaIntegrationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String analisarImagens(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma imagem fornecida para análise.");
        }

        if (imageUrls.stream().anyMatch(url -> url.contains("trigger-fail"))) {
            throw new RuntimeException("Simulated AI Failure");
        }

        List<Map<String, Object>> images = new ArrayList<>(imageUrls.size());
        for (String path : imageUrls) {
            Map<String, Object> image = new LinkedHashMap<>();
            image.put("storagePath", path);
            image.put("analysisId", "mock");
            image.put("overallSummary", "Pré-laudo gerado pela IA (MOCK) para " + path + ".");
            image.put("limitations", List.of(
                    "Análise baseada apenas em imagem; não substitui vistoria técnica presencial."));
            image.put("imageQuality", Map.of("usable", true, "issues", List.of()));
            image.put("areas", List.of(
                    Map.of(
                            "area", "parede",
                            "issueType", "stain",
                            "description", "Mancha aparente detectada no mock.",
                            "evidence", "Simulação local.",
                            "severity", "baixa",
                            "confidence", "baixa",
                            "recommendation", "Avaliação presencial recomendada.",
                            "location", "não aplicável"
                    )
            ));
            images.add(image);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", 1);
        root.put("images", images);
        try {
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Falha ao serializar pré-laudo mock.", exception);
        }
    }
}
