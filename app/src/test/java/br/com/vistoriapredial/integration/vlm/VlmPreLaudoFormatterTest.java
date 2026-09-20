package br.com.vistoriapredial.integration.vlm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.vistoriapredial.integration.vlm.dto.VlmAnalyzeResponse;
import br.com.vistoriapredial.integration.vlm.dto.VlmAreaFinding;
import br.com.vistoriapredial.integration.vlm.dto.VlmImageQuality;

class VlmPreLaudoFormatterTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void formatsFindingsAsStructuredJson() throws Exception {
        VlmAnalyzeResponse response = new VlmAnalyzeResponse();
        response.setAnalysisId("a1");
        response.setOverallSummary("Há indícios visuais de possível umidade.");
        response.setLimitations(List.of(
                "Análise baseada apenas em imagem; não substitui vistoria técnica presencial."));

        VlmImageQuality quality = new VlmImageQuality();
        quality.setUsable(true);
        response.setImageQuality(quality);

        VlmAreaFinding area = new VlmAreaFinding();
        area.setArea("parede");
        area.setIssueType("possible_moisture");
        area.setConfidence("media");
        area.setDescription("Mancha escura próxima ao rodapé.");
        response.setAreas(List.of(area));

        String json = VlmPreLaudoFormatter.format(List.of(
                new VlmPreLaudoFormatter.AnalyzedImage("uploads/foto.jpg", response)));

        JsonNode root = mapper.readTree(json);
        assertEquals(1, root.get("version").asInt());
        assertEquals("uploads/foto.jpg", root.get("images").get(0).get("storagePath").asText());
        assertEquals("possible_moisture", root.get("images").get(0).get("areas").get(0).get("issueType").asText());

        List<String> lines = VlmPreLaudoFormatter.toDisplayLines(json);
        assertTrue(lines.stream().anyMatch(line -> line.contains("possible_moisture")));
    }

    @Test
    void formatsEmptyAreasAsNoFindings() throws Exception {
        VlmAnalyzeResponse response = new VlmAnalyzeResponse();
        response.setOverallSummary("Sem sinais evidentes.");
        VlmImageQuality quality = new VlmImageQuality();
        quality.setUsable(true);
        response.setImageQuality(quality);
        response.setAreas(List.of());

        String json = VlmPreLaudoFormatter.format(List.of(
                new VlmPreLaudoFormatter.AnalyzedImage("uploads/a.jpg", response)));

        JsonNode areas = mapper.readTree(json).get("images").get(0).get("areas");
        assertEquals(0, areas.size());
        assertTrue(VlmPreLaudoFormatter.toDisplayLines(json).contains("Nenhum problema visual evidente."));
    }

    @Test
    void formatsUnusableImageWithIssues() throws Exception {
        VlmAnalyzeResponse response = new VlmAnalyzeResponse();
        response.setOverallSummary("Imagem não pôde ser utilizada para análise visual.");
        VlmImageQuality quality = new VlmImageQuality();
        quality.setUsable(false);
        quality.setIssues(List.of("not_a_decodable_image"));
        response.setImageQuality(quality);
        response.setAreas(List.of());

        String json = VlmPreLaudoFormatter.format(List.of(
                new VlmPreLaudoFormatter.AnalyzedImage("uploads/bad.jpg", response)));

        JsonNode qualityNode = mapper.readTree(json).get("images").get(0).get("imageQuality");
        assertEquals(false, qualityNode.get("usable").asBoolean());
        assertEquals("not_a_decodable_image", qualityNode.get("issues").get(0).asText());
    }

    @Test
    void rejectsEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> VlmPreLaudoFormatter.format(List.of()));
    }
}
