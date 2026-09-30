package br.com.vistoriapredial.vistoria.application.analysis;

import br.com.vistoriapredial.vistoria.application.exception.InvalidAiAnalysisException;
import br.com.vistoriapredial.vistoria.application.exception.FindingNotFoundException;
import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PreLaudoParser {

    private static final int SUPPORTED_VERSION = 1;
    private static final int MAX_FINDINGS = 100;

    private final ObjectMapper objectMapper;

    public PreLaudoParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AnaliseVistoria parse(Vistoria vistoria, String raw) {
        JsonNode root = readRoot(raw);
        if (!root.isObject() || !root.path("version").canConvertToInt()
                || root.path("version").intValue() != SUPPORTED_VERSION) {
            throw new InvalidAiAnalysisException("A versão da análise da IA não é suportada.");
        }

        JsonNode imagesNode = root.path("images");
        if (!imagesNode.isArray()) {
            throw invalidContract();
        }

        Map<String, ImagemVistoria> imagesByPath = vistoria.getImagens().stream()
                .collect(Collectors.toMap(ImagemVistoria::getUrl, Function.identity()));
        List<AnaliseVistoria.ImagemAnalise> images = new ArrayList<>();
        int findingCount = 0;

        for (JsonNode imageNode : imagesNode) {
            String storagePath = requiredText(imageNode, "storagePath");
            ImagemVistoria image = imagesByPath.get(storagePath);
            if (image == null) {
                throw new InvalidAiAnalysisException(
                        "A análise da IA referencia uma evidência desconhecida.");
            }

            JsonNode qualityNode = imageNode.path("imageQuality");
            JsonNode findingsNode = imageNode.path("areas");
            if (!qualityNode.isObject() || !qualityNode.path("usable").isBoolean()
                    || !findingsNode.isArray()) {
                throw invalidContract();
            }

            List<AnaliseVistoria.AchadoIa> findings = new ArrayList<>();
            for (int index = 0; index < findingsNode.size(); index++) {
                findingCount++;
                if (findingCount > MAX_FINDINGS) {
                    throw new InvalidAiAnalysisException(
                            "A análise da IA excede o limite de 100 achados.");
                }
                JsonNode finding = findingsNode.get(index);
                if (!finding.isObject()) {
                    throw invalidContract();
                }
                findings.add(new AnaliseVistoria.AchadoIa(
                        index,
                        optionalText(finding, "area"),
                        optionalText(finding, "issueType"),
                        optionalText(finding, "description"),
                        optionalText(finding, "evidence"),
                        optionalText(finding, "severity"),
                        optionalText(finding, "confidence"),
                        optionalText(finding, "recommendation"),
                        optionalText(finding, "location")
                ));
            }

            images.add(new AnaliseVistoria.ImagemAnalise(
                    image.getId(),
                    optionalText(imageNode, "analysisId"),
                    optionalText(imageNode, "overallSummary"),
                    textList(imageNode.path("limitations")),
                    new AnaliseVistoria.QualidadeImagem(
                            qualityNode.path("usable").booleanValue(),
                            textList(qualityNode.path("issues"))),
                    List.copyOf(findings)
            ));
        }

        return new AnaliseVistoria(SUPPORTED_VERSION, List.copyOf(images));
    }

    public AchadoRef requireFinding(Vistoria vistoria, long imageId, int findingIndex) {
        return parse(vistoria, vistoria.getPreLaudoIa()).imagens().stream()
                .filter(image -> image.imagemId().equals(imageId))
                .filter(image -> findingIndex >= 0 && findingIndex < image.achados().size())
                .map(image -> new AchadoRef(imageId, findingIndex, image.achados().get(findingIndex)))
                .findFirst()
                .orElseThrow(FindingNotFoundException::new);
    }

    public record AchadoRef(
            long imagemId,
            int indiceAchado,
            AnaliseVistoria.AchadoIa achado
    ) {
    }

    private JsonNode readRoot(String raw) {
        if (raw == null || raw.isBlank()) {
            throw invalidContract();
        }
        try {
            return objectMapper.readTree(raw);
        } catch (JsonProcessingException exception) {
            throw new InvalidAiAnalysisException("A análise da IA possui JSON inválido.", exception);
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null) {
            throw invalidContract();
        }
        return value;
    }

    private String optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw invalidContract();
        }
        String text = value.textValue().trim();
        return text.isEmpty() ? null : text;
    }

    private List<String> textList(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (!node.isArray()) {
            throw invalidContract();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isTextual()) {
                throw invalidContract();
            }
            String value = item.textValue().trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private InvalidAiAnalysisException invalidContract() {
        return new InvalidAiAnalysisException("A análise da IA não atende ao contrato esperado.");
    }
}
