package br.com.vistoriapredial.integration.vlm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.vistoriapredial.integration.vlm.dto.VlmAnalyzeResponse;
import br.com.vistoriapredial.integration.vlm.dto.VlmAreaFinding;
import br.com.vistoriapredial.integration.vlm.dto.VlmImageQuality;

/**
 * Serializa análises VLM em JSON estruturado para {@code preLaudoIa}.
 */
public final class VlmPreLaudoFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private VlmPreLaudoFormatter() {
    }

    public static String format(List<AnalyzedImage> images) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma análise VLM para formatar.");
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", 1);
        List<Map<String, Object>> imageNodes = new ArrayList<>(images.size());
        for (AnalyzedImage image : images) {
            imageNodes.add(toNode(image));
        }
        root.put("images", imageNodes);

        try {
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Falha ao serializar pré-laudo VLM.", exception);
        }
    }

    /** Texto legível (lista) a partir do JSON — usado na UI do engenheiro. */
    public static List<String> toDisplayLines(String jsonOrText) {
        if (jsonOrText == null || jsonOrText.isBlank()) {
            return List.of();
        }
        String trimmed = jsonOrText.trim();
        if (!trimmed.startsWith("{")) {
            return trimmed.lines()
                    .map(String::trim)
                    .map(line -> line.replaceFirst("^[-*•]\\s*", ""))
                    .filter(line -> !line.isEmpty())
                    .toList();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = MAPPER.readValue(trimmed, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> images = (List<Map<String, Object>>) root.getOrDefault("images", List.of());
            List<String> lines = new ArrayList<>();
            int index = 1;
            for (Map<String, Object> image : images) {
                lines.add("Foto " + index);
                Object summary = image.get("overallSummary");
                if (summary != null && !summary.toString().isBlank()) {
                    lines.add("Resumo: " + summary);
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> areas = (List<Map<String, Object>>) image.getOrDefault("areas", List.of());
                if (areas.isEmpty()) {
                    lines.add("Nenhum problema visual evidente.");
                } else {
                    for (Map<String, Object> area : areas) {
                        lines.add(formatAreaMap(area));
                    }
                }
                index++;
            }
            return lines;
        } catch (Exception ignored) {
            return List.of(trimmed);
        }
    }

    private static Map<String, Object> toNode(AnalyzedImage image) {
        VlmAnalyzeResponse response = image.response();
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("storagePath", image.storagePath());
        node.put("analysisId", response.getAnalysisId());
        node.put("overallSummary", response.getOverallSummary());
        node.put("limitations", response.getLimitations() != null ? response.getLimitations() : List.of());

        VlmImageQuality quality = response.getImageQuality();
        Map<String, Object> qualityNode = new LinkedHashMap<>();
        if (quality != null) {
            qualityNode.put("usable", quality.isUsable());
            qualityNode.put("issues", quality.getIssues() != null ? quality.getIssues() : List.of());
        } else {
            qualityNode.put("usable", true);
            qualityNode.put("issues", List.of());
        }
        node.put("imageQuality", qualityNode);

        List<Map<String, Object>> areas = new ArrayList<>();
        if (response.getAreas() != null) {
            for (VlmAreaFinding area : response.getAreas()) {
                Map<String, Object> areaNode = new LinkedHashMap<>();
                areaNode.put("area", area.getArea());
                areaNode.put("issueType", area.getIssueType());
                areaNode.put("description", area.getDescription());
                areaNode.put("evidence", area.getEvidence());
                areaNode.put("severity", area.getSeverity());
                areaNode.put("confidence", area.getConfidence());
                areaNode.put("recommendation", area.getRecommendation());
                areaNode.put("location", area.getLocation());
                areas.add(areaNode);
            }
        }
        node.put("areas", areas);
        return node;
    }

    private static String formatAreaMap(Map<String, Object> area) {
        String region = stringOr(area.get("area"), "área");
        String issueType = stringOr(area.get("issueType"), "other_visual_issue");
        String confidence = stringOr(area.get("confidence"), "");
        String description = stringOr(area.get("description"), "");
        StringBuilder builder = new StringBuilder();
        builder.append('[').append(region).append("] ").append(issueType);
        if (!confidence.isBlank()) {
            builder.append(" (confiança: ").append(confidence).append(')');
        }
        if (!description.isBlank()) {
            builder.append(": ").append(description);
        }
        return builder.toString();
    }

    private static String stringOr(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? fallback : text;
    }

    public record AnalyzedImage(String storagePath, VlmAnalyzeResponse response) {
    }
}
