package br.com.vistoriapredial.integration.oci.genai;

import br.com.vistoriapredial.integration.oci.genai.config.OciGenAiProperties;
import br.com.vistoriapredial.storage.StorageException;
import br.com.vistoriapredial.storage.StorageService;
import br.com.vistoriapredial.storage.StoredFile;
import br.com.vistoriapredial.vistoria.application.analysis.AnaliseVistoriaDocumentFactory;
import br.com.vistoriapredial.vistoria.application.analysis.ResultadoAnaliseCalculator;
import br.com.vistoriapredial.vistoria.application.ia.EvidenciaAnaliseIa;
import br.com.vistoriapredial.vistoria.application.ia.SolicitacaoAnaliseIa;
import br.com.vistoriapredial.vistoria.domain.CategoriaEvidencia;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.bmc.generativeaiinference.model.ChatContent;
import com.oracle.bmc.generativeaiinference.model.ChatDetails;
import com.oracle.bmc.generativeaiinference.model.ChatChoice;
import com.oracle.bmc.generativeaiinference.model.ChatResult;
import com.oracle.bmc.generativeaiinference.model.GenericChatRequest;
import com.oracle.bmc.generativeaiinference.model.GenericChatResponse;
import com.oracle.bmc.generativeaiinference.model.ImageContent;
import com.oracle.bmc.generativeaiinference.model.JsonSchemaResponseFormat;
import com.oracle.bmc.generativeaiinference.model.OnDemandServingMode;
import com.oracle.bmc.generativeaiinference.model.AssistantMessage;
import com.oracle.bmc.generativeaiinference.model.TextContent;
import com.oracle.bmc.generativeaiinference.model.UserMessage;
import com.oracle.bmc.generativeaiinference.requests.ChatRequest;
import com.oracle.bmc.generativeaiinference.responses.ChatResponse;
import com.oracle.bmc.model.BmcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OciGenAiIntegrationServiceTest {

    private static final String RESPOSTA_VALIDA = """
            {
              "imageQuality":"SUFICIENTE",
              "summary":"A evidência permite avaliar visualmente o ambiente.",
              "limitations":[],
              "captureGuidance":null,
              "findings":[]
            }
            """;
    private static final Instant AGORA = Instant.parse("2026-09-30T18:00:00Z");

    private StorageService storageService;
    private FakeChatClient chatClient;
    private ObjectMapper objectMapper;
    private OciGenAiIntegrationService service;

    @BeforeEach
    void setUp() {
        storageService = mock(StorageService.class);
        chatClient = new FakeChatClient();
        objectMapper = new ObjectMapper();
        OciGenAiProperties properties = new OciGenAiProperties();
        properties.setCompartmentId("ocid1.compartment.oc1..vistoria");
        service = new OciGenAiIntegrationService(
                storageService,
                chatClient,
                properties,
                new AnaliseVistoriaDocumentFactory(objectMapper, new ResultadoAnaliseCalculator()),
                objectMapper,
                Clock.fixed(AGORA, ZoneOffset.UTC),
                () -> "analise-oci-123");
    }

    @Test
    void deveFazerUmaChamadaPorImagemEManterAssociacaoComAmbiente() throws Exception {
        chatClient.responder(RESPOSTA_VALIDA);
        chatClient.responder(RESPOSTA_VALIDA);
        armazenar("uploads/sala.jpg", MediaType.IMAGE_JPEG, new byte[]{1, 2});
        armazenar("uploads/cozinha.png", MediaType.IMAGE_PNG, new byte[]{3, 4});
        SolicitacaoAnaliseIa solicitacao = new SolicitacaoAnaliseIa(77L, List.of(
                evidencia(1L, 10L, "Sala", CategoriaEvidencia.VISAO_GERAL,
                        "uploads/sala.jpg", "image/jpeg"),
                evidencia(2L, 20L, "Cozinha", CategoriaEvidencia.DETALHE,
                        "uploads/cozinha.png", "image/png")));

        JsonNode documento = objectMapper.readTree(service.analisar(solicitacao));

        assertThat(chatClient.requisicoes).hasSize(2);
        assertThat(documento.path("version").asInt()).isEqualTo(2);
        assertThat(documento.path("images").get(0).path("environment").path("name").asText())
                .isEqualTo("Sala");
        assertThat(documento.path("images").get(1).path("environment").path("name").asText())
                .isEqualTo("Cozinha");
        assertThat(documento.path("execution").path("analysisId").asText())
                .isEqualTo("analise-oci-123");
    }

    @Test
    void deveEnviarInstrucaoEmPortuguesComAmbienteECategoria() {
        prepararSucesso(MediaType.IMAGE_JPEG, new byte[]{1, 2, 3});

        service.analisar(solicitacaoPadrao());

        String instrucao = textoDa(chatClient.ultimaRequisicao());
        assertThat(instrucao)
                .contains("Analise a evidência fotográfica")
                .contains("Ambiente: Sala integrada")
                .contains("Categoria: VISAO_GERAL")
                .contains("Responda somente com JSON");
    }

    @Test
    void deveEnviarImagemComoDataUriSemAlterarOsBytes() {
        prepararSucesso(MediaType.IMAGE_JPEG, new byte[]{1, 2, 3});

        service.analisar(solicitacaoPadrao());

        assertThat(imagemDa(chatClient.ultimaRequisicao()).getImageUrl().getUrl())
                .isEqualTo("data:image/jpeg;base64,AQID");
    }

    @Test
    void deveUsarGeminiNoModoSobDemandaECompartmentConfigurado() {
        prepararSucesso(MediaType.IMAGE_JPEG, new byte[]{1});

        service.analisar(solicitacaoPadrao());

        ChatDetails details = chatClient.ultimaRequisicao().getChatDetails();
        assertThat(details.getCompartmentId()).isEqualTo("ocid1.compartment.oc1..vistoria");
        assertThat(details.getServingMode()).isInstanceOf(OnDemandServingMode.class);
        assertThat(((OnDemandServingMode) details.getServingMode()).getModelId())
                .isEqualTo("google.gemini-2.5-flash");
    }

    @Test
    void deveExigirSchemaJsonEstritoNaResposta() {
        prepararSucesso(MediaType.IMAGE_JPEG, new byte[]{1});

        service.analisar(solicitacaoPadrao());

        GenericChatRequest request = genericRequest(chatClient.ultimaRequisicao());
        assertThat(request.getResponseFormat()).isInstanceOf(JsonSchemaResponseFormat.class);
        JsonSchemaResponseFormat format = (JsonSchemaResponseFormat) request.getResponseFormat();
        assertThat(format.getJsonSchema().getIsStrict()).isTrue();
        assertThat(format.getJsonSchema().getName()).isEqualTo("analise_evidencia_v1");
        Map<?, ?> schema = (Map<?, ?>) format.getJsonSchema().getSchema();
        assertThat(schema.get("additionalProperties")).isEqualTo(false);
    }

    @ParameterizedTest
    @CsvSource({
            "image/jpeg,jpg",
            "image/png,png",
            "image/webp,webp"
    })
    void deveAceitarFormatosSuportados(String contentType, String extension) {
        MediaType mediaType = MediaType.parseMediaType(contentType);
        String path = "uploads/evidencia." + extension;
        chatClient.responder(RESPOSTA_VALIDA);
        armazenar(path, mediaType, new byte[]{9});
        SolicitacaoAnaliseIa solicitacao = new SolicitacaoAnaliseIa(77L, List.of(
                evidencia(1L, 10L, "Sala", CategoriaEvidencia.VISAO_GERAL,
                        path, contentType)));

        service.analisar(solicitacao);

        assertThat(imagemDa(chatClient.ultimaRequisicao()).getImageUrl().getUrl())
                .startsWith("data:" + contentType + ";base64,");
    }

    @Test
    void deveRejeitarTipoNaoSuportadoAntesDaRede() {
        armazenar("uploads/evidencia.gif", MediaType.IMAGE_GIF, new byte[]{1});
        SolicitacaoAnaliseIa solicitacao = new SolicitacaoAnaliseIa(77L, List.of(
                evidencia(1L, 10L, "Sala", CategoriaEvidencia.VISAO_GERAL,
                        "uploads/evidencia.gif", "image/gif")));

        assertThatThrownBy(() -> service.analisar(solicitacao))
                .isInstanceOf(OciGenAiIntegrationException.class)
                .extracting("categoria")
                .isEqualTo(OciGenAiFailureCategory.IA_INVALID_IMAGE);
        assertThat(chatClient.requisicoes).isEmpty();
    }

    @Test
    void deveRejeitarImagemMaiorQueSeteMegabytesAntesDaRede() {
        byte[] bytes = new byte[7 * 1024 * 1024 + 1];
        armazenar("uploads/sala.jpg", MediaType.IMAGE_JPEG, bytes);

        assertThatThrownBy(() -> service.analisar(solicitacaoPadrao()))
                .isInstanceOf(OciGenAiIntegrationException.class)
                .extracting("categoria")
                .isEqualTo(OciGenAiFailureCategory.IA_INVALID_IMAGE);
        assertThat(chatClient.requisicoes).isEmpty();
    }

    @Test
    void deveRejeitarImagemVaziaAntesDaRede() {
        armazenar("uploads/sala.jpg", MediaType.IMAGE_JPEG, new byte[0]);

        assertThatThrownBy(() -> service.analisar(solicitacaoPadrao()))
                .isInstanceOf(OciGenAiIntegrationException.class)
                .extracting("categoria")
                .isEqualTo(OciGenAiFailureCategory.IA_INVALID_IMAGE);
        assertThat(chatClient.requisicoes).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "401,OCI_AUTHENTICATION_FAILED",
            "403,OCI_AUTHORIZATION_FAILED",
            "404,OCI_MODEL_NOT_AVAILABLE",
            "429,OCI_QUOTA_EXCEEDED",
            "503,OCI_TEMPORARILY_UNAVAILABLE"
    })
    void deveClassificarFalhasHttp(int status, OciGenAiFailureCategory categoria) {
        armazenar("uploads/sala.jpg", MediaType.IMAGE_JPEG, new byte[]{1});
        chatClient.falhar(excecaoOci(status, false));

        assertThatThrownBy(() -> service.analisar(solicitacaoPadrao()))
                .isInstanceOf(OciGenAiIntegrationException.class)
                .extracting("categoria")
                .isEqualTo(categoria);
    }

    @Test
    void deveClassificarTimeoutComoIndisponibilidadeTemporaria() {
        armazenar("uploads/sala.jpg", MediaType.IMAGE_JPEG, new byte[]{1});
        chatClient.falhar(excecaoOci(0, true));

        assertThatThrownBy(() -> service.analisar(solicitacaoPadrao()))
                .isInstanceOf(OciGenAiIntegrationException.class)
                .extracting("categoria")
                .isEqualTo(OciGenAiFailureCategory.OCI_TEMPORARILY_UNAVAILABLE);
    }

    @Test
    void deveClassificarRespostaSemCorpo() {
        armazenar("uploads/sala.jpg", MediaType.IMAGE_JPEG, new byte[]{1});
        chatClient.respostas.add(ChatResponse.builder().__httpStatusCode__(200).build());

        assertThatThrownBy(() -> service.analisar(solicitacaoPadrao()))
                .isInstanceOf(OciGenAiIntegrationException.class)
                .extracting("categoria")
                .isEqualTo(OciGenAiFailureCategory.IA_INVALID_RESPONSE);
    }

    @Test
    void deveClassificarJsonInvalido() {
        prepararResposta("não é json");

        assertThatThrownBy(() -> service.analisar(solicitacaoPadrao()))
                .isInstanceOf(OciGenAiIntegrationException.class)
                .extracting("categoria")
                .isEqualTo(OciGenAiFailureCategory.IA_INVALID_RESPONSE);
    }

    @Test
    void deveClassificarSchemaInvalido() {
        prepararResposta("{\"imageQuality\":\"DESCONHECIDA\"}");

        assertThatThrownBy(() -> service.analisar(solicitacaoPadrao()))
                .isInstanceOf(OciGenAiIntegrationException.class)
                .extracting("categoria")
                .isEqualTo(OciGenAiFailureCategory.IA_INVALID_RESPONSE);
    }

    @Test
    void deveClassificarFalhaDoStorageSemChamarRede() {
        when(storageService.load("uploads/sala.jpg"))
                .thenThrow(new StorageException("detalhe local sensível"));

        assertThatThrownBy(() -> service.analisar(solicitacaoPadrao()))
                .isInstanceOf(OciGenAiIntegrationException.class)
                .hasMessageNotContaining("sensível")
                .extracting("categoria")
                .isEqualTo(OciGenAiFailureCategory.IA_STORAGE_FAILURE);
        assertThat(chatClient.requisicoes).isEmpty();
    }

    @Test
    void deveClassificarFalhaAoLerResourceSemChamarRede() throws Exception {
        ByteArrayResource resource = mock(ByteArrayResource.class);
        when(resource.getContentAsByteArray()).thenThrow(new IOException("conteúdo privado"));
        when(storageService.load("uploads/sala.jpg"))
                .thenReturn(new StoredFile(resource, MediaType.IMAGE_JPEG, 3));

        assertThatThrownBy(() -> service.analisar(solicitacaoPadrao()))
                .isInstanceOf(OciGenAiIntegrationException.class)
                .hasMessageNotContaining("privado")
                .extracting("categoria")
                .isEqualTo(OciGenAiFailureCategory.IA_STORAGE_FAILURE);
        assertThat(chatClient.requisicoes).isEmpty();
    }

    private void prepararSucesso(MediaType mediaType, byte[] bytes) {
        chatClient.responder(RESPOSTA_VALIDA);
        armazenar("uploads/sala.jpg", mediaType, bytes);
    }

    private void prepararResposta(String json) {
        chatClient.responder(json);
        armazenar("uploads/sala.jpg", MediaType.IMAGE_JPEG, new byte[]{1});
    }

    private void armazenar(String path, MediaType mediaType, byte[] bytes) {
        when(storageService.load(path)).thenReturn(new StoredFile(
                new ByteArrayResource(bytes), mediaType, bytes.length));
    }

    private SolicitacaoAnaliseIa solicitacaoPadrao() {
        return new SolicitacaoAnaliseIa(77L, List.of(evidencia(
                1L,
                10L,
                "Sala integrada",
                CategoriaEvidencia.VISAO_GERAL,
                "uploads/sala.jpg",
                "image/jpeg")));
    }

    private EvidenciaAnaliseIa evidencia(
            Long imagemId,
            Long ambienteId,
            String ambiente,
            CategoriaEvidencia categoria,
            String path,
            String contentType) {
        return new EvidenciaAnaliseIa(
                imagemId, ambienteId, ambiente, categoria, path, contentType);
    }

    private GenericChatRequest genericRequest(ChatRequest request) {
        return (GenericChatRequest) request.getChatDetails().getChatRequest();
    }

    private String textoDa(ChatRequest request) {
        UserMessage message = (UserMessage) genericRequest(request).getMessages().get(0);
        return message.getContent().stream()
                .filter(TextContent.class::isInstance)
                .map(TextContent.class::cast)
                .map(TextContent::getText)
                .findFirst()
                .orElseThrow();
    }

    private ImageContent imagemDa(ChatRequest request) {
        UserMessage message = (UserMessage) genericRequest(request).getMessages().get(0);
        return message.getContent().stream()
                .filter(ImageContent.class::isInstance)
                .map(ImageContent.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private BmcException excecaoOci(int status, boolean timeout) {
        BmcException exception = mock(BmcException.class);
        when(exception.getStatusCode()).thenReturn(status);
        when(exception.isTimeout()).thenReturn(timeout);
        return exception;
    }

    private static ChatResponse resposta(String json) {
        List<ChatContent> contents = List.of(TextContent.builder().text(json).build());
        AssistantMessage message = AssistantMessage.builder().content(contents).build();
        ChatChoice choice = ChatChoice.builder().index(0).message(message).build();
        GenericChatResponse generic = GenericChatResponse.builder()
                .choices(List.of(choice))
                .build();
        ChatResult result = ChatResult.builder()
                .modelId("google.gemini-2.5-flash")
                .chatResponse(generic)
                .build();
        return ChatResponse.builder()
                .__httpStatusCode__(200)
                .opcRequestId("req-oci-1")
                .chatResult(result)
                .build();
    }

    private static final class FakeChatClient implements OciGenAiChatClient {
        private final List<ChatRequest> requisicoes = new ArrayList<>();
        private final ArrayDeque<Object> respostas = new ArrayDeque<>();

        void responder(String json) {
            respostas.add(resposta(json));
        }

        void falhar(RuntimeException exception) {
            respostas.add(exception);
        }

        ChatRequest ultimaRequisicao() {
            return requisicoes.get(requisicoes.size() - 1);
        }

        @Override
        public ChatResponse chat(ChatRequest request) {
            requisicoes.add(request);
            Object proxima = respostas.remove();
            if (proxima instanceof RuntimeException exception) {
                throw exception;
            }
            return (ChatResponse) proxima;
        }
    }
}
