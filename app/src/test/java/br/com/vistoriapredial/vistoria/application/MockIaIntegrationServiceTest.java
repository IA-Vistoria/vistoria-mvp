package br.com.vistoriapredial.vistoria.application;

import br.com.vistoriapredial.vistoria.application.analysis.AnaliseVistoriaDocumentFactory;
import br.com.vistoriapredial.vistoria.application.analysis.ResultadoAnaliseCalculator;
import br.com.vistoriapredial.vistoria.application.ia.EvidenciaAnaliseIa;
import br.com.vistoriapredial.vistoria.application.ia.SolicitacaoAnaliseIa;
import br.com.vistoriapredial.vistoria.domain.CategoriaEvidencia;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockIaIntegrationServiceTest {

    private static final Instant AGORA = Instant.parse("2026-09-30T20:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockIaIntegrationService service = new MockIaIntegrationService(
            new AnaliseVistoriaDocumentFactory(
                    objectMapper, new ResultadoAnaliseCalculator()),
            Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void deveGerarDocumentoV2RastreavelComResultadosDistintosPorAmbiente() throws Exception {
        String result = service.analisar(solicitacao(
                evidencia(1L, 10L, "Sala", "uploads/sala.jpg"),
                evidencia(2L, 20L, "Cozinha", "uploads/cozinha.jpg"),
                evidencia(3L, 30L, "Quarto", "uploads/quarto.jpg"),
                evidencia(4L, 40L, "Lavanderia", "uploads/lavanderia.jpg")));

        JsonNode root = objectMapper.readTree(result);
        assertThat(root.path("version").intValue()).isEqualTo(2);
        assertThat(root.path("execution").path("provider").textValue()).isEqualTo("mock");
        assertThat(root.path("execution").path("model").textValue())
                .isEqualTo("fixture-visual-v2");
        assertThat(root.path("execution").path("promptVersion").textValue())
                .isEqualTo("vistoria-demo-v2");
        assertThat(root.path("execution").path("completedAt").textValue())
                .isEqualTo(AGORA.toString());
        assertThat(root.path("environments").findValuesAsText("result"))
                .containsExactly(
                        "NAO_APROVADO",
                        "APROVADO_COM_RESSALVAS",
                        "APROVADO",
                        "INCONCLUSIVO");
        assertThat(root.path("overallResult").textValue()).isEqualTo("NAO_APROVADO");
    }

    @Test
    void deveExplicarAchadoDemonstrativoSemExporCaminhoNoTextoVisivel() throws Exception {
        String result = service.analisar(solicitacao(
                evidencia(1L, 10L, "Sala", "uploads/segredo-interno.jpg")));

        JsonNode image = objectMapper.readTree(result).path("images").get(0);
        JsonNode finding = image.path("findings").get(0);

        assertThat(image.path("storagePath").textValue())
                .isEqualTo("uploads/segredo-interno.jpg");
        assertThat(image.path("summary").textValue())
                .contains("Cenário demonstrativo")
                .doesNotContain("uploads/");
        assertThat(finding.path("criterion").textValue()).isNotBlank();
        assertThat(finding.path("description").textValue()).isNotBlank();
        assertThat(finding.path("evidence").textValue()).isNotBlank();
        assertThat(finding.path("impact").textValue()).isNotBlank();
        assertThat(finding.path("recommendation").textValue()).isNotBlank();
        assertThat(finding.path("severity").textValue()).isEqualTo("ALTA");
        assertThat(finding.path("confidence").textValue()).isEqualTo("ALTA");
        assertThat(result).doesNotContain("Pré-laudo gerado pela IA (MOCK)");
    }

    @Test
    void deveRejeitarSolicitacaoNula() {
        assertThatThrownBy(() -> service.analisar(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A solicitação de análise é obrigatória.");
    }

    @Test
    void deveSimularFalhaSemProduzirDocumentoParcial() {
        assertThatThrownBy(() -> service.analisar(solicitacao(
                evidencia(1L, 10L, "Sala", "uploads/trigger-fail.jpg"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falha simulada do provedor de IA.");
    }

    private SolicitacaoAnaliseIa solicitacao(EvidenciaAnaliseIa... evidencias) {
        return new SolicitacaoAnaliseIa(7L, List.of(evidencias));
    }

    private EvidenciaAnaliseIa evidencia(
            Long imagemId,
            Long ambienteId,
            String ambiente,
            String path) {
        return new EvidenciaAnaliseIa(
                imagemId,
                ambienteId,
                ambiente,
                CategoriaEvidencia.VISAO_GERAL,
                path,
                "image/jpeg");
    }
}
