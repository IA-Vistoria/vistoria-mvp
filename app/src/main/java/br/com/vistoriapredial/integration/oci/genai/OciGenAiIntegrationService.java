package br.com.vistoriapredial.integration.oci.genai;

import br.com.vistoriapredial.integration.oci.genai.config.OciGenAiProperties;
import br.com.vistoriapredial.storage.StorageService;
import br.com.vistoriapredial.storage.StoredFile;
import br.com.vistoriapredial.vistoria.application.IaIntegrationService;
import br.com.vistoriapredial.vistoria.application.analysis.AnaliseVistoria;
import br.com.vistoriapredial.vistoria.application.analysis.AnaliseVistoriaDocumentFactory;
import br.com.vistoriapredial.vistoria.application.ia.EvidenciaAnaliseIa;
import br.com.vistoriapredial.vistoria.application.ia.SolicitacaoAnaliseIa;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.generativeaiinference.model.AssistantMessage;
import com.oracle.bmc.generativeaiinference.model.ChatChoice;
import com.oracle.bmc.generativeaiinference.model.ChatContent;
import com.oracle.bmc.generativeaiinference.model.ChatDetails;
import com.oracle.bmc.generativeaiinference.model.GenericChatRequest;
import com.oracle.bmc.generativeaiinference.model.GenericChatResponse;
import com.oracle.bmc.generativeaiinference.model.ImageContent;
import com.oracle.bmc.generativeaiinference.model.ImageUrl;
import com.oracle.bmc.generativeaiinference.model.JsonSchemaResponseFormat;
import com.oracle.bmc.generativeaiinference.model.OnDemandServingMode;
import com.oracle.bmc.generativeaiinference.model.ResponseJsonSchema;
import com.oracle.bmc.generativeaiinference.model.TextContent;
import com.oracle.bmc.generativeaiinference.model.UserMessage;
import com.oracle.bmc.generativeaiinference.requests.ChatRequest;
import com.oracle.bmc.generativeaiinference.responses.ChatResponse;
import com.oracle.bmc.model.BmcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@ConditionalOnProperty(name = "app.ia.provider", havingValue = "oci")
public class OciGenAiIntegrationService implements IaIntegrationService {

    static final long MAX_IMAGE_BYTES = 7L * 1024 * 1024;
    private static final String PROVIDER = "oci";
    private static final String PROMPT_VERSION = "oci-gemini-evidencia-v1";
    private static final Set<String> SUPPORTED_MEDIA_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp");
    private static final Set<String> RESPONSE_FIELDS = Set.of(
            "imageQuality", "summary", "limitations", "captureGuidance", "findings");
    private static final Logger LOGGER = LoggerFactory.getLogger(OciGenAiIntegrationService.class);

    private final StorageService storageService;
    private final OciGenAiChatClient chatClient;
    private final OciGenAiProperties properties;
    private final AnaliseVistoriaDocumentFactory documentFactory;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Supplier<String> analysisIdSupplier;

    @Autowired
    public OciGenAiIntegrationService(
            StorageService storageService,
            OciGenAiChatClient chatClient,
            OciGenAiProperties properties,
            AnaliseVistoriaDocumentFactory documentFactory,
            ObjectMapper objectMapper,
            Clock clock) {
        this(
                storageService,
                chatClient,
                properties,
                documentFactory,
                objectMapper,
                clock,
                () -> UUID.randomUUID().toString());
    }

    OciGenAiIntegrationService(
            StorageService storageService,
            OciGenAiChatClient chatClient,
            OciGenAiProperties properties,
            AnaliseVistoriaDocumentFactory documentFactory,
            ObjectMapper objectMapper,
            Clock clock,
            Supplier<String> analysisIdSupplier) {
        this.storageService = storageService;
        this.chatClient = chatClient;
        this.properties = properties;
        this.documentFactory = documentFactory;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.analysisIdSupplier = analysisIdSupplier;
        properties.validarParaUso();
    }

