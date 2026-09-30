package br.com.vistoriapredial.vistoria.application.analysis;

import br.com.vistoriapredial.vistoria.application.ia.EvidenciaAnaliseIa;
import br.com.vistoriapredial.vistoria.application.ia.SolicitacaoAnaliseIa;
import br.com.vistoriapredial.vistoria.domain.CategoriaEvidencia;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnaliseVistoriaDocumentFactoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AnaliseVistoriaDocumentFactory factory =
            new AnaliseVistoriaDocumentFactory(objectMapper, new ResultadoAnaliseCalculator());

    @Test
    void deveSerializarTodosOsCamposNomeadosDoDocumentoCanonico() throws Exception {
        String document = factory.criar(solicitacao(), execucao(), List.of(observacaoAlta()));

        JsonNode root = objectMapper.readTree(document);
        assertThat(root.path("version").intValue()).isEqualTo(2);
        assertThat(root.path("execution").path("provider").textValue()).isEqualTo("oci");
        assertThat(root.path("execution").path("model").textValue()).isEqualTo("google.gemini-2.5-flash");
        assertThat(root.path("execution").path("promptVersion").textValue()).isEqualTo("vistoria-visual-v2");
        assertThat(root.path("execution").path("analysisId").textValue()).isEqualTo("ana-7");
        assertThat(root.path("execution").path("completedAt").textValue())
                .isEqualTo("2026-09-30T20:00:00Z");

        JsonNode image = root.path("images").get(0);
        assertThat(image.path("imageId").longValue()).isEqualTo(31L);
        assertThat(image.path("storagePath").textValue()).isEqualTo("uploads/banheiro.webp");
        assertThat(image.path("environment").path("id").longValue()).isEqualTo(8L);
        assertThat(image.path("environment").path("name").textValue()).isEqualTo("Banheiro");
        assertThat(image.path("environment").path("category").textValue()).isEqualTo("VISAO_GERAL");
        assertThat(image.path("imageQuality").textValue()).isEqualTo("SUFICIENTE");
        assertThat(image.path("summary").textValue()).isEqualTo("Mofo aparente na parede.");
        assertThat(image.path("limitations").get(0).textValue()).isEqualTo("Sem medição de umidade.");
        assertThat(image.path("captureGuidance").isNull()).isTrue();

        JsonNode finding = image.path("findings").get(0);
        assertThat(finding.path("criterion").textValue()).isEqualTo("Integridade aparente da parede");
        assertThat(finding.path("area").textValue()).isEqualTo("parede");
        assertThat(finding.path("type").textValue()).isEqualTo("UMIDADE_OU_MOFO_APARENTE");
        assertThat(finding.path("description").textValue()).isEqualTo("Manchas escuras visíveis.");
        assertThat(finding.path("evidence").textValue()).isEqualTo("Padrão irregular na superfície.");
        assertThat(finding.path("impact").textValue()).isEqualTo("Pode indicar degradação do revestimento.");
        assertThat(finding.path("severity").textValue()).isEqualTo("ALTA");
        assertThat(finding.path("confidence").textValue()).isEqualTo("ALTA");
        assertThat(finding.path("recommendation").textValue()).isEqualTo("Avaliar a origem da umidade.");
        assertThat(finding.path("location").textValue()).isEqualTo("Parede ao fundo");

        JsonNode environment = root.path("environments").get(0);
        assertThat(environment.path("id").longValue()).isEqualTo(8L);
        assertThat(environment.path("name").textValue()).isEqualTo("Banheiro");
        assertThat(environment.path("result").textValue()).isEqualTo("NAO_APROVADO");
        assertThat(environment.path("resultReason").textValue())
                .isEqualTo("Foi identificado ao menos um achado de alta ou crítica gravidade.");
        assertThat(root.path("overallResult").textValue()).isEqualTo("NAO_APROVADO");
        assertThat(root.path("overallReason").textValue())
                .isEqualTo("Ao menos um ambiente possui achado de alta ou crítica gravidade.");
    }

    @Test
    void deveConservarIdentidadesLocaisSemAceitarIdsDaObservacao() throws Exception {
        String document = factory.criar(solicitacao(), execucao(), List.of(observacaoAlta()));

        JsonNode image = objectMapper.readTree(document).path("images").get(0);

        assertThat(image.path("imageId").longValue()).isEqualTo(31L);
        assertThat(image.path("environment").path("id").longValue()).isEqualTo(8L);
        assertThat(image.path("environment").path("name").textValue()).isEqualTo("Banheiro");
        assertThat(image.path("environment").path("category").textValue()).isEqualTo("VISAO_GERAL");
    }

    @Test
    void deveDerivarResultadoDosAchadosSemReceberDecisaoLivre() throws Exception {
        String document = factory.criar(solicitacao(), execucao(), List.of(observacaoAlta()));

        JsonNode root = objectMapper.readTree(document);

        assertThat(root.path("environments").get(0).path("result").textValue())
                .isEqualTo("NAO_APROVADO");
        assertThat(root.path("overallResult").textValue()).isEqualTo("NAO_APROVADO");
    }

    @Test
    void deveDerivarInconclusivoQuandoAImagemForInsuficiente() throws Exception {
        AnaliseVistoriaDocumentFactory.ObservacaoImagem insuficiente =
                new AnaliseVistoriaDocumentFactory.ObservacaoImagem(
                        "uploads/banheiro.webp",
                        AnaliseVistoria.QualidadeEvidencia.INSUFICIENTE,
                        "Imagem escura demais para análise.",
                        List.of("Baixa iluminação."),
                        "Refaça a foto com a luz acesa.",
                        List.of());

        String document = factory.criar(solicitacao(), execucao(), List.of(insuficiente));
        JsonNode root = objectMapper.readTree(document);

        assertThat(root.path("environments").get(0).path("result").textValue())
                .isEqualTo("INCONCLUSIVO");
        assertThat(root.path("overallResult").textValue()).isEqualTo("INCONCLUSIVO");
        assertThat(root.path("images").get(0).path("captureGuidance").textValue())
                .isEqualTo("Refaça a foto com a luz acesa.");
    }

    @Test
    void deveRejeitarObservacaoDeEvidenciaDesconhecida() {
        AnaliseVistoriaDocumentFactory.ObservacaoImagem desconhecida =
                new AnaliseVistoriaDocumentFactory.ObservacaoImagem(
                        "uploads/desconhecida.webp",
                        AnaliseVistoria.QualidadeEvidencia.SUFICIENTE,
                        "Sem achados.", List.of(), null, List.of());

        assertThatThrownBy(() -> factory.criar(solicitacao(), execucao(), List.of(desconhecida)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A observação referencia uma evidência desconhecida.");
    }

    @Test
    void deveRejeitarEvidenciaSemObservacao() {
        assertThatThrownBy(() -> factory.criar(solicitacao(), execucao(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cada evidência deve possuir exatamente uma observação.");
    }

    @Test
    void deveRejeitarObservacaoDuplicadaDaMesmaEvidencia() {
        assertThatThrownBy(() -> factory.criar(
                solicitacao(), execucao(), List.of(observacaoAlta(), observacaoAlta())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cada evidência deve possuir exatamente uma observação.");
    }

    @Test
    void deveRejeitarMetadadoDeExecucaoIncompleto() {
        assertThatThrownBy(() -> new AnaliseVistoriaDocumentFactory.MetadadosExecucao(
                " ", "google.gemini-2.5-flash", "vistoria-visual-v2", "ana-7", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O provedor da análise é obrigatório.");
    }

    private SolicitacaoAnaliseIa solicitacao() {
        return new SolicitacaoAnaliseIa(12L, List.of(new EvidenciaAnaliseIa(
                31L,
                8L,
                "Banheiro",
                CategoriaEvidencia.VISAO_GERAL,
                "uploads/banheiro.webp",
                "image/webp")));
    }

    private AnaliseVistoriaDocumentFactory.MetadadosExecucao execucao() {
        return new AnaliseVistoriaDocumentFactory.MetadadosExecucao(
                "oci",
                "google.gemini-2.5-flash",
                "vistoria-visual-v2",
                "ana-7",
                Instant.parse("2026-09-30T20:00:00Z"));
    }

    private AnaliseVistoriaDocumentFactory.ObservacaoImagem observacaoAlta() {
        return new AnaliseVistoriaDocumentFactory.ObservacaoImagem(
                "uploads/banheiro.webp",
                AnaliseVistoria.QualidadeEvidencia.SUFICIENTE,
                "Mofo aparente na parede.",
                List.of("Sem medição de umidade."),
                null,
                List.of(new AnaliseVistoriaDocumentFactory.ObservacaoAchado(
                        "Integridade aparente da parede",
                        "parede",
                        "UMIDADE_OU_MOFO_APARENTE",
                        "Manchas escuras visíveis.",
                        "Padrão irregular na superfície.",
                        "Pode indicar degradação do revestimento.",
                        AnaliseVistoria.GravidadeAchado.ALTA,
                        AnaliseVistoria.ConfiancaAchado.ALTA,
                        "Avaliar a origem da umidade.",
                        "Parede ao fundo")));
    }
}
