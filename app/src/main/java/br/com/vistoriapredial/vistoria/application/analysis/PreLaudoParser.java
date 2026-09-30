package br.com.vistoriapredial.vistoria.application.analysis;

import br.com.vistoriapredial.vistoria.application.exception.FindingNotFoundException;
import br.com.vistoriapredial.vistoria.application.exception.InvalidAiAnalysisException;
import br.com.vistoriapredial.vistoria.domain.AmbienteVistoria;
import br.com.vistoriapredial.vistoria.domain.CategoriaEvidencia;
import br.com.vistoriapredial.vistoria.domain.ImagemVistoria;
import br.com.vistoriapredial.vistoria.domain.Vistoria;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PreLaudoParser {

    private static final int LEGACY_VERSION = 1;
    private static final int CANONICAL_VERSION = 2;
    private static final int MAX_FINDINGS = 100;
    private static final int MAX_TEXT_LENGTH = 2_000;

    private final ObjectMapper objectMapper;
    private final ResultadoAnaliseCalculator resultadoCalculator;

    public PreLaudoParser(ObjectMapper objectMapper) {
        this(objectMapper, new ResultadoAnaliseCalculator());
    }

    @Autowired
    public PreLaudoParser(
            ObjectMapper objectMapper,
            ResultadoAnaliseCalculator resultadoCalculator) {
        this.objectMapper = objectMapper;
        this.resultadoCalculator = resultadoCalculator;
    }

    public AnaliseVistoria parse(Vistoria vistoria, String raw) {
        JsonNode root = readRoot(raw);
        if (!root.isObject() || !root.path("version").canConvertToInt()) {
            throw unsupportedVersion();
        }

        return switch (root.path("version").intValue()) {
            case LEGACY_VERSION -> parseV1(vistoria, root);
            case CANONICAL_VERSION -> parseV2(vistoria, root);
            default -> throw unsupportedVersion();
        };
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

    private AnaliseVistoria parseV1(Vistoria vistoria, JsonNode root) {
        JsonNode imagesNode = root.path("images");
        if (!imagesNode.isArray()) {
            throw invalidContract();
        }

        Map<String, ImagemVistoria> imagesByPath = imagesByPath(vistoria);
        List<AnaliseVistoria.ImagemAnalise> images = new ArrayList<>();
        int findingCount = 0;

        for (JsonNode imageNode : imagesNode) {
            String storagePath = requiredLegacyText(imageNode, "storagePath");
            ImagemVistoria image = imagesByPath.get(storagePath);
            if (image == null) {
                throw unknownEvidence();
            }

            JsonNode qualityNode = imageNode.path("imageQuality");
            JsonNode findingsNode = imageNode.path("areas");
            if (!qualityNode.isObject() || !qualityNode.path("usable").isBoolean()
                    || !findingsNode.isArray()) {
                throw invalidContract();
            }

            List<AnaliseVistoria.AchadoIa> findings = new ArrayList<>();
            for (int index = 0; index < findingsNode.size(); index++) {
                findingCount = validateFindingLimit(findingCount + 1);
                JsonNode finding = findingsNode.get(index);
                if (!finding.isObject()) {
                    throw invalidContract();
                }
                findings.add(new AnaliseVistoria.AchadoIa(
                        index,
                        optionalLegacyText(finding, "area"),
                        optionalLegacyText(finding, "issueType"),
                        optionalLegacyText(finding, "description"),
                        optionalLegacyText(finding, "evidence"),
                        optionalLegacyText(finding, "severity"),
                        optionalLegacyText(finding, "confidence"),
                        optionalLegacyText(finding, "recommendation"),
                        optionalLegacyText(finding, "location")
                ));
            }

            images.add(new AnaliseVistoria.ImagemAnalise(
                    image.getId(),
                    optionalLegacyText(imageNode, "analysisId"),
                    optionalLegacyText(imageNode, "overallSummary"),
                    legacyTextList(imageNode.path("limitations")),
                    new AnaliseVistoria.QualidadeImagem(
                            qualityNode.path("usable").booleanValue(),
                            legacyTextList(qualityNode.path("issues"))),
                    List.copyOf(findings)
            ));
        }

        return new AnaliseVistoria(LEGACY_VERSION, List.copyOf(images));
    }

    private AnaliseVistoria parseV2(Vistoria vistoria, JsonNode root) {
        AnaliseVistoria.ExecucaoAnalise execucao = parseExecution(root.path("execution"));
        JsonNode imagesNode = root.path("images");
        if (!imagesNode.isArray() || imagesNode.isEmpty()) {
            throw invalidContract();
        }

        Map<String, ImagemVistoria> imagesByPath = imagesByPath(vistoria);
        List<AnaliseVistoria.ImagemAnalise> images = new ArrayList<>();
        int findingCount = 0;
        for (JsonNode imageNode : imagesNode) {
            if (!imageNode.isObject()) {
                throw invalidContract();
            }
            String storagePath = requiredText(imageNode, "storagePath");
            ImagemVistoria image = imagesByPath.get(storagePath);
            if (image == null || !requiredLong(imageNode, "imageId").equals(image.getId())) {
                throw unknownEvidence();
            }

            AnaliseVistoria.AmbienteImagem ambiente = parseEnvironmentReference(
                    imageNode.path("environment"), image);
            AnaliseVistoria.QualidadeEvidencia nivel = requiredEnum(
                    imageNode, "imageQuality", AnaliseVistoria.QualidadeEvidencia.class);
            List<AnaliseVistoria.AchadoIa> findings = new ArrayList<>();
            JsonNode findingsNode = imageNode.path("findings");
            if (!findingsNode.isArray()) {
                throw invalidContract();
            }
            for (int index = 0; index < findingsNode.size(); index++) {
                findingCount = validateFindingLimit(findingCount + 1);
                findings.add(parseFinding(findingsNode.get(index), index));
            }

            images.add(new AnaliseVistoria.ImagemAnalise(
                    image.getId(),
                    storagePath,
                    execucao.identificadorAnalise(),
                    ambiente,
                    requiredText(imageNode, "summary"),
                    textList(imageNode.path("limitations")),
                    optionalText(imageNode, "captureGuidance"),
                    new AnaliseVistoria.QualidadeImagem(nivel, List.of()),
                    List.copyOf(findings)
            ));
        }

        ResultadoAnaliseCalculator.ResultadoCalculado calculado;
        try {
            calculado = resultadoCalculator.calcular(images);
        } catch (IllegalArgumentException exception) {
            throw invalidContract();
        }
        validateDeclaredResults(root, calculado);

        return new AnaliseVistoria(
                CANONICAL_VERSION,
                execucao,
                List.copyOf(images),
                calculado.ambientes(),
                calculado.resultadoGeral(),
                calculado.motivoResultadoGeral());
    }

    private AnaliseVistoria.ExecucaoAnalise parseExecution(JsonNode node) {
        if (!node.isObject()) {
            throw invalidContract();
        }
        return new AnaliseVistoria.ExecucaoAnalise(
                requiredText(node, "provider"),
                requiredText(node, "model"),
                requiredText(node, "promptVersion"),
                requiredText(node, "analysisId"),
                requiredInstant(node, "completedAt"));
    }

    private AnaliseVistoria.AmbienteImagem parseEnvironmentReference(
            JsonNode node, ImagemVistoria image) {
        if (!node.isObject()) {
            throw invalidContract();
        }
        AmbienteVistoria local = image.getAmbiente();
        Long id = requiredLong(node, "id");
        String name = requiredText(node, "name");
        CategoriaEvidencia category = requiredEnum(node, "category", CategoriaEvidencia.class);
        if (local == null || !id.equals(local.getId()) || !name.equals(local.getNome())
                || category != image.getCategoria()) {
            throw new InvalidAiAnalysisException(
                    "A análise da IA referencia um ambiente desconhecido.");
        }
        return new AnaliseVistoria.AmbienteImagem(id, name, category);
    }

    private AnaliseVistoria.AchadoIa parseFinding(JsonNode node, int index) {
        if (!node.isObject()) {
            throw invalidContract();
        }
        return new AnaliseVistoria.AchadoIa(
                index,
                requiredText(node, "criterion"),
                requiredText(node, "area"),
                requiredText(node, "type"),
                requiredText(node, "description"),
                requiredText(node, "evidence"),
                requiredText(node, "impact"),
                requiredEnum(node, "severity", AnaliseVistoria.GravidadeAchado.class),
                requiredEnum(node, "confidence", AnaliseVistoria.ConfiancaAchado.class),
                requiredText(node, "recommendation"),
                requiredText(node, "location"));
    }

    private void validateDeclaredResults(
            JsonNode root, ResultadoAnaliseCalculator.ResultadoCalculado calculado) {
        JsonNode environmentsNode = root.path("environments");
        if (!environmentsNode.isArray()) {
            throw invalidContract();
        }

        Map<Long, DeclaredEnvironmentResult> declared = new LinkedHashMap<>();
        for (JsonNode node : environmentsNode) {
            if (!node.isObject()) {
                throw invalidContract();
            }
            Long id = requiredLong(node, "id");
            DeclaredEnvironmentResult previous = declared.put(id, new DeclaredEnvironmentResult(
                    requiredText(node, "name"),
                    requiredEnum(node, "result", AnaliseVistoria.ResultadoAnalise.class),
                    requiredText(node, "resultReason")));
            if (previous != null) {
                throw invalidContract();
            }
        }

        if (declared.size() != calculado.ambientes().size()) {
            throw incompatibleResult();
        }
        for (AnaliseVistoria.AmbienteResultado environment : calculado.ambientes()) {
            DeclaredEnvironmentResult informed = declared.get(environment.id());
            if (informed == null || !informed.name().equals(environment.nome())
                    || informed.result() != environment.resultado()) {
                throw incompatibleResult();
            }
        }

        AnaliseVistoria.ResultadoAnalise overall = requiredEnum(
                root, "overallResult", AnaliseVistoria.ResultadoAnalise.class);
        requiredText(root, "overallReason");
        if (overall != calculado.resultadoGeral()) {
            throw incompatibleResult();
        }
    }

    private Map<String, ImagemVistoria> imagesByPath(Vistoria vistoria) {
        if (vistoria == null) {
            throw invalidContract();
        }
        try {
            return vistoria.getImagens().stream()
                    .collect(Collectors.toMap(ImagemVistoria::getUrl, Function.identity()));
        } catch (IllegalStateException | NullPointerException exception) {
            throw invalidContract();
        }
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

    private String requiredLegacyText(JsonNode node, String field) {
        String value = optionalLegacyText(node, field);
        if (value == null) {
            throw invalidContract();
        }
        return value;
    }

    private String optionalLegacyText(JsonNode node, String field) {
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

    private List<String> legacyTextList(JsonNode node) {
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
        String text = value.textValue().strip();
        if (text.length() > MAX_TEXT_LENGTH) {
            throw invalidContract();
        }
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
            String value = item.textValue().strip();
            if (value.isEmpty() || value.length() > MAX_TEXT_LENGTH) {
                throw invalidContract();
            }
            values.add(value);
        }
        return List.copyOf(values);
    }

    private Long requiredLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.canConvertToLong() || value.longValue() <= 0) {
            throw invalidContract();
        }
        return value.longValue();
    }

    private Instant requiredInstant(JsonNode node, String field) {
        String value = requiredText(node, field);
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            throw invalidContract();
        }
    }

    private <T extends Enum<T>> T requiredEnum(JsonNode node, String field, Class<T> type) {
        String value = requiredText(node, field);
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw invalidContract();
        }
    }

    private int validateFindingLimit(int count) {
        if (count > MAX_FINDINGS) {
            throw new InvalidAiAnalysisException(
                    "A análise da IA excede o limite de 100 achados.");
        }
        return count;
    }

    private InvalidAiAnalysisException unknownEvidence() {
        return new InvalidAiAnalysisException(
                "A análise da IA referencia uma evidência desconhecida.");
    }

    private InvalidAiAnalysisException incompatibleResult() {
        return new InvalidAiAnalysisException(
                "A análise da IA apresenta resultado incompatível com as evidências.");
    }

    private InvalidAiAnalysisException unsupportedVersion() {
        return new InvalidAiAnalysisException("A versão da análise da IA não é suportada.");
    }

    private InvalidAiAnalysisException invalidContract() {
        return new InvalidAiAnalysisException("A análise da IA não atende ao contrato esperado.");
    }

    private record DeclaredEnvironmentResult(
            String name,
            AnaliseVistoria.ResultadoAnalise result,
            String reason) {
    }
}