    @Override
    public String analisar(SolicitacaoAnaliseIa solicitacao) {
        if (solicitacao == null) {
            throw new IllegalArgumentException("A solicitação de análise é obrigatória.");
        }
        Instant inicio = clock.instant();
        String analysisId = analysisIdSupplier.get();
        try {
            List<AnaliseVistoriaDocumentFactory.ObservacaoImagem> observacoes =
                    new ArrayList<>(solicitacao.evidencias().size());
            for (EvidenciaAnaliseIa evidencia : solicitacao.evidencias()) {
                ImagemCarregada imagem = carregar(evidencia);
                ChatResponse response = chamarOci(solicitacao.vistoriaId(), evidencia, imagem);
                observacoes.add(converterResposta(evidencia.storagePath(), response));
            }
            String document = documentFactory.criar(
                    solicitacao,
                    new AnaliseVistoriaDocumentFactory.MetadadosExecucao(
                            PROVIDER,
                            properties.getModelId(),
                            PROMPT_VERSION,
                            analysisId,
                            clock.instant()),
                    observacoes);
            LOGGER.info(
                    "Análise OCI concluída: vistoria={}, analise={}, modelo={}, duracaoMs={}",
                    solicitacao.vistoriaId(),
                    analysisId,
                    properties.getModelId(),
                    Duration.between(inicio, clock.instant()).toMillis());
            return document;
        } catch (OciGenAiIntegrationException exception) {
            LOGGER.warn(
                    "Análise OCI falhou: vistoria={}, analise={}, modelo={}, categoria={}, duracaoMs={}",
                    solicitacao.vistoriaId(),
                    analysisId,
                    properties.getModelId(),
                    exception.getCategoria(),
                    Duration.between(inicio, clock.instant()).toMillis());
            throw exception;
        }
    }

    private ImagemCarregada carregar(EvidenciaAnaliseIa evidencia) {
        StoredFile stored;
        try {
            stored = storageService.load(evidencia.storagePath());
        } catch (RuntimeException exception) {
            throw new OciGenAiIntegrationException(
                    OciGenAiFailureCategory.IA_STORAGE_FAILURE,
                    "Não foi possível carregar a evidência para análise.",
                    exception);
        }
        if (stored == null || stored.resource() == null || stored.mediaType() == null) {
            throw invalidImage("A evidência armazenada está incompleta.");
        }
        String mediaType = stored.mediaType().toString().toLowerCase(java.util.Locale.ROOT);
        if (!SUPPORTED_MEDIA_TYPES.contains(mediaType)
                || !mediaType.equals(evidencia.contentType().toLowerCase(java.util.Locale.ROOT))) {
            throw invalidImage("A evidência deve ser JPEG, PNG ou WebP com tipo consistente.");
        }
        if (stored.length() <= 0 || stored.length() > MAX_IMAGE_BYTES) {
            throw invalidImage("A evidência deve possuir até 7 MB e não pode estar vazia.");
        }
        byte[] bytes;
        try {
            bytes = stored.resource().getContentAsByteArray();
        } catch (IOException exception) {
            throw new OciGenAiIntegrationException(
                    OciGenAiFailureCategory.IA_STORAGE_FAILURE,
                    "Não foi possível ler a evidência para análise.",
                    exception);
        }
        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES || bytes.length != stored.length()) {
            throw invalidImage("O tamanho da evidência armazenada é inválido para análise.");
        }
        return new ImagemCarregada(mediaType, bytes);
    }

    private ChatResponse chamarOci(
            Long vistoriaId,
            EvidenciaAnaliseIa evidencia,
            ImagemCarregada imagem) {
        ChatRequest request = request(vistoriaId, evidencia, imagem);
        try {
            return chatClient.chat(request);
        } catch (BmcException exception) {
            throw traduzir(exception);
        }
    }

    private ChatRequest request(
            Long vistoriaId,
            EvidenciaAnaliseIa evidencia,
            ImagemCarregada imagem) {
        String prompt = """
                Analise a evidência fotográfica de uma vistoria predial.
                Vistoria: %d
                Ambiente: %s
                Categoria: %s
                Descreva somente elementos visualmente sustentados pela imagem, em português.
                Para cada achado, explique o que foi observado, onde, o critério visual,
                o impacto possível e a recomendação prudente. Não invente causas ocultas,
                medições, conformidade normativa ou validade jurídica. Se a imagem não for
                suficiente, marque a qualidade como INSUFICIENTE e explique a nova captura.
                Responda somente com JSON aderente ao schema fornecido.
                """.formatted(
                vistoriaId,
                evidencia.ambienteNome(),
                evidencia.categoria().name());
        String dataUri = "data:" + imagem.mediaType() + ";base64,"
                + Base64.getEncoder().encodeToString(imagem.bytes());
        UserMessage message = UserMessage.builder()
                .content(List.of(
                        TextContent.builder().text(prompt).build(),
                        ImageContent.builder()
                                .imageUrl(ImageUrl.builder().url(dataUri).build())
                                .build()))
                .build();
        GenericChatRequest chat = GenericChatRequest.builder()
                .messages(List.of(message))
                .temperature(0.1)
                .isStream(false)
                .responseFormat(responseFormat())
                .build();
        ChatDetails details = ChatDetails.builder()
                .compartmentId(properties.getCompartmentId())
                .servingMode(OnDemandServingMode.builder()
                        .modelId(properties.getModelId())
                        .build())
                .chatRequest(chat)
                .build();
        return ChatRequest.builder()
                .chatDetails(details)
                .opcRequestId("vistoria-" + vistoriaId + "-imagem-" + evidencia.imagemId())
                .build();
    }

    private JsonSchemaResponseFormat responseFormat() {
        Map<String, Object> findingProperties = new LinkedHashMap<>();
        findingProperties.put("criterion", textSchema());
        findingProperties.put("area", textSchema());
        findingProperties.put("type", textSchema());
        findingProperties.put("description", textSchema());
        findingProperties.put("evidence", textSchema());
        findingProperties.put("impact", textSchema());
        findingProperties.put("severity", enumSchema("BAIXA", "MEDIA", "ALTA", "CRITICA"));
        findingProperties.put("confidence", enumSchema("BAIXA", "MEDIA", "ALTA"));
        findingProperties.put("recommendation", textSchema());
        findingProperties.put("location", textSchema());

        Map<String, Object> finding = objectSchema(
                findingProperties,
                findingProperties.keySet());
        Map<String, Object> rootProperties = new LinkedHashMap<>();
        rootProperties.put("imageQuality", enumSchema("SUFICIENTE", "INSUFICIENTE"));
        rootProperties.put("summary", textSchema());
        rootProperties.put("limitations", Map.of(
                "type", "array",
                "items", textSchema()));
        rootProperties.put("captureGuidance", Map.of("type", List.of("string", "null")));
        rootProperties.put("findings", Map.of(
                "type", "array",
                "maxItems", 100,
                "items", finding));
        Map<String, Object> schema = objectSchema(rootProperties, rootProperties.keySet());
        return JsonSchemaResponseFormat.builder()
                .jsonSchema(ResponseJsonSchema.builder()
                        .name("analise_evidencia_v1")
                        .description("Observação visual estruturada de uma evidência de vistoria.")
                        .schema(schema)
                        .isStrict(true)
                        .build())
                .build();
    }

    private Map<String, Object> textSchema() {
        return Map.of("type", "string", "minLength", 1, "maxLength", 2_000);
    }

    private Map<String, Object> enumSchema(String... values) {
        return Map.of("type", "string", "enum", List.of(values));
    }

    private Map<String, Object> objectSchema(
            Map<String, Object> properties,
            java.util.Collection<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.copyOf(required));
        schema.put("additionalProperties", false);
        return schema;
    }

    private AnaliseVistoriaDocumentFactory.ObservacaoImagem converterResposta(
            String storagePath,
            ChatResponse response) {
        String raw = textoResposta(response);
        try {
            JsonNode root = objectMapper.readTree(raw);
            if (root == null || !root.isObject()
                    || !fieldNames(root).equals(RESPONSE_FIELDS)) {
                throw invalidResponse();
            }
            ModelResponse model = objectMapper.treeToValue(root, ModelResponse.class);
            List<AnaliseVistoriaDocumentFactory.ObservacaoAchado> findings = model.findings()
                    .stream()
                    .map(this::converterAchado)
                    .toList();
            return new AnaliseVistoriaDocumentFactory.ObservacaoImagem(
                    storagePath,
                    model.imageQuality(),
                    model.summary(),
                    model.limitations(),
                    model.captureGuidance(),
                    findings);
        } catch (OciGenAiIntegrationException exception) {
            throw exception;
        } catch (JsonProcessingException | IllegalArgumentException | NullPointerException exception) {
            throw new OciGenAiIntegrationException(
                    OciGenAiFailureCategory.IA_INVALID_RESPONSE,
                    "A resposta da IA não atende ao contrato esperado.",
                    exception);
        }
    }

    private AnaliseVistoriaDocumentFactory.ObservacaoAchado converterAchado(ModelFinding finding) {
        return new AnaliseVistoriaDocumentFactory.ObservacaoAchado(
                finding.criterion(),
                finding.area(),
                finding.type(),
                finding.description(),
                finding.evidence(),
                finding.impact(),
                finding.severity(),
                finding.confidence(),
                finding.recommendation(),
                finding.location());
    }

    private String textoResposta(ChatResponse response) {
        if (response == null
                || response.getChatResult() == null
                || !(response.getChatResult().getChatResponse() instanceof GenericChatResponse generic)
                || generic.getChoices() == null
                || generic.getChoices().isEmpty()) {
            throw invalidResponse();
        }
        ChatChoice choice = generic.getChoices().get(0);
        if (choice == null || !(choice.getMessage() instanceof AssistantMessage message)
                || message.getContent() == null) {
            throw invalidResponse();
        }
        return message.getContent().stream()
                .filter(TextContent.class::isInstance)
                .map(TextContent.class::cast)
                .map(TextContent::getText)
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .orElseThrow(this::invalidResponse);
    }

    private Set<String> fieldNames(JsonNode root) {
        Set<String> names = new LinkedHashSet<>();
        root.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private OciGenAiIntegrationException traduzir(BmcException exception) {
        OciGenAiFailureCategory categoria;
        String mensagem;
        if (exception.isTimeout() || exception.getStatusCode() >= 500) {
            categoria = OciGenAiFailureCategory.OCI_TEMPORARILY_UNAVAILABLE;
            mensagem = "O serviço de análise está temporariamente indisponível.";
        } else {
            categoria = switch (exception.getStatusCode()) {
                case 401 -> OciGenAiFailureCategory.OCI_AUTHENTICATION_FAILED;
                case 403 -> OciGenAiFailureCategory.OCI_AUTHORIZATION_FAILED;
                case 404 -> OciGenAiFailureCategory.OCI_MODEL_NOT_AVAILABLE;
                case 429 -> OciGenAiFailureCategory.OCI_QUOTA_EXCEEDED;
                default -> OciGenAiFailureCategory.OCI_REQUEST_FAILED;
            };
            mensagem = switch (categoria) {
                case OCI_AUTHENTICATION_FAILED -> "A autenticação com a OCI falhou.";
                case OCI_AUTHORIZATION_FAILED -> "A OCI negou acesso ao recurso configurado.";
                case OCI_MODEL_NOT_AVAILABLE -> "O modelo configurado não está disponível.";
                case OCI_QUOTA_EXCEEDED -> "A cota ou o limite de requisições da OCI foi excedido.";
                default -> "A OCI recusou a solicitação de análise.";
            };
        }
        return new OciGenAiIntegrationException(categoria, mensagem, exception);
    }

    private OciGenAiIntegrationException invalidImage(String message) {
        return new OciGenAiIntegrationException(
                OciGenAiFailureCategory.IA_INVALID_IMAGE,
                message);
    }

    private OciGenAiIntegrationException invalidResponse() {
        return new OciGenAiIntegrationException(
                OciGenAiFailureCategory.IA_INVALID_RESPONSE,
                "A resposta da IA está vazia ou não atende ao contrato esperado.");
    }

    private record ImagemCarregada(String mediaType, byte[] bytes) {
    }

    private record ModelResponse(
            AnaliseVistoria.QualidadeEvidencia imageQuality,
            String summary,
            List<String> limitations,
            String captureGuidance,
            List<ModelFinding> findings) {
    }

    private record ModelFinding(
            String criterion,
            String area,
            String type,
            String description,
            String evidence,
            String impact,
            AnaliseVistoria.GravidadeAchado severity,
            AnaliseVistoria.ConfiancaAchado confidence,
            String recommendation,
            String location) {
    }
}
